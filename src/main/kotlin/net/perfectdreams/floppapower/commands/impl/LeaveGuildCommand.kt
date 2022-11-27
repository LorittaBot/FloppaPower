package net.perfectdreams.floppapower.commands.impl

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.sharding.ShardManager
import net.perfectdreams.floppapower.FloppaPower
import net.perfectdreams.floppapower.commands.AbstractSlashCommand

class LeaveGuildCommand(private val m: FloppaPower, private val shardManager: ShardManager) : AbstractSlashCommand("leaveguild") {
    override fun execute(event: SlashCommandEvent) {
        val guildId = event.getOption("guild_id")?.asString?.toLongOrNull()

        val allowedUsers = listOf(
            123170274651668480L,
            361977144445763585L
        )

        if (allowedUsers.contains(event.interaction.user.idLong)) {
            if (guildId == null) {
                event.deferReply(true)
                    .setContent("Não sei qual é esse servidor não fera <a:floppaTeeth:849638419885195324>")
                    .queue()
                return
            }

            val guild = shardManager.getGuildById(guildId)

            if (guild !== null) {
                guild.leave()

                event.deferReply()
                    .setContent(
                        """
                        |Eu saí de `${guild.name}` (`${guild.id}`) como foi pedido!
                        |A guild tinha `${guild.memberCount} membros`!
                        |E pertencia à `${guild.owner?.user?.asTag}` (`${guild.owner?.id}`)
                        """.trimIndent()
                    )
                    .queue()
            } else {
                event.deferReply(true)
                    .setContent("Não estou em nenhuma guild com este ID! Verifique se está certo")
                    .queue()
                return
            }
        } else {
            event.deferReply(true)
                .setContent("Você não tem permissão para usar este comando!")
                .queue()
            return
        }
    }
}