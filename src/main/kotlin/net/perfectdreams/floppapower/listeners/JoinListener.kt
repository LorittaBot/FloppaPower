package net.perfectdreams.floppapower.listeners

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.sharding.ShardManager
import net.perfectdreams.floppapower.FloppaPower
import net.perfectdreams.floppapower.utils.CheckedDueToType

class JoinListener(private val m: FloppaPower, private val shardManager: ShardManager) : ListenerAdapter() {
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        if (event.user.isBot) // We don't care about bots
            return

        GlobalScope.launch {
            m.processIfMemberShouldBeBanned(shardManager, event.guild, event.member, CheckedDueToType.MEMBER_GUILD_JOIN)
        }
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
        m.log(shardManager, "Entrei no servidor ${event.guild.name} (`${event.guild.idLong}`) :3 Irei tentar analisar todos os membros do servidor para ver se tem algum meliante escondido... Quantidade de membros: ${event.guild.memberCache.size()}")

        GlobalScope.launch {
            for (member in event.guild.memberCache) {
                m.processIfMemberShouldBeBanned(shardManager, event.guild, member, CheckedDueToType.FLOPPA_JOIN)
            }

            m.log(shardManager, "Terminei de verificar os membros no servidor ${event.guild.name} (`${event.guild.idLong}`)! :D")
        }
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        m.log(shardManager, "Sai do servidor ${event.guild.name} (`${event.guild.idLong}`) :(")
    }
}