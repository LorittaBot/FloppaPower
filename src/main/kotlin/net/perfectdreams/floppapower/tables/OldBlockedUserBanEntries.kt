package net.perfectdreams.floppapower.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.`java-time`.timestamp

object OldBlockedUserBanEntries : LongIdTable() {
    override val tableName = "BlockedUserBanEntries"

    val user = long("user").index()
    val guild = long("guild").index()
    val bannedAt = long("banned_at")
    val blockEntry = reference("block_entry", BlockedUsers)
}