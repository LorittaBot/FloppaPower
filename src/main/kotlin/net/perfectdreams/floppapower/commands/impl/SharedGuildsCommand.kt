package net.perfectdreams.floppapower.commands.impl

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.perfectdreams.floppapower.FloppaPower
import net.perfectdreams.floppapower.commands.AbstractSlashCommand

class SharedGuildsCommand(private val m: FloppaPower) : AbstractSlashCommand("sharedguilds") {
    override fun execute(event: SlashCommandEvent) {
        val user = event.getOption("user")!!.asUser
        val mutualGuilds = event.jda.getMutualGuilds(user)

        event.deferReply()
            .addEmbeds(
                EmbedBuilder()
                    .setTitle(user.name)
                    .setThumbnail(user.effectiveAvatarUrl)
                    .addField("Servidores", mutualGuilds.joinToString("\n") { it.name}, true)
                    .build()
            ).queue()
    }
}