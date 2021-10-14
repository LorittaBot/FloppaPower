package net.perfectdreams.floppapower.dao

import net.perfectdreams.floppapower.tables.BlockedAvatarHashesBanEntries
import net.perfectdreams.floppapower.tables.BlockedUserBanEntries
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class BlockedAvatarHashBanEntry(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<BlockedAvatarHashBanEntry>(BlockedAvatarHashesBanEntries)

    var user by BlockedAvatarHashesBanEntries.user
    var guild by BlockedAvatarHashesBanEntries.guild
    var bannedAt by BlockedAvatarHashesBanEntries.bannedAt
    var blockEntry by BlockedAvatarHash referencedOn BlockedAvatarHashesBanEntries.blockEntry
}