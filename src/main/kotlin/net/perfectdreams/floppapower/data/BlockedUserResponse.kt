package net.perfectdreams.floppapower.data

data class BlockedUserResponse(
    val userId: Long,
    val entry: Long,
    val valid: Boolean
)