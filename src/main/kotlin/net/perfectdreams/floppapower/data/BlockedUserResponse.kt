package net.perfectdreams.floppapower.data

import kotlinx.serialization.Serializable

@Serializable
data class BlockedUserResponse(
    val userId: Long,
    val entry: Long,
    val valid: Boolean
)