package net.perfectdreams.floppapower.commands.impl

import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.sharding.ShardManager
import net.perfectdreams.floppapower.commands.AbstractSlashCommand
import net.perfectdreams.floppapower.utils.InfoGenerationUtils


class SearchUsersCommand(private val shardManager: ShardManager) : AbstractSlashCommand("searchusers") {
    companion object {
        // 1000 users is around 493 KB
        // so let's to 15_000 users!
        const val MAX_USERS_PER_LIST = 15_000
    }

    override fun execute(event: SlashCommandEvent) {
        event.deferReply().queue()
        val hook = event.hook // This is a special webhook that allows you to send messages without having permissions in the channel and also allows ephemeral messages

        val regexPatternAsString = event.getOption("pattern")?.asString!!
        val regex = Regex(regexPatternAsString, RegexOption.IGNORE_CASE)

        val matchedUsers = mutableListOf<User>()
        var tooManyUsers = false

        shardManager.userCache.forEach {
            if (matchedUsers.size >= MAX_USERS_PER_LIST) {
                tooManyUsers = true
                return@forEach
            }

            if (it.name.matches(regex)) {
                matchedUsers.add(it)
            }
        }

        val builder = StringBuilder("Users (${matchedUsers.size}):")
        builder.append("\n")
        matchedUsers.sortedBy { it.timeCreated }.forEach {
            InfoGenerationUtils.generateUserInfoLines(shardManager, it, it.mutualGuilds).first.forEach {
                builder.append(it)
            }
            builder.append("\n")
        }

        hook
            .editOriginal(if (tooManyUsers) "Tem tantos usuários que eu limitei a $MAX_USERS_PER_LIST usuários! <a:floppaTeeth:849638419885195324>" else "<a:SCfloppaEARflop2:750859905858142258>")
            .addFile(builder.toString().toByteArray(Charsets.UTF_8), "users.txt")
            .queue()
    }
}