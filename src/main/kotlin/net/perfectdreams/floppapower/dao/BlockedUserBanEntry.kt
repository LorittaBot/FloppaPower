package net.perfectdreams.floppapower.dao

import net.perfectdreams.floppapower.tables.BlockedUserBanEntries
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class BlockedUserBanEntry(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<BlockedUserBanEntry>(BlockedUserBanEntries)

    var user by BlockedUserBanEntries.user
    var guild by BlockedUserBanEntries.guild
    var bannedAt by BlockedUserBanEntries.bannedAt
    var blockEntry by BlockedUser referencedOn BlockedUserBanEntries.blockEntry
}