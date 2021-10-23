package net.perfectdreams.floppapower.listeners

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Emoji
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Button
import net.dv8tion.jda.api.interactions.components.ButtonStyle
import net.perfectdreams.floppapower.FloppaPower
import net.perfectdreams.floppapower.dao.MessageMetadata
import net.perfectdreams.floppapower.dao.MultiEntryMetadata
import net.perfectdreams.floppapower.dao.UserApproval
import net.perfectdreams.floppapower.data.UserWithRole
import net.perfectdreams.floppapower.tables.BlockedAvatarHashes
import net.perfectdreams.floppapower.tables.BlockedUsers
import net.perfectdreams.floppapower.utils.CheckedDueToType
import net.perfectdreams.floppapower.utils.Constants
import net.perfectdreams.floppapower.utils.MetadataEntryType
import net.perfectdreams.floppapower.utils.extensions.await
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

class MessageListener(private val m: FloppaPower) : ListenerAdapter() {
    companion object {
        private val GENERIC_ROLES = listOf(
            "Owner",
            "Admin",
            "Moderator"
        )
        private val logger = KotlinLogging.logger {}
        private val mutex = Mutex()
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.message.channel.idLong != Constants.REPORT_CHANNEL_ID)
            return

        val perLines = event.message.contentRaw.lines()
        val type = perLines.firstOrNull() ?: return
        val comment = perLines.filter { it.startsWith("#") }.map { it.removePrefix("#") }.joinToString("\n") { it.trim() }

        if (type == "selfbot_id" || type == "selfbot_ids" || type == "delete_selfbot_id" || type == "delete_selfbot_ids") {
            GlobalScope.launch {
                val data = retrieveUserIdsFromMessage(event.message)
                event.message.delete().queue()

                val metadata = transaction {
                    MessageMetadata.new {
                        this.type = when {
                            type.startsWith("delete_") -> MetadataEntryType.DELETE_SELFBOT_ID
                            else -> MetadataEntryType.SELFBOT_ID
                        }

                        this.comment = comment
                        this.submittedBy = event.author.idLong
                        this.size = data.size
                        this.approvedBy = "[]"
                    }
                }

                event.channel.sendMessage(
                    MessageBuilder()
                        .setContent(generateContentFromMetadata(metadata))
                        .setActionRows(generateActionRowFromMetadata(metadata))
                        .setAllowedMentions(listOf(Message.MentionType.ROLE)) // none. thank you, next
                        .build()
                ).addFile(
                    checkUserIdsInLines(event.jda, data)
                        .toMutableList()
                        .apply {
                            this.add(0, "")
                            this.add(0, "# Message Metadata ID: ${metadata.id.value}")
                        }
                        .joinToString("\n")
                        .toByteArray(Charsets.UTF_8),
                    "report.txt"
                ).queue()
            }
        } else if (type == "selfbot_avatar_hash" || type == "selfbot_avatar_hashes" || type == "delete_selfbot_avatar_hash" || type == "delete_selfbot_avatar_hashes") {
            GlobalScope.launch {
                val validatedData = retrieveAvatarHashesFromMessage(event.message)

                event.message.delete().queue()

                val metadata = transaction {
                    MessageMetadata.new {
                        this.type = when {
                            type.startsWith("delete_") -> MetadataEntryType.DELETE_AVATAR_HASHES
                            else -> MetadataEntryType.AVATAR_HASHES
                        }

                        this.comment = comment
                        this.submittedBy = event.author.idLong
                        this.size = validatedData.size
                        this.approvedBy = "[]"
                    }
                }

                event.channel.sendMessage(
                    MessageBuilder()
                        .setContent(generateContentFromMetadata(metadata))
                        .setActionRows(generateActionRowFromMetadata(metadata))
                        .setAllowedMentions(listOf(Message.MentionType.ROLE)) // only role mentions
                        .build()
                ).addFile(
                    checkAvatarHashesInLines(event.jda, validatedData)
                        .toMutableList()
                        .apply {
                            this.add(0, "")
                            this.add(0, "# Message Metadata ID: ${metadata.id.value}")
                        }
                        .joinToString("\n")
                        .toByteArray(Charsets.UTF_8),

                    "report.txt"
                ).queue()
            }
        }
    }

    private suspend fun retrieveUserIdsFromMessage(message: Message): List<Long> {
        val data = if (message.attachments.isNotEmpty()) {
            val perLines = withContext(Dispatchers.IO) {
                message.attachments.first().retrieveInputStream().get().readAllBytes()
                    .toString(Charsets.UTF_8).lines()
            }
            perLines.filter { !it.startsWith("#") && it.isNotEmpty() }
        } else {
            val perLines = message.contentRaw.lines()
            perLines.drop(1)
                .filter { !it.startsWith("#") && it.isNotEmpty() }
        }

        return retrieveUserIdsFromLines(data)
    }

    private fun retrieveUserIdsFromLines(lines: List<String>): List<Long> {
        val validatedData = mutableListOf<Long>()

        for (line in lines) {
            validatedData.addAll(
                line.split(" ")
                    .map { it.replace(",", "") }
                    .mapNotNull { it.toLongOrNull() }
            )
        }

        return validatedData.distinct()
    }

    private suspend fun retrieveAvatarHashesFromMessage(message: Message): List<String> {
        val data = if (message.attachments.isNotEmpty()) {
            val perLines = withContext(Dispatchers.IO) {
                message.attachments.first().retrieveInputStream().get().readAllBytes()
                    .toString(Charsets.UTF_8).lines()
            }
            perLines.filter { !it.startsWith("#") && it.isNotEmpty() }
        } else {
            val perLines = message.contentRaw.lines()
            perLines.drop(1)
                .filter { !it.startsWith("#") && it.isNotEmpty() }
        }

        return retrieveAvatarHashesFromLines(data)
    }

    private fun retrieveAvatarHashesFromLines(lines: List<String>): List<String> {
        val validatedData = mutableListOf<String>()

        for (line in lines) {
            validatedData.addAll(
                line.split(" ")
                    .map { it.replace(",", "") }
                    .filter { it.length in Constants.AVATAR_HASH_LENGTH } // Hashes seem to always have 32 chars in length
            )
        }

        return validatedData.distinct()
    }

    private suspend fun checkUserIdsInLines(jda: JDA, data: List<Long>): List<String> {
        val newLines = mutableListOf<String>()
        val retrievedUsers = data.map {
            it to try {
                jda.retrieveUserById(it, false).await()
            } catch (e: ErrorResponseException) {
                null
            }
        }

        val attentionUsers = mutableListOf<User>()

        for ((userId, user) in retrievedUsers.sortedByDescending { it.second?.flags?.size ?: 0 }) {
            if (user != null) {
                val (lines, attentionMembersX) = generateUserInfoLines(jda, user)
                newLines.addAll(lines)
                attentionUsers.addAll(attentionMembersX.map { it.user }.distinct())
                newLines.add(userId.toString())
                newLines.add("")
            }
        }

        if (attentionUsers.isNotEmpty()) {
            newLines.addAll(0, generateAttentionLines(jda, attentionUsers))
        }

        for ((userId, user) in retrievedUsers.filter { it.second == null }) {
            newLines.add("# \uD83D\uDE2D Usuário $userId não existe!")
        }

        return newLines
    }

    private suspend fun checkAvatarHashesInLines(jda: JDA, data: List<String>): List<String> {
        val newLines = mutableListOf<String>()
        val attentionUsers = mutableListOf<User>()

        for (id in data) {
            val users = jda.userCache.filter { it.avatarId == id }
                .sortedByDescending { it.flags.size }

            // Show the user name, if possible
            users.forEach {
                val (lines, attentionMembersX) = generateUserInfoLines(jda, it)
                newLines.addAll(lines)
                attentionUsers.addAll(attentionMembersX.map { it.user }.distinct())
            }

            newLines.add(id)
            newLines.add("")
        }

        if (attentionUsers.isNotEmpty()) {
            newLines.addAll(0, generateAttentionLines(jda, attentionUsers))
        }

        return newLines
    }

    private fun generateUserInfoLines(jda: JDA, user: User): Pair<List<String>, List<Member>> {
        // Show the user name, if possible
        val newLines = mutableListOf<String>()
        val attentionMembers = mutableListOf<Member>()
        newLines.add("# \uD83D\uDE10 ${user.name}#${user.discriminator} (${user.idLong}) [${Constants.DATE_FORMATTER.format(user.timeCreated)}]")
        newLines.add("# ┗ \uD83D\uDD16️ Flags: ${user.flags.joinToString(", ")}")
        val mutualGuilds = jda.getMutualGuilds(user)

        if (mutualGuilds.isNotEmpty()) {
            newLines.add("# ┗ \uD83C\uDFE0 Servidores:")
            mutualGuilds.forEach { guild ->
                val member = guild.getMember(user)!!
                newLines.add("# ┗━ \uD83C\uDFE0 ${guild.name} (${member.roles.joinToString(", ") { it.name }}) [${Constants.DATE_FORMATTER.format(member.timeJoined)}]")
                if (member.roles.any { Constants.TRUSTED_ROLES.any { trustedRoleName -> it.name.contains(trustedRoleName, true) }} || member.hasPermission(Constants.TRUSTED_PERMISSIONS)) {
                    attentionMembers.add(member)
                }
            }
        }
        // newLines.add(user.idLong.toString())
        return Pair(newLines, attentionMembers)
    }

    private fun generateAttentionLines(jda: JDA, attentionUsers: List<User>): MutableList<String> {
        val veryImportantStuff = mutableListOf<String>()
        veryImportantStuff.add("# \uD83D\uDEA8 ATENÇÃO!!! OS SEGUINTES USUÁRIOS SÃO DA EQUIPE DE OUTROS SERVIDORES!!! ANALISE MUITO BEM ANTES DE ACEITAR!!")

        for (attentionUser in attentionUsers) {
            val (lines, attentionMembersX) = generateUserInfoLines(jda, attentionUser)
            veryImportantStuff.addAll(lines)
            veryImportantStuff.add("")
        }

        veryImportantStuff.add("# \uD83D\uDEA8 NOVAMENTE, ANALISE MUITO BEM ANTES DE CONTINUAR!!! Agora vem a lista para ver com todo mundo, MAS BEM SE TEM ALGUÉM DA EQUIPE DE UM SERVIDOR QUER DIZER QUE A PARADA TÁ FEIA MERMÃO")
        veryImportantStuff.add("")
        veryImportantStuff.add("")
        return veryImportantStuff
    }

    override fun onButtonClick(event: ButtonClickEvent) {
        if (!event.componentId.startsWith("approve_ban_entry-") && !event.componentId.startsWith("delete_ban_entry-"))
            return

        val message = event.message
        val metadataId = event.componentId.removePrefix("approve_ban_entry-").removePrefix("delete_ban_entry-")
            .toLong()
        val member = event.member ?: return

        GlobalScope.launch {
            val shouldCheck = mutex.withLock {
                newSuspendedTransaction {
                    val metadata = MessageMetadata.findById(metadataId) ?: run {
                        event.deferReply(true)
                            .setContent("Pelo visto eu não tenho os dados desta mensagem guardados... Talvez a denúncia já foi aprovada mas eu esqueci de atualizar a mensagem <a:SCfloppaSHAKE:853893512542289930>")
                            .queue()
                        return@newSuspendedTransaction Pair(false, null)
                    }

                    val allMatchingRolesFromTheUser = member.roles.filter {
                        it.color != null && it.name !in GENERIC_ROLES
                    }

                    val topRole = allMatchingRolesFromTheUser.firstOrNull {
                        it.idLong !in metadata.approvedByAsList.map { it.roleId }
                    }

                    if (metadata.processed) {
                        // Update the message if it was processed but the buttons are somehow active
                        event.deferEdit()
                            .setContent(generateContentFromMetadata(metadata))
                            .setActionRows(generateActionRowFromMetadata(metadata))
                            .queue {
                                // And tell the user that it was already processed
                                it.setEphemeral(true)
                                    .sendMessage("A denúncia já tinha sido processada, mas como parece que os botões ainda estavam ativos eu mesmo atualizei a mensagem! <a:floppaTeeth:849638419885195324>")
                                    .queue()
                            }
                        
                        return@newSuspendedTransaction Pair(false, metadata.type)
                    }

                    if (event.componentId.startsWith("delete_ban_entry-")) {
                        if (event.user.idLong != metadata.submittedBy) {
                            event.deferReply(true)
                                .setContent("Só quem denunciou pode deletar a denúncia, safad@! <a:floppaTeeth:849638419885195324>")
                                .queue()
                            return@newSuspendedTransaction Pair(false, metadata.type)
                        }

                        metadata.delete()
                        message.delete().queue()
                        return@newSuspendedTransaction Pair(false, metadata.type)
                    } else {
                        if (topRole == null) {
                            event.deferReply(true)
                                .setContent("Alguém da equipe dos servidores que você administra já aprovou isto e você não é ademir de nenhum outro servidor... You are too slow fera! <a:floppaTeeth:849638419885195324>")
                                .queue()
                        } else if (event.user.idLong == metadata.submittedBy) {
                            event.deferReply(true)
                                .setContent("Se você pudesse aprovar as coisas não teria graça, bobinh@! <a:floppaTeeth:849638419885195324>")
                                .queue()
                        } else if (metadata.approvedByAsList.any { it.userId == event.user.idLong }) {
                            event.deferReply(true)
                                .setContent("Você já aprovou esse daqui, bobão! <a:floppaTeeth:849638419885195324>")
                                .queue()
                        } else {
                            metadata.approvedBy = Json.encodeToString(
                                metadata.approvedByAsList.toMutableList().also {
                                    it.add(
                                        UserWithRole(
                                            event.user.idLong,
                                            topRole.idLong
                                        )
                                    )
                                }
                            )

                            event.deferEdit()
                                .setContent(generateContentFromMetadata(metadata))
                                .setActionRows(generateActionRowFromMetadata(metadata))
                                .queue()

                            if (metadata.approvedByAsList.size >= metadata.type.requiredApprovals) {
                                metadata.processed = true

                                // So, this depends on if this is a delete or create operation
                                // If it is a removal process, we will invalidate them
                                if (metadata.type.remove) {
                                    invalidateValuesFromMetadataFromDatabase(metadata, message)
                                } else {
                                    insertValuesFromMetadataIntoDatabase(metadata, message)
                                }

                                // We won't delete the metadata because what if we need to reprocess the report?
                                return@newSuspendedTransaction Pair(true, metadata.type)
                            }
                        }
                    }

                    return@newSuspendedTransaction Pair(false, metadata.type)
                }
            }

            logger.info { "Finished check for ${event.user.idLong} about $metadataId! Should check? $shouldCheck" }

            if (shouldCheck.first) {
                m.updateRepository(event.jda)

                val fileLines = withContext(Dispatchers.IO) {
                    message.attachments.first().retrieveInputStream().get().readAllBytes().toString(Charsets.UTF_8)
                        .lines()
                        .filter { !it.startsWith("#") }
                        .filter { it.isNotEmpty() }
                }
                logger.info { "Retrieved ${fileLines.size} for file..." }

                val metadataType = shouldCheck.second
                when (metadataType) {
                    MetadataEntryType.SELFBOT_ID -> {
                        val userIds = fileLines.map { it.toLong() }

                        event.jda.guildCache.forEach { guild ->
                            logger.info { "Checking ${guild}... Members: ${guild.memberCache.size()}" }
                            guild.memberCache.forEach {
                                if (it.user.idLong in userIds) {
                                    logger.info { "Checking..." }
                                    m.processIfMemberShouldBeBanned(
                                        guild,
                                        it,
                                        CheckedDueToType.ALREADY_IN_THE_SERVER
                                    )
                                }
                            }
                        }
                    }
                    MetadataEntryType.AVATAR_HASHES -> {
                        event.jda.guildCache.forEach { guild ->
                            logger.info { "Checking ${guild}... Members: ${guild.memberCache.size()}" }
                            guild.memberCache.forEach {
                                if (it.user.avatarId in fileLines) {
                                    logger.info { "Checking..." }
                                    m.processIfMemberShouldBeBanned(
                                        guild,
                                        it,
                                        CheckedDueToType.ALREADY_IN_THE_SERVER
                                    )
                                }
                            }
                        }
                    }
                    MetadataEntryType.DELETE_SELFBOT_ID, MetadataEntryType.DELETE_AVATAR_HASHES -> {
                        event.jda.guildCache.forEach { guild ->
                            m.processIfGuildHasMembersThatShouldBeUnbanned(guild, CheckedDueToType.ALREADY_IN_THE_SERVER)
                        }
                    }
                    else -> {
                        logger.info { "No matching type found!" }
                    }
                }
            }
        }
    }

    private suspend fun insertValuesFromMetadataIntoDatabase(metadata: MessageMetadata, message: Message) {
        // ayaya??
        val multiEntry = MultiEntryMetadata.new {
            this.submittedBy = metadata.submittedBy
            this.comment = metadata.comment
            this.approvedAt = Instant.now()
        }

        for (approval in metadata.approvedByAsList) {
            UserApproval.new {
                this.user = approval.userId
                this.approvedEntries = multiEntry.id
            }
        }

        if (metadata.type == MetadataEntryType.SELFBOT_ID) {
            val userIds = withContext(Dispatchers.IO) {
                message.attachments.first().retrieveInputStream().get().readAllBytes()
                    .toString(Charsets.UTF_8)
                    .lines()
                    .filter { !it.startsWith("#") }
                    .filter { it.isNotEmpty() }
                    .map { it.toLong() }
            }

            for (userId in userIds) {
                BlockedUsers.insertIgnore {
                    it[BlockedUsers.userId] = userId
                    it[BlockedUsers.entry] = multiEntry.id.value
                }
            }
        } else {
            val avatarHashes = withContext(Dispatchers.IO) {
                message.attachments.first().retrieveInputStream().get().readAllBytes()
                    .toString(Charsets.UTF_8)
                    .lines()
                    .filter { !it.startsWith("#") }
                    .filter { it.isNotEmpty() }
            }

            for (avatarHash in avatarHashes) {
                BlockedAvatarHashes.insertIgnore {
                    it[BlockedAvatarHashes.avatarHash] = avatarHash
                    it[BlockedAvatarHashes.entry] = multiEntry.id.value
                }
            }
        }
    }

    private suspend fun invalidateValuesFromMetadataFromDatabase(metadata: MessageMetadata, message: Message) {
        if (metadata.type == MetadataEntryType.DELETE_SELFBOT_ID) {
            val userIds = withContext(Dispatchers.IO) {
                message.attachments.first().retrieveInputStream().get().readAllBytes()
                    .toString(Charsets.UTF_8)
                    .lines()
                    .filter { !it.startsWith("#") }
                    .filter { it.isNotEmpty() }
                    .map { it.toLong() }
            }

            for (userId in userIds) {
                BlockedUsers.update({ BlockedUsers.userId eq userId }) {
                    it[valid] = false
                }
            }
        } else {
            val avatarHashes = withContext(Dispatchers.IO) {
                message.attachments.first().retrieveInputStream().get().readAllBytes()
                    .toString(Charsets.UTF_8)
                    .lines()
                    .filter { !it.startsWith("#") }
                    .filter { it.isNotEmpty() }
            }

            for (avatarHash in avatarHashes) {
                BlockedAvatarHashes.update({ BlockedAvatarHashes.avatarHash eq avatarHash }) {
                    it[valid] = false
                }
            }
        }
    }

    private fun generateContentFromMetadata(metadata: MessageMetadata): String {
        val message = """**Hmmm, selfbots fresquinhos para você analisar <@&${Constants.FLOPPA_GANG_ROLE_ID}> <a:floppaTeeth:849638419885195324>**
                            |
                            |**Tipo:** ${metadata.type.fancyName}
                            |**Denúncia feita por:** <@${metadata.submittedBy}>
                            |**Quantidade:** ${metadata.size}
                            |
                            |${metadata.comment.lines().joinToString("\n") { "> $it" }}
                            |
                            |**Aprovados (${metadata.approvedByAsList.size}/${metadata.type.requiredApprovals}):**
                            |${metadata.approvedByAsList.joinToString("\n") { "<@${it.userId}> (<@&${it.roleId}>)" }}
                            |
                            |**NÃO SE ESQUEÇA DE ANALISAR O ARQUIVO PARA VER QUAIS PESSOAS SERÃO BANIDAS, se não você vai deixar o Floppa bem triste... <a:floppaTeeth:849638419885195324>**
                    """.trimMargin()

        if (message.length > 2000) {
            // So big that we need to remove the metadata content...
            val newMessage = """**Hmmm, selfbots fresquinhos para você analisar <@&${Constants.FLOPPA_GANG_ROLE_ID}> <a:floppaTeeth:849638419885195324>**
                            |
                            |**Tipo:** ${metadata.type.fancyName}
                            |**Denúncia feita por:** <@${metadata.submittedBy}>
                            |**Quantidade:** ${metadata.size}
                            |
                            |*A mensagem está tão grande que não consigo mostrar o comentário dela ;w;*
                            |
                            |**Aprovados (${metadata.approvedByAsList.size}/${metadata.type.requiredApprovals}):**
                            |${metadata.approvedByAsList.joinToString("\n") { "<@${it.userId}> (<@&${it.roleId}>)" }}
                            |
                            |**NÃO SE ESQUEÇA DE ANALISAR O ARQUIVO PARA VER QUAIS PESSOAS SERÃO BANIDAS, se não você vai deixar o Floppa bem triste... <a:floppaTeeth:849638419885195324>**
                    """.trimMargin()

            if (newMessage.length > 2000) {
                return """**Hmmm, selfbots fresquinhos para você analisar <@&${Constants.FLOPPA_GANG_ROLE_ID}> <a:floppaTeeth:849638419885195324>**
                            |
                            |**Tipo:** ${metadata.type.fancyName}
                            |**Denúncia feita por:** <@${metadata.submittedBy}>
                            |**Quantidade:** ${metadata.size}
                            |
                            |*A mensagem está tão grande que não consigo mostrar o comentário dela ;w;*
                            |
                            |**Aprovados (${metadata.approvedByAsList.size}/${metadata.type.requiredApprovals}):**
                            |${metadata.approvedByAsList.joinToString("\n") { "<@${it.userId}> (<@&${it.roleId}>)" }}
                    """.trimMargin()
            } else {
                return newMessage
            }
        } else {
            return message
        }
    }

    private fun generateActionRowFromMetadata(metadata: MessageMetadata) =
        if (metadata.approvedByAsList.size >= metadata.type.requiredApprovals) {
            ActionRow.of(
                Button.of(
                    ButtonStyle.SUCCESS,
                    "finished",
                    when (metadata.type.remove) {
                        true -> "Yay, os não-meliantes foram removidos do xilindró!"
                        false -> "Yay, os meliantes foram adicionados ao xilindró!"
                    },
                    Emoji.fromEmote("lori_coffee", 727631176432484473, false)
                ).asDisabled()
            )
        } else {
            ActionRow.of(
                Button.of(
                    ButtonStyle.DANGER,
                    "approve_ban_entry-${metadata.id.value}",
                    "Aprovar",
                    Emoji.fromEmote("lori_ban_hammer", 741058240455901254, false)
                ),
                Button.of(
                    ButtonStyle.SECONDARY,
                    "delete_ban_entry-${metadata.id.value}",
                    "Deletar",
                    Emoji.fromEmote("lori_sob", 556524143281963008, false)
                )
            )
        }
}