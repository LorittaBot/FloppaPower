package net.perfectdreams.floppapower.listeners

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.perfectdreams.floppapower.FloppaPower
import net.perfectdreams.floppapower.utils.Constants

class FloppaGangButtonListener(private val m: FloppaPower) : ListenerAdapter() {
    override fun onButtonClick(event: ButtonClickEvent) {
        if (event.componentId != "get_floppa_role")
            return

        val guild = event.guild!!
        val member = event.member!!
        val role = guild.getRoleById(Constants.FLOPPA_GANG_ROLE_ID)!!

        if (member.roles.contains(role)) {
            guild.removeRoleFromMember(member, role).queue()
            event.deferReply(true)
                .setContent("aaa pra que sair da floppa gang #decepcionado")
                .queue()
        } else {
            guild.addRoleToMember(member, role).queue()
            event.deferReply(true)
                .setContent("Bem-vind@ a Floppa Gang! Espero que goste!! E não se esqueça de ficar atento nas <#852513688102633472>! <a:floppaTeeth:849638419885195324>")
                .queue()
        }
    }
}