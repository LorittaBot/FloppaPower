package net.perfectdreams.floppapower.dao

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.perfectdreams.floppapower.data.UserWithRole
import net.perfectdreams.floppapower.tables.MessagesMetadata
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class MessageMetadata(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<MessageMetadata>(MessagesMetadata)

    var type by MessagesMetadata.type
    var comment by MessagesMetadata.comment
    var size by MessagesMetadata.size
    var submittedBy by MessagesMetadata.submittedBy
    var approvedBy by MessagesMetadata.approvedBy

    val approvedByAsList: List<UserWithRole>
        get() = Json.decodeFromString(approvedBy)
}