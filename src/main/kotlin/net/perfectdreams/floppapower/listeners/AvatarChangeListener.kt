package net.perfectdreams.floppapower.listeners

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.user.update.UserUpdateAvatarEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.sharding.ShardManager
import net.perfectdreams.floppapower.FloppaPower
import net.perfectdreams.floppapower.utils.CheckedDueToType

class AvatarChangeListener(private val m: FloppaPower, private val shardManager: ShardManager) : ListenerAdapter() {
    override fun onUserUpdateAvatar(event: UserUpdateAvatarEvent) {
        if (event.user.isBot) // We don't care about bots
            return

        GlobalScope.launch {
            shardManager.getMutualGuilds(event.user).forEach {
                // should never be null!
                m.processIfMemberShouldBeBanned(shardManager, it, it.getMember(event.user)!!, CheckedDueToType.USER_AVATAR_CHANGE)
            }
        }
    }
}