package net.perfectdreams.floppapower.data

import kotlinx.serialization.Serializable

@Serializable
data class UserWithRole(
    val userId: Long,
    val roleId: Long
)