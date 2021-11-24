package net.perfectdreams.floppapower.commands.impl

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.sharding.ShardManager
import net.perfectdreams.floppapower.FloppaPower
import net.perfectdreams.floppapower.commands.AbstractSlashCommand
import net.perfectdreams.floppapower.listeners.SlashCommandListener
import net.perfectdreams.floppapower.utils.CheckedDueToType
import net.perfectdreams.floppapower.utils.Constants

class SameAvatarUserCommand(private val m: FloppaPower, private val shardManager: ShardManager) : AbstractSlashCommand("sameavatar/user") {
    override fun execute(event: SlashCommandEvent) {
        val avatarId = event.getOption("user")?.asUser?.avatarId

        if (avatarId == null || avatarId.length !in Constants.AVATAR_HASH_LENGTH) {
            event.deferReply(true)
                .setContent("Avatar não existe safado! <a:floppaTeeth:849638419885195324>")
                .queue()
            return
        }

        val matchedUsers = shardManager.userCache.filter { it.avatarId == avatarId }
        if (matchedUsers.isEmpty()) {
            event.deferReply(true)
                .setContent("Ninguém está usando esse avatar! <a:floppaTeeth:849638419885195324>")
                .queue()
            return
        }

        val first = matchedUsers.first()

        event.deferReply()
            .addEmbeds(
                EmbedBuilder()
                    .setThumbnail(first.effectiveAvatarUrl)
                    .addField("Avatar Hash", first.avatarId, true)
                    .setDescription(
                        buildString {
                            matchedUsers.sortedByDescending { it.timeCreated }.forEach {
                                append("**${it.name}** (`${it.idLong}`) [${Constants.DATE_FORMATTER.format(it.timeCreated)}]")
                                append("\n")
                            }
                        }.take(2000)
                    )
                    .build()
            ).queue()
    }
}