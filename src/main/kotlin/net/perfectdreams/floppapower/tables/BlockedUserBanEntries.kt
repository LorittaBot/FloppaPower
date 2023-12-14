package net.perfectdreams.floppapower.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object BlockedUserBanEntries : LongIdTable() {
    val user = long("user").index()
    val guild = long("guild").index()
    val bannedAt = timestamp("banned_at")
    val blockEntry = reference("block_entry", BlockedUsers)
}