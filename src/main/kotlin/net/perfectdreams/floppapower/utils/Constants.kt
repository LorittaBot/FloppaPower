package net.perfectdreams.floppapower.utils

import net.dv8tion.jda.api.Permission
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Constants {
    const val REPORT_CHANNEL_ID = 852513688102633472L
    const val LOG_CHANNEL_ID = 852513804133597204L
    const val ELITE_PENGUIN_FORCE_GUILD_ID = 801560356795056168L
    const val FLOPPA_GANG_ROLE_ID = 852575934938349568
    val AVATAR_HASH_LENGTH = 32..34 // 34 = animated avatar
    val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")
        .withZone(ZoneId.of("America/Sao_Paulo"))
    val TRUSTED_PERMISSIONS = listOf(
        Permission.ADMINISTRATOR,
        Permission.BAN_MEMBERS,
        Permission.KICK_MEMBERS,
        Permission.MANAGE_CHANNEL,
        Permission.MANAGE_GUILD_EXPRESSIONS,
        Permission.MANAGE_PERMISSIONS,
        Permission.MANAGE_ROLES,
        Permission.MANAGE_SERVER
    )
    val TRUSTED_ROLES = listOf(
        "Staff", "Administrador", "Moderador", "Ajudante", "Helper", "Equipe", "Protegido"
    )

    val emojis = mutableListOf(
        "🙈",
        "🙉",
        "🙊",
        "🐵",
        "🐒",
        "🦍",
        "🦧",
        "🐶",
        "🐕",
        "🦮",
        "🐕‍🦺",
        "🐩",
        "🐺",
        "🦊",
        "🦝",
        "🐱",
        "🐈",
        "🦁",
        "🐯",
        "🐅",
        "🐆",
        "🐴",
        "🐎",
        "🦄",
        "🦓",
        "🦌",
        "🐮",
        "🐂",
        "🐃",
        "🐄",
        "🐷",
        "🐖",
        "🐗",
        "🐏",
        "🐑",
        "🐐",
        "🐪",
        "🐫",
        "🦙",
        "🦒",
        "🐘",
        "🦏",
        "🦛",
        "🐭",
        "🐁",
        "🐀",
        "🐹",
        "🐰",
        "🐇",
        "🐿",
        "🦔",
        "🦇",
        "🐻",
        "🐨",
        "🐼",
        "🦥",
        "🦦",
        "🦨",
        "🦘",
        "🦡",
        "🦃",
        "🐔",
        "🐓",
        "🐣",
        "🐤",
        "🐥",
        "🐦",
        "🐧",
        "🕊",
        "🦅",
        "🦆",
        "🦢",
        "🦉",
        "🦩",
        "🦚",
        "🦜",
        "🐸",
        "🐊",
        "🐢",
        "🦎",
        "🐍",
        "🐲",
        "🐉",
        "🦕",
        "🦖",
        "🐳",
        "🐋",
        "🐬",
        "🐟",
        "🐠",
        "🐡",
        "🦈",
        "🐙",
        "🐚",
        "🐌",
        "🦋",
        "🐛",
        "🐜",
        "🐝",
        "🐞",
        "🦗",
        "🕷",
        "🦂",
        "🦟",
        "🦠",
        "\uD83E\uDD80",
        "\uD83E\uDD9E",
        "\uD83E\uDD90",
        "\uD83E\uDD91"
    )
}