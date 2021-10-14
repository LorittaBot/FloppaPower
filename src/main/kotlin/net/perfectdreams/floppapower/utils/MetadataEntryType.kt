package net.perfectdreams.floppapower.utils

enum class MetadataEntryType(val fancyName: String, val remove: Boolean, val requiredApprovals: Int) {
    SELFBOT_ID("IDs de Selfbots", false, 10),
    AVATAR_HASHES("Avatar Hashes de Selfbots", false, 15),
    DELETE_SELFBOT_ID("Remover IDs", true, 3),
    DELETE_AVATAR_HASHES("Remover Avatar Hashes", true, 3)
}