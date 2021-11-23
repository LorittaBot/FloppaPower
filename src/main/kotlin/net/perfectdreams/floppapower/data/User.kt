package net.perfectdreams.floppapower.data

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val name: String,
    val discriminator: String,
    val avatarId: String?
)