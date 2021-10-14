package net.perfectdreams.floppapower.commands.impl

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.perfectdreams.floppapower.commands.AbstractSlashCommand

class GuildsCommand : AbstractSlashCommand("guilds") {
    override fun execute(event: SlashCommandEvent) {
        val builder = StringBuilder("Guilds (${event.jda.guildCache.size()})")
        builder.append("\n")
        event.jda.guildCache.sortedByDescending { it.memberCache.size() }.forEach {
            builder.append("${it.name} (${it.idLong}) [${it.memberCache.size()} membros]")
            builder.append("\n")
        }

        event.deferReply()
            .addFile(builder.toString().toByteArray(Charsets.UTF_8), "guilds.txt")
            .queue()
    }
}