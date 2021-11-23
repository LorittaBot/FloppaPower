package net.perfectdreams.floppapower.data

import kotlinx.serialization.Serializable

@Serializable
data class BlockedAvatarHashResponse(
    val avatarHash: String,
    val entry: Long,
    val valid: Boolean
)