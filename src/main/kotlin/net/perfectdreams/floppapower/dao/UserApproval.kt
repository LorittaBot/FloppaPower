package net.perfectdreams.floppapower.dao

import net.perfectdreams.floppapower.tables.MultiEntriesMetadata
import net.perfectdreams.floppapower.tables.UsersApprovals
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class UserApproval(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<UserApproval>(UsersApprovals)

    var user by UsersApprovals.user
    var approvedEntries by UsersApprovals.approvedEntries
}