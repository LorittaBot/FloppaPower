package net.perfectdreams.floppapower.commands.impl

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.sharding.ShardManager
import net.perfectdreams.floppapower.commands.AbstractSlashCommand
import net.perfectdreams.floppapower.utils.Constants


class TopMutualUsersCommand(private val shardManager: ShardManager) : AbstractSlashCommand("topmutualusers") {
    companion object {
        // 1000 users is around 493 KB
        // so let's to 15_000 users!
        const val MAX_USERS_PER_LIST = 15_000
    }

    override fun execute(event: SlashCommandEvent) {
        event.deferReply().queue()
        val hook = event.hook // This is a special webhook that allows you to send messages without having permissions in the channel and also allows ephemeral messages

        val mutualGuilds = mutableMapOf<User, List<Guild>>()

        fun generateTopUsersMutualGuildsLines(): MutableList<String> {
            val lines = mutableListOf("Users:")

            mutualGuilds
                .toList()
                .sortedWith(
                    compareBy(
                        {
                            // negative because we want it to be descending
                            -it.second.size
                        },
                        {
                            it.first.timeCreated
                        }
                    )
                )
                .take(MAX_USERS_PER_LIST)
                .forEach {
                    lines.addAll(generateUserInfoLines(shardManager, it.first, it.second).first)
                }

            return lines
        }

        // I don't think that this is a good idea because it will take a looong time I guess...
        var idx = 0
        val userCacheSize = shardManager.userCache.size()
        shardManager.userCache.forEach {
            if (idx % 250_000 == 0 && idx != 0)
                hook.editOriginal("**Usuários verificados:** $idx/$userCacheSize <a:floppaTeeth:849638419885195324>\nResultado apenas possui os top $MAX_USERS_PER_LIST usuários, ignorando bots e usuários que estão na EPF!")
                    .retainFiles(listOf()) // Remove all files from the message
                    .addFile(generateTopUsersMutualGuildsLines().joinToString("\n").toByteArray(Charsets.UTF_8), "users.txt")
                    .queue()

            // Ignore bots
            if (!it.isBot) {
                val userMutualGuilds = it.mutualGuilds

                // Ignore accounts that are in less than or equal to 3 servers (to speed up our checks)
                if (userMutualGuilds.size >= 3) {
                    // Ignore users that are in the EPF guild
                    if (!userMutualGuilds.any { it.idLong == Constants.ELITE_PENGUIN_FORCE_GUILD_ID })
                        mutualGuilds[it] = userMutualGuilds
                }
            }

            idx++
        }

        hook.editOriginal("**Todos os $userCacheSize usuários foram verificados!** <a:SCfloppaEARflop2:750859905858142258>\nResultado apenas possui os top $MAX_USERS_PER_LIST usuários, ignorando bots e usuários que estão na EPF!")
            .retainFiles(listOf()) // Remove all files from the message
            .addFile(generateTopUsersMutualGuildsLines().joinToString("\n").toByteArray(Charsets.UTF_8), "users.txt")
            .queue()
    }

    // Stolen from MessageListener, maybe we should refactor the code later...
    private fun generateUserInfoLines(shardManager: ShardManager, user: User, mutualGuilds: List<Guild>): Pair<List<String>, List<Member>> {
        // Show the user name, if possible
        val newLines = mutableListOf<String>()
        val attentionMembers = mutableListOf<Member>()
        newLines.add("# \uD83D\uDE10 ${user.name}#${user.discriminator} (${user.idLong}) [${Constants.DATE_FORMATTER.format(user.timeCreated)}]")
        newLines.add("# ┗ \uD83D\uDD16️ Flags: ${user.flags.joinToString(", ")}")

        if (mutualGuilds.isNotEmpty()) {
            newLines.add("# ┗ \uD83C\uDFE0 Servidores:")
            mutualGuilds.forEach { guild ->
                val member = guild.getMember(user)!!
                newLines.add("# ┗━ \uD83C\uDFE0 ${guild.name} (${member.roles.joinToString(", ") { it.name }}) [${Constants.DATE_FORMATTER.format(member.timeJoined)}]")
                if (member.roles.any { Constants.TRUSTED_ROLES.any { trustedRoleName -> it.name.contains(trustedRoleName, true) }} || member.hasPermission(
                        Constants.TRUSTED_PERMISSIONS)) {
                    attentionMembers.add(member)
                }
            }
        }
        // newLines.add(user.idLong.toString())
        return Pair(newLines, attentionMembers)
    }
}