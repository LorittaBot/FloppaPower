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

        // Trying to avoid a humongous allocation due to array list resizes
        // The filter seems to only match 15459 of 2303084 users, that's waaaay less than expected!
        // We are going to create a array list that has / 128 of the size of the current user cache
        // Due to the checks we are doing below, we *probably* won't need to resize this... I hope.
        val userWithMutualGuilds = ArrayList<UserWithMutualGuilds>(
            (shardManager.userCache.size() / 128).toInt()
        )

        fun generateTopUsersMutualGuildsLines(): MutableList<String> {
            val lines = mutableListOf("Users:")

            userWithMutualGuilds
                .asSequence()
                .sortedWith(
                    compareBy(
                        {
                            // negative because we want it to be descending
                            -it.mutualGuilds.size
                        },
                        {
                            it.user.timeCreated
                        }
                    )
                )
                .take(MAX_USERS_PER_LIST)
                .forEach {
                    lines.addAll(generateUserInfoLines(shardManager, it.user, it.mutualGuilds).first)
                }

            return lines
        }

        // I don't think that this is a good idea because it will take a looong time I guess...
        var idx = 0
        val userCacheSize = shardManager.userCache.size()
        shardManager.userCache.forEach {
            // Ignore bots
            if (!it.isBot) {
                val userMutualGuilds = it.mutualGuilds

                // Ignore accounts that are in less than or equal to 3 servers (to speed up our checks)
                if (userMutualGuilds.size >= 3) {
                    // Ignore users that are in the EPF guild
                    if (!userMutualGuilds.any { it.idLong == Constants.ELITE_PENGUIN_FORCE_GUILD_ID })
                        userWithMutualGuilds.add(
                            UserWithMutualGuilds(
                                it,
                                userMutualGuilds
                            )
                        )
                }
            }

            idx++
        }

        hook.editOriginal("**Todos os $userCacheSize usuários foram verificados! (${userWithMutualGuilds.size} válidos baseado no filtro)** <a:SCfloppaEARflop2:750859905858142258>\nResultado apenas possui os top $MAX_USERS_PER_LIST usuários, ignorando bots e usuários que estão na EPF!")
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

    data class UserWithMutualGuilds(
        val user: User,
        val mutualGuilds: List<Guild>
    )
}