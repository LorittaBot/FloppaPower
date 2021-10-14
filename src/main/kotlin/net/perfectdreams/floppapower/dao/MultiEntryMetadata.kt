package net.perfectdreams.floppapower.dao

import net.perfectdreams.floppapower.tables.MultiEntriesMetadata
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class MultiEntryMetadata(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<MultiEntryMetadata>(MultiEntriesMetadata)

    var submittedBy by MultiEntriesMetadata.submittedBy
    var comment by MultiEntriesMetadata.comment
    var approvedAt by MultiEntriesMetadata.approvedAt
}