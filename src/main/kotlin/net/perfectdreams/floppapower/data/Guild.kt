package net.perfectdreams.floppapower.data

import kotlinx.serialization.Serializable

@Serializable
data class Guild(
    val id: Long,
    val name: String,
    val member: Member
)