package net.perfectdreams.floppapower.utils

import net.dv8tion.jda.api.interactions.components.Button
import net.dv8tion.jda.api.interactions.components.ButtonStyle
import net.perfectdreams.floppapower.FloppaPower
import java.util.*
import javax.annotation.Nonnull

object FloppaButton {
    @Nonnull
    fun of(m: FloppaPower, style: ButtonStyle, label: String, callback: (FloppaButtonClickEvent) -> (Unit)): Button {
        val uniqueId = UUID.randomUUID()
        m.slashCommandButtonInteractionCache[uniqueId] = callback
        return Button.of(style, uniqueId.toString(), label)
    }
}