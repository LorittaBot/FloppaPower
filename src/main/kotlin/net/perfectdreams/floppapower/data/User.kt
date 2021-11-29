package net.perfectdreams.floppapower.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.LongAsStringSerializer

@Serializable
data class User(
    @Serializable(LongAsStringSerializer::class)
    val id: Long,
    val name: String,
    val discriminator: String,
    val avatarId: String?
)