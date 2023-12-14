package net.perfectdreams.floppapower.commands.impl

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.sharding.ShardManager
import net.perfectdreams.floppapower.FloppaPower
import net.perfectdreams.floppapower.commands.AbstractSlashCommand

class PingCommand(private val m: FloppaPower, private val shardManager: ShardManager) : AbstractSlashCommand("ping") {
    override fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply(false)
            .setContent("floppa flopper flop <a:floppaTeeth:849638419885195324>")
            .queue()
    }
}