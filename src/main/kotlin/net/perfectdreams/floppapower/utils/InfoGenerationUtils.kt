package net.perfectdreams.floppapower.utils

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager

object InfoGenerationUtils {
    fun generateUserInfoLines(shardManager: ShardManager, user: User) = generateUserInfoLines(user, shardManager.getMutualGuilds(user))

    fun generateUserInfoLines(user: User, mutualGuilds: List<Guild>): Pair<List<String>, List<Member>> {
        // Show the user name, if possible
        val newLines = mutableListOf<String>()
        val attentionMembers = mutableListOf<Member>()
        newLines.add("# \uD83D\uDE10 ${user.name}#${user.discriminator} (${user.idLong}) [${Constants.DATE_FORMATTER.format(user.timeCreated)}]")
        newLines.add("# ┗ \uD83D\uDD16️ Flags: ${user.flags.joinToString(", ")}")

        if (mutualGuilds.isNotEmpty()) {
            newLines.add("# ┗ \uD83C\uDFE0 Servidores:")
            mutualGuilds.forEach { guild ->
                val member = guild.getMember(user)
                // They could've left the server by the time this is happening
                if (member != null) {
                    newLines.add(
                        "# ┗━ \uD83C\uDFE0 ${guild.name} (${member.roles.joinToString(", ") { it.name }}) [${
                            Constants.DATE_FORMATTER.format(
                                member.timeJoined
                            )
                        }]"
                    )
                    if (member.roles.any {
                            Constants.TRUSTED_ROLES.any { trustedRoleName ->
                                it.name.contains(
                                    trustedRoleName,
                                    true
                                )
                            }
                        } || member.hasPermission(
                            Constants.TRUSTED_PERMISSIONS
                        )) {
                        attentionMembers.add(member)
                    }
                }
            }
        }
        // newLines.add(user.idLong.toString())
        return Pair(newLines, attentionMembers)
    }
}