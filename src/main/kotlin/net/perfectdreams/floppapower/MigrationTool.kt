package net.perfectdreams.floppapower

import net.perfectdreams.floppapower.tables.BlockedAvatarHashes
import net.perfectdreams.floppapower.tables.BlockedAvatarHashesBanEntries
import net.perfectdreams.floppapower.tables.BlockedUserBanEntries
import net.perfectdreams.floppapower.tables.BlockedUsers
import net.perfectdreams.floppapower.tables.MessagesMetadata
import net.perfectdreams.floppapower.tables.MultiEntriesMetadata
import net.perfectdreams.floppapower.tables.OldBlockedAvatarHashesBanEntries
import net.perfectdreams.floppapower.tables.OldBlockedUserBanEntries
import net.perfectdreams.floppapower.tables.OldMultiEntriesMetadata
import net.perfectdreams.floppapower.tables.UsersApprovals
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object MigrationTool {
    fun migrate(database: Database, sqliteDatabase: Database) {
        val multiEntriesMetadata = transaction(sqliteDatabase) {
            OldMultiEntriesMetadata.selectAll()
                .toList()
        }

        transaction(database) {
            for (entry in multiEntriesMetadata) {
                MultiEntriesMetadata.insert {
                    it[MultiEntriesMetadata.id] = entry[OldMultiEntriesMetadata.id]
                    it[MultiEntriesMetadata.comment] = entry[OldMultiEntriesMetadata.comment]
                    it[MultiEntriesMetadata.submittedBy] = entry[OldMultiEntriesMetadata.submittedBy]
                    it[MultiEntriesMetadata.approvedAt] = Instant.ofEpochMilli(entry[OldMultiEntriesMetadata.approvedAt])
                }
            }
        }

        val usersApprovals = transaction(sqliteDatabase) {
            UsersApprovals.selectAll()
                .toList()
        }

        transaction(database) {
            for (entry in usersApprovals) {
                UsersApprovals.insert {
                    it[UsersApprovals.id] = entry[UsersApprovals.id]
                    it[UsersApprovals.user] = entry[UsersApprovals.user]
                    it[UsersApprovals.approvedEntries] = entry[UsersApprovals.approvedEntries]
                }
            }
        }

        val messagesMetadata = transaction(sqliteDatabase) {
            MessagesMetadata.selectAll()
                .toList()
        }

        transaction(database) {
            for (entry in messagesMetadata) {
                MessagesMetadata.insert {
                    it[MessagesMetadata.id] = entry[MessagesMetadata.id]
                    it[MessagesMetadata.type] = entry[MessagesMetadata.type]
                    it[MessagesMetadata.comment] = entry[MessagesMetadata.comment]
                    it[MessagesMetadata.size] = entry[MessagesMetadata.size]
                    it[MessagesMetadata.submittedBy] = entry[MessagesMetadata.submittedBy]
                    it[MessagesMetadata.approvedBy] = entry[MessagesMetadata.approvedBy]
                }
            }
        }

        val blockedUsers = transaction(sqliteDatabase) {
            BlockedUsers.selectAll()
                .toList()
        }

        transaction(database) {
            for (entry in blockedUsers) {
                BlockedUsers.insert {
                    it[BlockedUsers.id] = entry[BlockedUsers.id]
                    it[BlockedUsers.entry] = entry[BlockedUsers.entry]
                    it[BlockedUsers.valid] = entry[BlockedUsers.valid]
                }
            }
        }

        val blockedAvatarHashes = transaction(sqliteDatabase) {
            BlockedAvatarHashes.selectAll()
                .toList()
        }

        transaction(database) {
            for (entry in blockedAvatarHashes) {
                BlockedAvatarHashes.insert {
                    it[BlockedAvatarHashes.id] = entry[BlockedAvatarHashes.id]
                    it[BlockedAvatarHashes.entry] = entry[BlockedAvatarHashes.entry]
                    it[BlockedAvatarHashes.valid] = entry[BlockedAvatarHashes.valid]
                }
            }
        }

        val blockedUserBanEntries = transaction(sqliteDatabase) {
            OldBlockedUserBanEntries.selectAll()
                .toList()
        }

        transaction(database) {
            for (entry in blockedUserBanEntries) {
                BlockedUserBanEntries.insert {
                    it[BlockedUserBanEntries.id] = entry[OldBlockedUserBanEntries.id]
                    it[BlockedUserBanEntries.user] = entry[OldBlockedUserBanEntries.user]
                    it[BlockedUserBanEntries.guild] = entry[OldBlockedUserBanEntries.guild]
                    it[BlockedUserBanEntries.bannedAt] = Instant.ofEpochMilli(entry[OldBlockedUserBanEntries.bannedAt])
                    it[BlockedUserBanEntries.blockEntry] = entry[OldBlockedUserBanEntries.blockEntry]
                }
            }
        }

        val blockedAvatarHashesBanEntries = transaction(sqliteDatabase) {
            OldBlockedAvatarHashesBanEntries.selectAll()
                .toList()
        }

        transaction(database) {
            for (entry in blockedAvatarHashesBanEntries) {
                BlockedAvatarHashesBanEntries.insert {
                    it[BlockedAvatarHashesBanEntries.id] = entry[OldBlockedAvatarHashesBanEntries.id]
                    it[BlockedAvatarHashesBanEntries.user] = entry[OldBlockedAvatarHashesBanEntries.user]
                    it[BlockedAvatarHashesBanEntries.guild] = entry[OldBlockedAvatarHashesBanEntries.guild]
                    it[BlockedAvatarHashesBanEntries.bannedAt] = Instant.ofEpochMilli(entry[OldBlockedAvatarHashesBanEntries.bannedAt])
                    it[BlockedAvatarHashesBanEntries.blockEntry] = entry[OldBlockedAvatarHashesBanEntries.blockEntry]
                }
            }
        }

        println("Done!")
    }
}