package net.perfectdreams.floppapower.commands.impl

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.components.Button
import net.dv8tion.jda.api.interactions.components.ButtonStyle
import net.dv8tion.jda.api.sharding.ShardManager
import net.perfectdreams.floppapower.FloppaPower
import net.perfectdreams.floppapower.commands.AbstractSlashCommand
import net.perfectdreams.floppapower.utils.Constants
import net.perfectdreams.floppapower.utils.FloppaButton

class CheckSimilarAvatarsCommand(private val m: FloppaPower, private val shardManager: ShardManager) : AbstractSlashCommand("checksimilaravatars") {
    override fun execute(event: SlashCommandEvent) {
        val pageId = (event.getOption("page")?.asString?.toInt() ?: 1).coerceAtLeast(1)

        val reply = event.deferReply()

        reply.addEmbeds(buildAvatarMessages(shardManager, pageId))
        reply.addActionRow(*buildActionRow(pageId, event.user.idLong).toTypedArray())
        reply.queue()
    }

    private fun buildActionRow(page: Int, userId: Long): List<Button> {
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
                    .setEmbeds(buildAvatarMessages(shardManager, page + 1))
                    .setActionRow(buildActionRow(page + 1, userId))
                    .queue()
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
                        .setEmbeds(buildAvatarMessages(shardManager, page - 1))
                        .setActionRow(buildActionRow(page - 1, userId))
                        .queue()
                }
            )

        return actionRows
    }

    private fun buildAvatarMessages(shardManager: ShardManager, page: Int): List<MessageEmbed> {
        val drop = (page - 1) * 5

        val currentUsedEmojis = Constants.emojis.toMutableList()
        val guildToEmoji = mutableMapOf<Guild, String>()

        val embeds = mutableListOf<MessageEmbed>()
        shardManager.userCache.groupBy { it.avatarId }
            .filter { it.key != null }
            .entries
            .sortedByDescending { it.value.size }
            .drop(drop)
            .take(5)
            .forEach {
                val firstUser = it.value.first()

                val guildsFound = mutableSetOf<Guild>()

                val serverListText = buildString {
                    val userAndGuildsSorted = it.value.map {
                        it to shardManager.getMutualGuilds(
                            it
                        )
                        // negative because we want to sort descending
                        // and compare by descending does not like longs
                    }.toMap().entries.sortedWith(compareBy({ -it.value.size }, { -it.key.idLong }))

                    for ((user, guilds) in userAndGuildsSorted.take(20)) {
                        append("**${user.name}** (`${user.idLong}`) [")
                        guilds.forEach {
                            val emoji = guildToEmoji[it] ?: run {
                                val random = currentUsedEmojis.random()
                                currentUsedEmojis.remove(random)
                                guildToEmoji[it] = random
                                random
                            }

                            append(emoji)
                        }
                        append("]")
                        append("\n")
                        guildsFound.addAll(guilds)
                    }
                }

                embeds.add(
                    EmbedBuilder()
                        .setTitle("Usado por ${it.value.size} usuários")
                        .addField("Avatar Hash", firstUser.avatarId, true)
                        .addField(
                            "Visto em Servidores (${guildsFound.size})",
                            serverListText.take(1000),
                            false
                        )
                        .setThumbnail(it.value.first().effectiveAvatarUrl)
                        .setFooter("Página $page")
                        .build()
                )
            }

        // Add it as the first embed that you will see
        embeds.add(
            0,
            EmbedBuilder()
                .setTitle("Legenda")
                .setDescription(
                    buildString {
                        for ((guild, emoji) in guildToEmoji) {
                            append("$emoji ${guild.name}\n")
                        }
                    }
                ).build()
        )
        return embeds
    }
}