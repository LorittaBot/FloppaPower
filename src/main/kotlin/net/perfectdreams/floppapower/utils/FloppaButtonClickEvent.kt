package net.perfectdreams.floppapower.utils

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.perfectdreams.floppapower.FloppaPower
import java.util.*

class FloppaButtonClickEvent(val m: FloppaPower, val uniqueId: UUID, val event: ButtonInteractionEvent) {
    fun invalidate() {
        m.slashCommandButtonInteractionCache.remove(uniqueId)
    }
}