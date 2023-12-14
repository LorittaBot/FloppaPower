package net.perfectdreams.floppapower.commands.impl

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.sharding.ShardManager
import net.perfectdreams.floppapower.FloppaPower
import net.perfectdreams.floppapower.commands.AbstractSlashCommand
import net.perfectdreams.floppapower.utils.Constants
import net.perfectdreams.floppapower.utils.FloppaButton
import java.time.OffsetDateTime
import java.time.ZoneId

class SusJoinsCommand(private val m: FloppaPower, private val shardManager: ShardManager) : AbstractSlashCommand("susjoins") {
    override fun execute(event: SlashCommandInteractionEvent) {
        val diffBetweenJoinTimes = event.getOption("time")?.asString?.toLong() ?: error("Missing time!")
        val creationTimeDayFilter = event.getOption("creation_time_filter")?.asString?.toLong() ?: error("Missing time!")
        // val pageId = (event.getOption("page")?.asString?.toInt() ?: 1).coerceAtLeast(1)

        event.deferReply()
            .queue {
                it.editOriginalEmbeds(buildAvatarMessages(shardManager, diffBetweenJoinTimes, creationTimeDayFilter,1))
                    .setActionRow(*buildActionRow(1, diffBetweenJoinTimes, creationTimeDayFilter, event.user.idLong).toTypedArray())
                    .queue()
            }
    }

    private fun buildActionRow(page: Int, diffBetweenJoinTimes: Long, creationTimeDayFilter: Long, userId: Long): List<Button> {
        val actionRows = mutableListOf(
            FloppaButton.of(m, ButtonStyle.PRIMARY, "Próxima Página da Depressão") callback@{
                if (it.event.user.idLong != userId) {
                    it.event.deferReply(true)
                        .setContent("sai sai <a:floppaTeeth:849638419885195324>")
                        .queue()
                    return@callback
                }

                it.invalidate()

                it.event.deferEdit()
                    .queue {
                        it.editOriginalEmbeds(buildAvatarMessages(shardManager, diffBetweenJoinTimes, creationTimeDayFilter, page + 1))
                            .setActionRow(buildActionRow(page + 1, diffBetweenJoinTimes, creationTimeDayFilter, userId))
                            .queue()
                    }
            }
        )

        if (page != 1)
            actionRows.add(
                FloppaButton.of(m, ButtonStyle.PRIMARY, "Voltar") callback@{
                    if (it.event.user.idLong != userId) {
                        it.event.deferReply(true)
                            .setContent("sai sai <a:floppaTeeth:849638419885195324>")
                            .queue()
                        return@callback
                    }

                    it.invalidate()

                    it.event.deferEdit()
                        .queue {
                            it.editOriginalEmbeds(buildAvatarMessages(shardManager, diffBetweenJoinTimes, creationTimeDayFilter, page - 1))
                                .setActionRow(buildActionRow(page - 1, diffBetweenJoinTimes, creationTimeDayFilter, userId))
                                .queue()
                        }
                }
            )

        return actionRows
    }

    private fun buildAvatarMessages(shardManager: ShardManager, diffBetweenJoinTimes: Long, creationTimeDayFilter: Long, page: Int): List<MessageEmbed> {
        val susUsers = mutableListOf<Pair<User, List<Pair<Guild, OffsetDateTime>>>>()

        val now = OffsetDateTime.now(ZoneId.of("America/Sao_Paulo"))
            .minusDays(creationTimeDayFilter)

        shardManager.userCache.filter { !it.isBot }.filter { it.timeCreated.isAfter(now) }.forEach {
            val mutualGuilds = shardManager.getMutualGuilds(it)

            if (mutualGuilds.size > 2) {
                val timeJoineds = mutualGuilds.map { guild ->
                    guild to guild.getMember(it)!!.timeJoined
                }.sortedBy { it.second }

                val timeInTheMiddle = timeJoineds[timeJoineds.size / 2]

                val minusOneHour = timeInTheMiddle.second.toLocalDateTime()
                    .minusSeconds(diffBetweenJoinTimes)
                val plusOneHour = timeInTheMiddle.second.toLocalDateTime()
                    .plusSeconds(diffBetweenJoinTimes)

                val isSus = timeJoineds.all {
                    val localDateTime = it.second.toLocalDateTime()
                    localDateTime.isAfter(minusOneHour) && localDateTime.isBefore(plusOneHour)
                }

                if (isSus) {
                    susUsers.add(Pair(it, timeJoineds))
                }
            }
        }

        val drop = (page - 1) * 10

        val embeds = mutableListOf<MessageEmbed>()
        susUsers
            .sortedByDescending { it.first.timeCreated }
            .drop(drop)
            .take(10)
            .forEach {
                val mutualGuilds = shardManager.getMutualGuilds(it.first)
                val guildsWhereTheSusLiesWithin = it.second.map { it.first }
                embeds.add(
                    EmbedBuilder()
                        .setTitle("${it.first.name}#${it.first.discriminator} (${it.first.idLong})")
                        .addField("Conta criada em...", Constants.DATE_FORMATTER.format(it.first.timeCreated), false)
                        .setDescription(
                            buildString {
                                append("**Total: ${mutualGuilds.size}**")
                                append("\n")
                                mutualGuilds.map { guild -> guild to guild.getMember(it.first)!! }
                                    .sortedBy { it.second.timeJoined }
                                    .forEach { (guild, member) ->
                                        val isASusJoin = guild in guildsWhereTheSusLiesWithin

                                        if (isASusJoin)
                                            append("**")
                                        append("${guild.name} [${Constants.DATE_FORMATTER.format(member.timeJoined)}]")
                                        if (isASusJoin)
                                            append("**")
                                        append("\n")
                                    }
                            }
                        )
                        .setThumbnail(it.first.effectiveAvatarUrl)
                        .setFooter("Página $page")
                        .build()
                )
            }

        return embeds
    }
}