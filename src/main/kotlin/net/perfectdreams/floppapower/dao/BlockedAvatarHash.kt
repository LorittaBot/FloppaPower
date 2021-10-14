package net.perfectdreams.floppapower.dao

import net.perfectdreams.floppapower.tables.BlockedAvatarHashes
import org.jetbrains.exposed.dao.id.EntityID

class BlockedAvatarHash(id: EntityID<String>) : TextEntity(id) {
    companion object : TextEntityClass<BlockedAvatarHash>(BlockedAvatarHashes)

    var entry by MultiEntryMetadata referencedOn BlockedAvatarHashes.entry
    val valid by BlockedAvatarHashes.valid
}