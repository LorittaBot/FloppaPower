package net.perfectdreams.floppapower.data

data class BlockedAvatarHashResponse(
    val avatarHash: String,
    val entry: Long,
    val valid: Boolean
)