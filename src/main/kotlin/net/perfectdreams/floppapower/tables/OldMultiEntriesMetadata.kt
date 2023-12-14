package net.perfectdreams.floppapower.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object OldMultiEntriesMetadata : LongIdTable() {
    override val tableName = "MultiEntriesMetadata"

    val submittedBy = long("submitted_by").index()
    val comment = text("comment")
    val approvedAt = long("approved_at")
}