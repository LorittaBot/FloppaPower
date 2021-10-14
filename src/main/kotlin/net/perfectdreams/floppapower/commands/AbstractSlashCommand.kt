package net.perfectdreams.floppapower.commands

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent

abstract class AbstractSlashCommand(val commandPath: String) {
    abstract fun execute(event: SlashCommandEvent)
}