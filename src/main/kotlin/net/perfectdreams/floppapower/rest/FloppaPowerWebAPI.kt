package net.perfectdreams.floppapower.rest

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import net.dv8tion.jda.api.sharding.ShardManager
import net.perfectdreams.floppapower.FloppaPower
import net.perfectdreams.floppapower.data.BlockedAvatarHashResponse
import net.perfectdreams.floppapower.data.BlockedUserResponse
import net.perfectdreams.floppapower.data.Guild
import net.perfectdreams.floppapower.data.Member
import net.perfectdreams.floppapower.data.User
import net.perfectdreams.floppapower.tables.BlockedAvatarHashes
import net.perfectdreams.floppapower.tables.BlockedUsers
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class FloppaPowerWebAPI(val floppaPower: FloppaPower, val shardManager: ShardManager) {
    @OptIn(ExperimentalSerializationApi::class)
    fun start() {
        embeddedServer(Netty, port = 8000) {
            routing {
                get("/api/v1/blocked-users") {
                    val blockedUsers = withContext(Dispatchers.IO) {
                        transaction {
                            BlockedUsers.select {
                                BlockedUsers.valid eq true
                            }.map {
                                it[BlockedUsers.userId].value
                            }
                        }
                    }

                    call.respondOutputStream(ContentType.Application.Json) {
                        Json.encodeToStream(
                            blockedUsers,
                            this
                        )
                    }
                }

                get("/api/v1/blocked-users/{userId}") {
                    val userId = call.parameters.getOrFail("userId").toLong()

                    val blockedUser = withContext(Dispatchers.IO) {
                        transaction {
                            BlockedUsers.select {
                                BlockedUsers.valid eq true and (BlockedUsers.userId eq userId)
                            }.firstOrNull()
                        }
                    }

                    if (blockedUser == null) {
                        call.respondText("{}", ContentType.Application.Json, HttpStatusCode.NotFound)
                    } else {
                        call.respondText(
                            Json.encodeToString(
                                BlockedUserResponse(blockedUser[BlockedUsers.userId].value, blockedUser[BlockedUsers.entry].value, blockedUser[BlockedUsers.valid])
                            ),
                            ContentType.Application.Json,
                            HttpStatusCode.OK
                        )
                    }
                }

                get("/api/v1/blocked-avatar-hashes") {
                    val blockedAvatarHashes = withContext(Dispatchers.IO) {
                        transaction {
                            BlockedAvatarHashes.select {
                                BlockedAvatarHashes.valid eq true
                            }.map {
                                it[BlockedAvatarHashes.avatarHash].value
                            }
                        }
                    }

                    call.respondOutputStream(ContentType.Application.Json) {
                        Json.encodeToStream(
                            blockedAvatarHashes,
                            this
                        )
                    }
                }

                get("/api/v1/blocked-avatar-hashes/{avatarHash}") {
                    val avatarHash = call.parameters.getOrFail("avatarHash")

                    val blockedAvatarHash = withContext(Dispatchers.IO) {
                        transaction {
                            BlockedAvatarHashes.select {
                                BlockedAvatarHashes.valid eq true and (BlockedAvatarHashes.avatarHash eq avatarHash)
                            }.firstOrNull()
                        }
                    }

                    if (blockedAvatarHash == null) {
                        call.respondText("{}", ContentType.Application.Json, HttpStatusCode.NotFound)
                    } else {
                        call.respondText(
                            Json.encodeToString(
                                BlockedAvatarHashResponse(blockedAvatarHash[BlockedAvatarHashes.avatarHash].value, blockedAvatarHash[BlockedAvatarHashes.entry].value, blockedAvatarHash[BlockedAvatarHashes.valid])
                            ),
                            ContentType.Application.Json,
                            HttpStatusCode.OK
                        )
                    }
                }

                get("/api/v1/users/{userId}/guilds") {
                    val userId = call.parameters.getOrFail("userId").toLong()

                    val user = shardManager.getUserById(userId)
                    if (user == null) {
                        call.respondText("[]", ContentType.Application.Json, HttpStatusCode.NotFound)
                    } else {
                        val mutualGuilds = shardManager.getMutualGuilds(user)
                        call.respondText(
                            Json.encodeToString(
                                mutualGuilds.map {
                                    val member = it.getMemberById(user.idLong)!!

                                    Guild(
                                        it.idLong,
                                        it.name,
                                        Member(
                                            member.nickname,
                                            member.timeJoined.toInstant().toKotlinInstant(),
                                            User(
                                                member.user.idLong,
                                                member.user.name,
                                                member.user.discriminator,
                                                member.user.avatarId
                                            )
                                        )
                                    )
                                }
                            ),
                            ContentType.Application.Json,
                            HttpStatusCode.OK
                        )
                    }
                }

                get("/api/v1/avatar-hashes/{avatarHash}/users") {
                    val avatarHash = call.parameters.getOrFail("avatarHash")

                    val matchedUsers = shardManager.userCache.filter { it.avatarId == avatarHash }
                    if (matchedUsers.isEmpty()) {
                        call.respondText("[]", ContentType.Application.Json, HttpStatusCode.NotFound)
                    } else {
                        call.respondText(
                            Json.encodeToString(
                                matchedUsers.map {
                                    User(
                                        it.idLong,
                                        it.name,
                                        it.discriminator,
                                        it.avatarId
                                    )
                                }
                            ),
                            ContentType.Application.Json,
                            HttpStatusCode.OK
                        )
                    }
                }
            }
        }.start(wait = true)
    }
}