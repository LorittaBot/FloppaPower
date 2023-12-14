package net.perfectdreams.floppapower.commands

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

abstract class AbstractSlashCommand(val commandPath: String) {
    abstract fun execute(event: SlashCommandInteractionEvent)
}