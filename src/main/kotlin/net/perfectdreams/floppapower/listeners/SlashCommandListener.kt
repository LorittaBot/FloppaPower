package net.perfectdreams.floppapower.listeners

import kotlinx.coroutines.sync.Mutex
import mu.KotlinLogging
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.perfectdreams.floppapower.FloppaPower
import net.perfectdreams.floppapower.commands.impl.CheckGuildCommand
import net.perfectdreams.floppapower.commands.impl.CheckGuildsCommand
import net.perfectdreams.floppapower.commands.impl.CheckSimilarAvatarsCommand
import net.perfectdreams.floppapower.commands.impl.GuildsCommand
import net.perfectdreams.floppapower.commands.impl.SameAvatarHashCommand
import net.perfectdreams.floppapower.commands.impl.SameAvatarUserCommand
import net.perfectdreams.floppapower.commands.impl.SharedGuildsCommand
import net.perfectdreams.floppapower.commands.impl.SusJoinsCommand
import net.perfectdreams.floppapower.utils.FloppaButtonClickEvent
import java.util.*

class SlashCommandListener(private val m: FloppaPower) : ListenerAdapter() {
    companion object {
        private val logger = KotlinLogging.logger {}
        val verificationMutex = Mutex()
    }

    private val commands = listOf(
        GuildsCommand(),
        CheckGuildCommand(m),
        CheckGuildsCommand(m),
        SameAvatarHashCommand(m),
        SharedGuildsCommand(m),
        CheckSimilarAvatarsCommand(m),
        SameAvatarHashCommand(m),
        SameAvatarUserCommand(m),
        SusJoinsCommand(m)
    )

    override fun onSlashCommand(event: SlashCommandEvent) {
        logger.info { "Received Slash Command ${event.name} from ${event.user}" }

        val command = commands.firstOrNull { it.commandPath == event.commandPath }

        if (command != null) {
            command.execute(event)
            return
        }
    }

    override fun onButtonClick(event: ButtonClickEvent) {
        try {
            val uniqueId = UUID.fromString(event.componentId)
            m.slashCommandButtonInteractionCache[uniqueId]?.invoke(
                FloppaButtonClickEvent(m, uniqueId, event)
            )
        } catch (e: IllegalArgumentException) {}
    }
}