package net.perfectdreams.floppapower.commands.impl

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.sharding.ShardManager
import net.perfectdreams.floppapower.FloppaPower
import net.perfectdreams.floppapower.commands.AbstractSlashCommand

class LeaveGuildCommand(private val m: FloppaPower, private val shardManager: ShardManager) : AbstractSlashCommand("leaveguild") {
    override fun execute(event: SlashCommandEvent) {
        val guildId = event.getOption("guild_id")!!.asLong
        val allowedUsers = listOf(
            123170274651668480L,
            361977144445763585L
        )

        if (allowedUsers.contains(event.interaction.user.idLong)) {
            val guild = shardManager.getGuildById(guildId)

            if (guild !== null) {
                guild.leave()

                event.reply(
                    """
                    |Eu saí de `${guild.name}` (`${guild.id}`) como foi pedido!
                    |A guild tinha `${guild.memberCount} membros`!
                    |E pertencia à `${guild.owner?.user?.asTag}` (`${guild.owner?.id}`)
                    """.trimIndent()
                )
            } else {
                event.reply(
                    "Não estou em nenhuma guild com este ID! Verifique se está certo"
                )
            }
        } else {
            event.reply(
                "Você não tem permissão para usar este comando!"
            ).setEphemeral(true)
        }
    }
}