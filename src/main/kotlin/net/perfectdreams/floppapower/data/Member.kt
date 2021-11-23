package net.perfectdreams.floppapower.data

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Member(
    val nickname: String?,
    val joinedAt: Instant,
    val user: User
)