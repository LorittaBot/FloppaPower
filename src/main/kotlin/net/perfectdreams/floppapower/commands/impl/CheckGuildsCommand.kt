package net.perfectdreams.floppapower.commands.impl

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.perfectdreams.floppapower.FloppaPower
import net.perfectdreams.floppapower.commands.AbstractSlashCommand
import net.perfectdreams.floppapower.listeners.SlashCommandListener
import net.perfectdreams.floppapower.utils.CheckedDueToType

class CheckGuildsCommand(private val m: FloppaPower) : AbstractSlashCommand("checkguilds") {
    override fun execute(event: SlashCommandEvent) {
        if (SlashCommandListener.verificationMutex.isLocked) {
            event.deferReply(true)
                .setContent("Já tem uma verificação em andamento, espere ela acabar! <a:floppaTeeth:849638419885195324>")
                .queue()
            return
        }

        GlobalScope.launch {
            SlashCommandListener.verificationMutex.withLock {
                event.deferReply()
                    .setContent("Verificando todos... <a:floppaTeeth:849638419885195324>")
                    .queue()

                for (guild in event.jda.guildCache) {
                    m.log(
                        event.jda,
                        "Verificando todos os membros em ${guild.name} (`${guild.idLong}`) pois ${event.user.asMention} pediu! Quantidade de membros: ${guild.memberCache.size()}"
                    )

                    val size = guild.memberCache.size()
                    for ((index, member) in guild.memberCache.withIndex()) {
                        if (index % 10000 == 0) {
                            m.log(
                                event.jda,
                                "Verificando usuários para serem banidos em ${guild.name} (`${guild.idLong}`)... Progresso: ${index}/$size"
                            )
                        }

                        m.processIfMemberShouldBeBanned(guild, member, CheckedDueToType.MANUAL_CHECK)
                    }

                    m.log(
                        event.jda,
                        "Verificando todos os membros para serem desbanidos no servidor ${guild.name} (`${guild.idLong}`), pedido por ${event.user.asMention}! :D"
                    )
                    m.processIfGuildHasMembersThatShouldBeUnbanned(guild, CheckedDueToType.MANUAL_CHECK)
                    m.log(
                        event.jda,
                        "Terminei de verificar os membros no servidor ${guild.name} (`${guild.idLong}`), pedido por ${event.user.asMention}! :D"
                    )
                }
            }
        }
    }
}