package net.perfectdreams.floppapower.dao

import net.perfectdreams.floppapower.tables.BlockedUsers
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class BlockedUser(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<BlockedUser>(BlockedUsers)

    var entry by MultiEntryMetadata referencedOn BlockedUsers.entry
    val valid by BlockedUsers.valid
}