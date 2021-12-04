package net.perfectdreams.floppapower.commands.impl

import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.sharding.ShardManager
import net.perfectdreams.floppapower.commands.AbstractSlashCommand


class SearchUsersCommand(private val shardManager: ShardManager) : AbstractSlashCommand("guilds") {
    override fun execute(event: SlashCommandEvent) {
        event.deferReply().queue()
        val hook = event.hook // This is a special webhook that allows you to send messages without having permissions in the channel and also allows ephemeral messages

        val regexPatternAsString = event.getOption("pattern")?.asString!!
        val regex = Regex(regexPatternAsString, RegexOption.IGNORE_CASE)

        val matchedUsers = mutableListOf<User>()
        var tooManyUsers = false

        shardManager.userCache.forEach {
            if (matchedUsers.size >= 1000) {
                tooManyUsers = true
                return@forEach
            }

            if (it.name.matches(regex)) {
                matchedUsers.add(it)
            }
        }

        val builder = StringBuilder("Users (${matchedUsers.size}):")
        builder.append("\n")
        matchedUsers.forEach {
            builder.append("${it.name}#${it.discriminator} (${it.idLong})")
            builder.append("\n")
        }

        hook
            .editOriginal(if (tooManyUsers) "Tem tantos usuários que eu limitei a 1000 usuários! <a:floppaTeeth:849638419885195324>" else "<a:SCfloppaEARflop2:750859905858142258>")
            .addFile(builder.toString().toByteArray(Charsets.UTF_8), "users.txt")
            .queue()
    }
}