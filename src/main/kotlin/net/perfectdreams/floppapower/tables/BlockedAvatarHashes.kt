package net.perfectdreams.floppapower.tables

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object BlockedAvatarHashes : IdTable<String>() {
    val avatarHash = text("avatar_hash").entityId().uniqueIndex()

    override val id: Column<EntityID<String>>
        get() = avatarHash

    val entry = reference("entry", MultiEntriesMetadata)
    val valid = bool("valid").default(true)
}