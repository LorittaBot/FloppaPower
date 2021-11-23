package net.perfectdreams.floppapower

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.util.IsolationLevel
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.perfectdreams.floppapower.dao.BlockedAvatarHash
import net.perfectdreams.floppapower.dao.BlockedAvatarHashBanEntry
import net.perfectdreams.floppapower.dao.BlockedUser
import net.perfectdreams.floppapower.dao.BlockedUserBanEntry
import net.perfectdreams.floppapower.listeners.AvatarChangeListener
import net.perfectdreams.floppapower.listeners.FloppaGangButtonListener
import net.perfectdreams.floppapower.listeners.JoinListener
import net.perfectdreams.floppapower.listeners.MessageListener
import net.perfectdreams.floppapower.listeners.SlashCommandListener
import net.perfectdreams.floppapower.rest.FloppaPowerWebAPI
import net.perfectdreams.floppapower.tables.BlockedAvatarHashes
import net.perfectdreams.floppapower.tables.BlockedAvatarHashesBanEntries
import net.perfectdreams.floppapower.tables.BlockedUserBanEntries
import net.perfectdreams.floppapower.tables.BlockedUsers
import net.perfectdreams.floppapower.tables.MessagesMetadata
import net.perfectdreams.floppapower.tables.MultiEntriesMetadata
import net.perfectdreams.floppapower.tables.UsersApprovals
import net.perfectdreams.floppapower.utils.CheckedDueToType
import net.perfectdreams.floppapower.utils.Constants
import net.perfectdreams.floppapower.utils.FloppaButtonClickEvent
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.DEFAULT_REPETITION_ATTEMPTS
import org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class FloppaPower {
    companion object {
        private val logger = KotlinLogging.logger {}

        private val DRIVER_CLASS_NAME = "org.postgresql.Driver"
        private val ISOLATION_LEVEL =
            IsolationLevel.TRANSACTION_REPEATABLE_READ // We use repeatable read to avoid dirty and non-repeatable reads! Very useful and safe!!
    }

    val sqLiteDatabaseFile = File("floppa.db")
    val sqliteDatabase = Database.connect("jdbc:sqlite:${sqLiteDatabaseFile.toPath()}", driver = "org.sqlite.JDBC")

    val database = initPostgreSQL(
        System.getenv("FLOPPA_DATABASE_ADDRESS"),
        System.getenv("FLOPPA_DATABASE_NAME"),
        System.getenv("FLOPPA_DATABASE_USERNAME"),
        System.getenv("FLOPPA_DATABASE_PASSWORD")
    )
    val queue = ConcurrentLinkedQueue<String>()
    // Tries to avoid "database is locked"
    val semaphoreCheck = Semaphore(1)
    // Store button interactions cache
    // Only used for commands!
    val slashCommandButtonInteractionCache = ConcurrentHashMap<UUID, (FloppaButtonClickEvent) -> (Unit)>()

    fun start() {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(
                BlockedUsers,
                BlockedAvatarHashes,
                MessagesMetadata,
                MultiEntriesMetadata,
                UsersApprovals,
                BlockedUserBanEntries,
                BlockedAvatarHashesBanEntries
            )
        }

        if (System.getenv("FLOPPA_MIGRATE_DATABASE") == "true") {
            MigrationTool.migrate(database, sqliteDatabase)
            exitProcess(0)
        }

        val jda = JDABuilder.createDefault(
            System.getenv("FLOPPA_DISCORD_TOKEN"),
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.GUILD_BANS
        ).setMemberCachePolicy(MemberCachePolicy.ALL) // we, want, EVERYTHING
            .setChunkingFilter(ChunkingFilter.ALL) // EVERYTHING
            .setStatus(OnlineStatus.INVISIBLE) // no one will ever know!
            .addEventListeners(
                MessageListener(this),
                JoinListener(this),
                SlashCommandListener(this),
                FloppaGangButtonListener(this),
                AvatarChangeListener(this)
            )
            .build()

        thread {
            while (true) {
                val builder = StringBuilder()

                while (queue.isNotEmpty()) {
                    val firstElement = queue.peek()
                        .take(1999) // take 1999 just to avoid a message to big issue

                    // Current length + First Element + "\n"
                    // If it will overflow, we break the loop and send the message as is
                    if (builder.length + firstElement.length + 1 > 2000)
                        break

                    // Append the message content
                    builder.append(firstElement)
                    builder.append("\n")

                    // And remove the current message!
                    queue.remove()
                }

                if (builder.isNotEmpty()) {
                    try {
                        // Send the message if the content is not empty
                        val reportChannelId = jda.getTextChannelById(Constants.LOG_CHANNEL_ID) ?: continue
                        reportChannelId.sendMessage(MessageBuilder(builder.toString()).setAllowedMentions(listOf()).build()).queue()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                Thread.sleep(5_000)
            }
        }

        jda.getGuildById(Constants.ELITE_PENGUIN_FORCE_GUILD_ID)?.upsertCommand(
            CommandData("guilds", "Mostra quais servidores o Floppa está")
        )?.queue()

        jda.getGuildById(Constants.ELITE_PENGUIN_FORCE_GUILD_ID)?.upsertCommand(
            CommandData("checkguild", "Verifique meliantes em um servidor")
                .addOption(OptionType.STRING, "guild_id", "ID do Servidor", true)
        )?.queue()

        jda.getGuildById(Constants.ELITE_PENGUIN_FORCE_GUILD_ID)?.upsertCommand(
            CommandData("checkguilds", "Verifique meliantes em todos os servidores que eu estou")
        )?.queue()

        jda.getGuildById(Constants.ELITE_PENGUIN_FORCE_GUILD_ID)?.upsertCommand(
            CommandData("checksimilaravatars", "Verifique meliantes com avatares similares")
                .addOption(OptionType.INTEGER, "page", "Página", false)
        )?.queue()

        jda.getGuildById(Constants.ELITE_PENGUIN_FORCE_GUILD_ID)?.upsertCommand(
            CommandData("sameavatar", "Verifique meliantes que possuem o mesmo avatar")
                .addSubcommands(
                    SubcommandData("hash", "Verifique meliantes que possuem o mesmo avatar pelo hash")
                        .addOption(OptionType.STRING, "hash", "O hash do avatar", true),
                    SubcommandData("user", "Verifique meliantes que possuem o mesmo avatar pelo usuário")
                        .addOption(OptionType.USER, "user", "O meliante", true)
                )
        )?.queue()

        jda.getGuildById(Constants.ELITE_PENGUIN_FORCE_GUILD_ID)?.upsertCommand(
            CommandData("sharedguilds", "Verifique aonde os meliantes estão na fuga")
                .addOption(OptionType.USER, "user", "O meliante", true)
        )?.queue()

        jda.getGuildById(Constants.ELITE_PENGUIN_FORCE_GUILD_ID)?.upsertCommand(
            CommandData("susjoins", "Verifique meliantes que entraram em vários servidores em seguida")
                .addOptions(
                    OptionData(OptionType.STRING, "time", "A diferença + e - de tempo que o meliante entrou, baseado no tempo de entrada no \"meio\" dos joins", true)
                        .addChoice("10 segundos", "5")
                        .addChoice("15 segundos", "7")
                        .addChoice("30 segundos", "15")
                        .addChoice("Um minuto", "30")
                        .addChoice("Dois minutos", "60")
                        .addChoice("Cinco minutos", "150")
                        .addChoice("Dez minutos", "300")
                        .addChoice("Trinta minutos", "900")
                )
                .addOptions(
                    OptionData(OptionType.STRING, "creation_time_filter", "Filtrar usuários que criaram a conta recentemente, acelera a pesquisa de usuários", true)
                        .addChoice("3 dias", "3")
                        .addChoice("7 dias", "7")
                        .addChoice("14 dias", "14")
                        .addChoice("30 dias", "30")
                        .addChoice("90 dias", "90")
                        .addChoice("180 dias", "180")
                        .addChoice("365 dias", "365")
                        .addChoice("Gosto de ver pegando fogo!", "36500")
                )
        )?.queue()

        val webAPI = FloppaPowerWebAPI(this, jda)
        webAPI.start()
    }

    suspend fun processIfMemberShouldBeBanned(guild: Guild, member: Member, checkedDueTo: CheckedDueToType) {
        /* if (member.roles.isNotEmpty()) // Ignore members with roles
            return */
        // logger.info { "Checking if ${member.idLong} in ${guild.idLong} should be banned... Checked due to: $checkedDueTo" }

        try {
            semaphoreCheck.withPermit {
                if (checkedDueTo == CheckedDueToType.MEMBER_GUILD_JOIN) {
                    logger.info { "Verifying $member's join in $guild..." }
                }

                newSuspendedTransaction {
                    val blockedUser = BlockedUser.findById(member.user.idLong)
                    if (blockedUser != null && blockedUser.valid) {
                        logger.info { "Is user ${member.idLong} Selfbot ID blocked? Yes and it is valid! $blockedUser" }

                        if (guild.selfMember.hasPermission(Permission.BAN_MEMBERS)) {
                            if (guild.selfMember.canInteract(member)) {
                                guild.ban(
                                    member,
                                    0,
                                    "FloppaPower! Self Bot (ID) Report #${blockedUser.entry.id.value}"
                                ).queue()

                                log(
                                    guild.jda,
                                    "`[Report Selfbot IDs #${blockedUser.entry.id.value}/$checkedDueTo]` Usuário ${member.asMention} (`${member.idLong}`) foi banido em ${guild.name} (`${guild.idLong}`) por causa do ID (Cargos do usuário: ${member.roles.joinToString { it.name }})"
                                )

                                BlockedUserBanEntry.new {
                                    this.user = member.user.idLong
                                    this.guild = member.guild.idLong
                                    this.bannedAt = Instant.now()
                                    this.blockEntry = blockedUser
                                }
                            } else {
                                log(
                                    guild.jda,
                                    "`[Report Avatar Hashes #${blockedUser.entry.id.value}/$checkedDueTo]` Usuário ${member.asMention} (`${member.idLong}`) seria banido em ${guild.name} (`${guild.idLong}`) por causa do ID... mas não consigo interagir com o usuário para banir! <a:floppaTeeth:849638419885195324> (Cargos do usuário: ${member.roles.joinToString { it.name }})"
                                )
                            }
                        } else {
                            log(
                                guild.jda,
                                "`[Report Selfbot IDs #${blockedUser.entry.id.value}/$checkedDueTo]` Usuário ${member.asMention} (`${member.idLong}`) seria banido em ${guild.name} (`${guild.idLong}`) por causa do ID... mas não tenho permissão para banir! <a:floppaTeeth:849638419885195324>"
                            )
                        }
                    }

                    // If I put a else here, it complains that it isn't a expression... weird
                    if (blockedUser == null) {
                        val avatarId = member.user.avatarId

                        if (avatarId != null) {
                            val blockedAvatarHash = BlockedAvatarHash.findById(avatarId)
                            if (blockedAvatarHash != null && blockedAvatarHash.valid) {
                                logger.info { "Is user ${member.idLong} Selfbot Avatar Hash blocked? Yes and it is valid! $blockedAvatarHash" }

                                if (guild.selfMember.hasPermission(Permission.BAN_MEMBERS)) {
                                    if (guild.selfMember.canInteract(member)) {
                                        guild.ban(
                                            member,
                                            0,
                                            "FloppaPower! Self Bot (Avatar Hash) Report #${blockedAvatarHash.entry.id.value}"
                                        )
                                            .queue()

                                        log(
                                            guild.jda,
                                            "`[Report Avatar Hashes #${blockedAvatarHash.entry.id.value}/$checkedDueTo]` Usuário ${member.asMention} (`${member.idLong}`) foi banido em ${guild.name} (`${guild.idLong}`) por causa do avatar ${blockedAvatarHash.id.value} (Cargos do usuário: ${member.roles.joinToString { it.name }})"
                                        )

                                        BlockedAvatarHashBanEntry.new {
                                            this.user = member.user.idLong
                                            this.guild = member.guild.idLong
                                            this.bannedAt = Instant.now()
                                            this.blockEntry = blockedAvatarHash
                                        }
                                    } else {
                                        log(
                                            guild.jda,
                                            "`[Report Avatar Hashes #${blockedAvatarHash.entry.id.value}/$checkedDueTo]` Usuário ${member.asMention} (`${member.idLong}`) seria banido em ${guild.name} (`${guild.idLong}`) por causa do avatar ${blockedAvatarHash.id.value}... mas não consigo interagir com o usuário para banir! <a:floppaTeeth:849638419885195324> (Cargos do usuário: ${member.roles.joinToString { it.name }})"
                                        )
                                    }
                                } else {
                                    log(
                                        guild.jda,
                                        "`[Report Avatar Hashes #${blockedAvatarHash.entry.id.value}/$checkedDueTo]` Usuário ${member.asMention} (`${member.idLong}`) seria banido em ${guild.name} (`${guild.idLong}`) por causa do avatar ${blockedAvatarHash.id.value}... mas não tenho permissão para banir! <a:floppaTeeth:849638419885195324>"
                                    )
                                }
                            }
                        }
                    }
                }

                if (checkedDueTo == CheckedDueToType.MEMBER_GUILD_JOIN) {
                    logger.info { "Verification of $member's join in $guild finished!" }
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Something went wrong while processing users to be banned" }
            log(guild.jda, "Deu ruim!\n\n```\n${e.stackTraceToString()}\n```")
        }
    }

    suspend fun processIfGuildHasMembersThatShouldBeUnbanned(guild: Guild, checkedDueTo: CheckedDueToType) {
        // logger.info { "Checking if ${guild.idLong} has members that should be unbanned... Checked due to: $checkedDueTo" }

        try {
            semaphoreCheck.withPermit {
                val blockedUsersThatShouldBeNotBlockedAnymore = newSuspendedTransaction {
                    BlockedUsers.innerJoin(BlockedUserBanEntries)
                        .select { BlockedUsers.valid eq false and (BlockedUserBanEntries.guild eq guild.idLong) }
                        .toList()
                }

                log(
                    guild.jda,
                    "`[Unban Selfbot IDs/$checkedDueTo]` Existem ${blockedUsersThatShouldBeNotBlockedAnymore.size} usuários que estão banidos em ${guild.name} (`${guild.idLong}`) mas não deveriam estar!"
                )

                blockedUsersThatShouldBeNotBlockedAnymore.forEach {
                    val userId = it[BlockedUserBanEntries.user]
                    if (guild.selfMember.hasPermission(Permission.BAN_MEMBERS)) {
                        // hmm where is the unban(Long)?
                        guild.unban(it[BlockedUserBanEntries.user].toString()).queue()

                        log(
                            guild.jda,
                            "`[Unban Selfbot IDs/$checkedDueTo]` Usuário <@$userId> (`$userId`) foi desbanido em ${guild.name} (`${guild.idLong}`) pois a denúncia foi invalidada!"
                        )
                    } else {
                        log(
                            guild.jda,
                            "`[Unban Selfbot IDs/$checkedDueTo]` Usuário <@$userId> (`$userId`) seria desbanido em ${guild.name} (`${guild.idLong}`) pois a denúncia foi invalidada... mas não tenho permissão para banir! <a:floppaTeeth:849638419885195324>"
                        )
                    }
                }

                newSuspendedTransaction {
                    BlockedUserBanEntries.deleteWhere { BlockedUserBanEntries.id inList blockedUsersThatShouldBeNotBlockedAnymore.map { it[BlockedUserBanEntries.id] } }
                }

                val blockedAvatarHashesThatShouldBeNotBlockedAnymore = newSuspendedTransaction {
                    BlockedAvatarHashes.innerJoin(BlockedAvatarHashesBanEntries)
                        .select { BlockedAvatarHashes.valid eq false and (BlockedAvatarHashesBanEntries.guild eq guild.idLong) }
                        .toList()
                }

                log(
                    guild.jda,
                    "`[Unban Avatar Hashes/$checkedDueTo]` Existem ${blockedAvatarHashesThatShouldBeNotBlockedAnymore.size} usuários que estão banidos em ${guild.name} (`${guild.idLong}`) mas não deveriam estar!"
                )

                blockedAvatarHashesThatShouldBeNotBlockedAnymore.forEach {
                    val userId = it[BlockedAvatarHashesBanEntries.user]
                    if (guild.selfMember.hasPermission(Permission.BAN_MEMBERS)) {
                        // hmm where is the unban(Long)?
                        guild.unban(it[BlockedAvatarHashesBanEntries.user].toString()).queue()

                        log(
                            guild.jda,
                            "`[Unban Avatar Hashes/$checkedDueTo]` Usuário <@$userId> (`$userId`) foi desbanido em ${guild.name} (`${guild.idLong}`) pois a denúncia foi invalidada!"
                        )
                    } else {
                        log(
                            guild.jda,
                            "`[Unban Avatar Hashes/$checkedDueTo]` Usuário <@$userId> (`$userId`) seria desbanido em ${guild.name} (`${guild.idLong}`) pois a denúncia foi invalidada... mas não tenho permissão para banir! <a:floppaTeeth:849638419885195324>"
                        )
                    }
                }

                newSuspendedTransaction {
                    BlockedAvatarHashesBanEntries.deleteWhere { BlockedAvatarHashesBanEntries.id inList blockedAvatarHashesThatShouldBeNotBlockedAnymore.map { it[BlockedAvatarHashesBanEntries.id] } }
                }
            }
        } catch (e: Throwable) {
            logger.warn(e) { "Something went wrong while processing users to be unbanned" }
            log(guild.jda, "Deu ruim!\n\n```\n${e.stackTraceToString()}\n```")
        }
    }

    /**
     * Updates the FloppaPower repository with the new changes
     */
    suspend fun updateRepository(jda: JDA) {
        // TODO: Fix this, because we are running a Docker container idk how we could do this
        // Maybe by cloning it before starting the project?
        log(jda, "Atualizando repositório...")

        /* newSuspendedTransaction {
            val connection = (TransactionManager.current().connection.connection as HikariProxyConnection).unwrap(
                PgConnection::class.java
            )

            val copyManager = CopyManager(connection)
            val file = File("./data/output.csv")
            val fileOutputStream = ByteArrayOutputStream()

            //and finally execute the COPY command to the file with this method:
            copyManager.copyOut("COPY (select * from profiles) TO STDOUT WITH (FORMAT CSV, HEADER)", fileOutputStream)
        }

        ProcessBuilder("bash", "dump_and_push.sh")
            .inheritIO()
            .directory(File("./data/"))
            .start()
            .waitFor() */
    }

    fun log(jda: JDA, text: String) {
        queue.add(text)
    }

    fun initPostgreSQL(address: String, databaseName: String, username: String, password: String): Database {
        val hikariConfig = createHikariConfig()
        hikariConfig.jdbcUrl = "jdbc:postgresql://$address/$databaseName"

        hikariConfig.username = username
        hikariConfig.password = password

        var database: Database? = null
        while (database == null) {
            try {
                database = connectToDatabase(HikariDataSource(hikariConfig))
                break
            } catch (e: Exception) {
                logger.warn(e) { "Failed to connect to the database, retrying in 2s..." }
            }
            Thread.sleep(2_000)
        }
        return database!!
    }

    private fun createHikariConfig(): HikariConfig {
        val hikariConfig = HikariConfig()

        hikariConfig.driverClassName = DRIVER_CLASS_NAME

        // Exposed uses autoCommit = false, so we need to set this to false to avoid HikariCP resetting the connection to
        // autoCommit = true when the transaction goes back to the pool, because resetting this has a "big performance impact"
        // https://stackoverflow.com/a/41206003/7271796
        hikariConfig.isAutoCommit = false

        // Useful to check if a connection is not returning to the pool, will be shown in the log as "Apparent connection leak detected"
        hikariConfig.leakDetectionThreshold = 30L * 1000
        hikariConfig.transactionIsolation = IsolationLevel.TRANSACTION_REPEATABLE_READ.name // We use repeatable read to avoid dirty and non-repeatable reads! Very useful and safe!!

        return hikariConfig
    }

    private fun connectToDatabase(dataSource: HikariDataSource): Database =
        Database.connect(
            HikariDataSource(dataSource)
        ) {
            // This code is the same callback used in the "Database.connect(...)" call, but with the default isolation level change
            ThreadLocalTransactionManager(it, DEFAULT_REPETITION_ATTEMPTS).also {
                it.defaultIsolationLevel = ISOLATION_LEVEL.levelId // Change our default isolation level
            }
        }
}