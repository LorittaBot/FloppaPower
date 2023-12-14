package net.perfectdreams.floppapower.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object OldBlockedAvatarHashesBanEntries : LongIdTable() {
    override val tableName = "BlockedAvatarHashesBanEntries"
    val user = long("user").index()
    val guild = long("guild").index()
    val bannedAt = long("banned_at")
    val blockEntry = reference("block_entry", BlockedAvatarHashes)
}