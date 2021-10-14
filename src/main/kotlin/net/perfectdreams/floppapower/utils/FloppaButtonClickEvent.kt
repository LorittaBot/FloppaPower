package net.perfectdreams.floppapower.utils

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.perfectdreams.floppapower.FloppaPower
import java.util.*

class FloppaButtonClickEvent(val m: FloppaPower, val uniqueId: UUID, val event: ButtonClickEvent) {
    fun invalidate() {
        m.slashCommandButtonInteractionCache.remove(uniqueId)
    }
}