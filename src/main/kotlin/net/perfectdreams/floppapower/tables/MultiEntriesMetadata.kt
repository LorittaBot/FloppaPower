package net.perfectdreams.floppapower.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object MultiEntriesMetadata : LongIdTable() {
    val submittedBy = long("submitted_by").index()
    val comment = text("comment")
    val approvedAt = timestamp("approved_at")
}