package net.perfectdreams.floppapower.tables

import net.perfectdreams.floppapower.utils.MetadataEntryType
import net.perfectdreams.floppapower.utils.exposed.jsonb
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.`java-time`.timestamp

object MessagesMetadata : LongIdTable() {
    val type = enumeration("type", MetadataEntryType::class)
    val comment = text("comment")
    val size = integer("size")
    val submittedBy = long("submitted_by").index()
    val approvedBy = jsonb("approved_by") // This is later converted into a proper thing
}