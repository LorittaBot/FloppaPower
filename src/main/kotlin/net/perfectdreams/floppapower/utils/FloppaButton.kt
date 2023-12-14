package net.perfectdreams.floppapower.utils

import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.perfectdreams.floppapower.FloppaPower
import java.util.*

object FloppaButton {
    fun of(m: FloppaPower, style: ButtonStyle, label: String, callback: (FloppaButtonClickEvent) -> (Unit)): Button {
        val uniqueId = UUID.randomUUID()
        m.slashCommandButtonInteractionCache[uniqueId] = callback
        return Button.of(style, uniqueId.toString(), label)
    }
}