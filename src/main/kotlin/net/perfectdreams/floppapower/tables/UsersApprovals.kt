package net.perfectdreams.floppapower.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object UsersApprovals : LongIdTable() {
    val user = long("user").index()
    val approvedEntries = reference("approved_entries", MultiEntriesMetadata)
}