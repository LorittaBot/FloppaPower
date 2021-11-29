package net.perfectdreams.floppapower.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.LongAsStringSerializer

@Serializable
data class BlockedUserResponse(
    @Serializable(LongAsStringSerializer::class)
    val userId: Long,
    val entry: Long,
    val valid: Boolean
)