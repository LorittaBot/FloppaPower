package net.perfectdreams.floppapower.commands.impl

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.sharding.ShardManager
import net.perfectdreams.floppapower.commands.AbstractSlashCommand
import net.perfectdreams.floppapower.commands.impl.SearchUsersCommand.Companion.MAX_USERS_PER_LIST
import net.perfectdreams.floppapower.utils.Constants
import net.perfectdreams.floppapower.utils.InfoGenerationUtils

class TopMutualUsersCommand(private val shardManager: ShardManager) : AbstractSlashCommand("topmutualusers") {
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

        fun generateTopUsersMutualGuildsLines(): Pair<Int, MutableList<String>> {
            val lines = mutableListOf("Users:")
            var successfullyAddedUsers = 0

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
                    val linesToBeAdded = InfoGenerationUtils.generateUserInfoLines(it.user, it.mutualGuilds).first

                    // Current size + lines to be added + new line length
                    // If it is bigger than 8000, we are going to ignore it
                    if (lines.sumOf { it.length } + linesToBeAdded.sumOf { it.length } + 1 > 8000)
                        return@forEach

                    lines.addAll(linesToBeAdded)
                    lines.add("\n")

                    successfullyAddedUsers++
                }

            return Pair(successfullyAddedUsers, lines)
        }

        // I don't think that this is a good idea because it will take a looong time I guess...
        var idx = 0
        val userCacheSize = shardManager.userCache.size()
        shardManager.userCache.forEach {
            // Ignore bots
            if (!it.isBot) {
                val userMutualGuilds = shardManager.getMutualGuilds(it)

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

        val (successfullyAddedUsers, lines) = generateTopUsersMutualGuildsLines()
        hook.editOriginal("**Todos os $userCacheSize usuários verificados! (${userWithMutualGuilds.size} válidos baseado no filtro)** <a:SCfloppaEARflop2:750859905858142258>\nResultado apenas possui os top $successfullyAddedUsers usuários, ignorando bots e usuários que estão na EPF!")
            .retainFiles(listOf()) // Remove all files from the message
            .addFile(
                lines
                    .joinToString("\n")
                    .toByteArray(Charsets.UTF_8),
                "users.txt"
            )
            .queue()
    }

    data class UserWithMutualGuilds(
        val user: User,
        val mutualGuilds: List<Guild>
    )
}