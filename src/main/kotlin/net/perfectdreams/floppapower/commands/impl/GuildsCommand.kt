package net.perfectdreams.floppapower.commands.impl

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.sharding.ShardManager
import net.perfectdreams.floppapower.commands.AbstractSlashCommand

class GuildsCommand(private val shardManager: ShardManager) : AbstractSlashCommand("guilds") {
    override fun execute(event: SlashCommandEvent) {
        val builder = StringBuilder("Guilds (${shardManager.guildCache.size()})")
        builder.append("\n")
        shardManager.guildCache.sortedByDescending { it.memberCache.size() }.forEach {
            builder.append("${it.name} (${it.idLong}) [${it.memberCache.size()} membros] <Shard ${it.jda.shardInfo.shardId}>")
            builder.append("\n")
        }

        event.deferReply()
            .addFile(builder.toString().toByteArray(Charsets.UTF_8), "guilds.txt")
            .queue()
    }
}