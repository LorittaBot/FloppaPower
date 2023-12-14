package net.perfectdreams.floppapower.commands.impl

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.sharding.ShardManager
import net.perfectdreams.floppapower.FloppaPower
import net.perfectdreams.floppapower.commands.AbstractSlashCommand

class LeaveGuildCommand(private val m: FloppaPower, private val shardManager: ShardManager) : AbstractSlashCommand("leaveguild") {
    override fun execute(event: SlashCommandInteractionEvent) {
        val guildId = event.getOption("guild_id")?.asString?.toLongOrNull()
        val peguinGuardID = 819692196378443800L

        if (event.member?.roles?.map { it.idLong }?.contains(peguinGuardID)!!) {
            if (guildId == null) {
                event.deferReply(true)
                    .setContent("Que diacho é esse que você escreveu meu amigo kk <a:floppaTeeth:849638419885195324>")
                    .queue()
                return
            }

            val guild = shardManager.getGuildById(guildId)

            if (guild !== null) {
                guild
                    .leave()
                    .queue()

                event.deferReply()
                    .setContent(
                        """
                            🚪 Eu saí de `${guild.name}` (`${guild.id}`) como foi pedido!
                            👑 Pertence à `${guild.owner?.user?.asTag}` (`${guild.owner?.id}`)
                            👥 E tem um total de `${guild.memberCount} membros`!
                        """.trimIndent()
                    )
                    .queue()
            } else {
                event.deferReply(true)
                    .setContent("Não estou em nenhuma guild com este ID! Verifique se está certo <a:00floppaearsfast:853465928036909126>")
                    .queue()
                return
            }
        } else {
            event.deferReply(true)
                .setContent("Você não tem permissão para usar este comando! <a:floppaTeeth:849638419885195324>")
                .queue()
            return
        }
    }
}