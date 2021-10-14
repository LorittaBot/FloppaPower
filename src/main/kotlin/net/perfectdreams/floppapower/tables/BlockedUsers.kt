package net.perfectdreams.floppapower.tables

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object BlockedUsers : IdTable<Long>() {
    val userId = long("user").entityId().uniqueIndex()

    override val id: Column<EntityID<Long>>
        get() = userId

    val entry = reference("entry", MultiEntriesMetadata)
    val valid = bool("valid").default(true)
}