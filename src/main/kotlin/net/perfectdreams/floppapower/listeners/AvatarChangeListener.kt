package net.perfectdreams.floppapower.listeners

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.user.update.UserUpdateAvatarEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.perfectdreams.floppapower.FloppaPower
import net.perfectdreams.floppapower.utils.CheckedDueToType

class AvatarChangeListener(private val m: FloppaPower) : ListenerAdapter() {
    override fun onUserUpdateAvatar(event: UserUpdateAvatarEvent) {
        if (event.user.isBot) // We don't care about bots
            return

        GlobalScope.launch {
            event.jda.getMutualGuilds(event.user).forEach {
                // should never be null!
                m.processIfMemberShouldBeBanned(it, it.getMember(event.user)!!, CheckedDueToType.USER_AVATAR_CHANGE)
            }
        }
    }
}