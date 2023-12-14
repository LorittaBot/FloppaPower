package net.perfectdreams.floppapower.commands.impl

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.sharding.ShardManager
import net.perfectdreams.floppapower.FloppaPower
import net.perfectdreams.floppapower.commands.AbstractSlashCommand
import net.perfectdreams.floppapower.listeners.SlashCommandListener
import net.perfectdreams.floppapower.utils.CheckedDueToType

class CheckGuildCommand(private val m: FloppaPower, private val shardManager: ShardManager) : AbstractSlashCommand("checkguild") {
    override fun execute(event: SlashCommandInteractionEvent) {
        val guildId = event.getOption("guild_id")?.asString?.toLongOrNull()

        if (guildId == null) {
            event.deferReply(true)
                .setContent("Não sei qual é esse servidor não fera <a:floppaTeeth:849638419885195324>")
                .queue()
            return
        }

        val guild = shardManager.getGuildById(guildId)

        if (guild == null) {
            event.deferReply(true)
                .setContent("Não sei qual é esse servidor não fera <a:floppaTeeth:849638419885195324>")
                .queue()
            return
        }

        if (SlashCommandListener.verificationMutex.isLocked) {
            event.deferReply(true)
                .setContent("Já tem uma verificação em andamento, espere ela acabar! <a:floppaTeeth:849638419885195324>")
                .queue()
            return
        }

        GlobalScope.launch {
            SlashCommandListener.verificationMutex.withLock {
                event.deferReply()
                    .setContent("Verificando... <a:floppaTeeth:849638419885195324>")
                    .queue()

                m.log(shardManager, "Verificando todos os membros em ${guild.name} (`${guild.idLong}`) pois ${event.user.asMention} pediu! Quantidade de membros: ${guild.memberCache.size()}")

                m.log(
                    shardManager,
                    "Verificando todos os membros para serem banidos no servidor ${guild.name} (`${guild.idLong}`), pedido por ${event.user.asMention}! :D"
                )

                val size = guild.memberCache.size()
                for ((index, member) in guild.memberCache.withIndex()) {
                    if (index % 10000 == 0) {
                        m.log(
                            shardManager,
                            "Verificando usuários para serem banidos em ${guild.name} (`${guild.idLong}`)... Progresso: ${index}/$size"
                        )
                    }

                    m.processIfMemberShouldBeBanned(shardManager, guild, member, CheckedDueToType.MANUAL_CHECK)
                }

                m.log(
                    shardManager,
                    "Verificando todos os membros para serem desbanidos no servidor ${guild.name} (`${guild.idLong}`), pedido por ${event.user.asMention}! :D"
                )
                m.processIfGuildHasMembersThatShouldBeUnbanned(shardManager, guild, CheckedDueToType.MANUAL_CHECK)
                m.log(
                    shardManager,
                    "Terminei de verificar os membros no servidor ${guild.name} (`${guild.idLong}`), pedido por ${event.user.asMention}! :D"
                )
            }
        }
    }
}