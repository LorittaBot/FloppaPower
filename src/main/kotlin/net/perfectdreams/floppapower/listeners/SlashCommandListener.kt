package net.perfectdreams.floppapower.listeners

import kotlinx.coroutines.sync.Mutex
import mu.KotlinLogging
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.sharding.ShardManager
import net.perfectdreams.floppapower.FloppaPower
import net.perfectdreams.floppapower.commands.impl.CheckGuildCommand
import net.perfectdreams.floppapower.commands.impl.CheckGuildsCommand
import net.perfectdreams.floppapower.commands.impl.CheckSimilarAvatarsCommand
import net.perfectdreams.floppapower.commands.impl.GuildsCommand
import net.perfectdreams.floppapower.commands.impl.SameAvatarHashCommand
import net.perfectdreams.floppapower.commands.impl.SameAvatarUserCommand
import net.perfectdreams.floppapower.commands.impl.SearchUsersCommand
import net.perfectdreams.floppapower.commands.impl.SharedGuildsCommand
import net.perfectdreams.floppapower.commands.impl.SusJoinsCommand
import net.perfectdreams.floppapower.commands.impl.TopMutualUsersCommand
import net.perfectdreams.floppapower.utils.Constants
import net.perfectdreams.floppapower.utils.FloppaButtonClickEvent
import java.util.*

class SlashCommandListener(private val m: FloppaPower, private val shardManager: ShardManager) : ListenerAdapter() {
    companion object {
        private val logger = KotlinLogging.logger {}
        val verificationMutex = Mutex()
    }

    private val commands = listOf(
        GuildsCommand(shardManager),
        CheckGuildCommand(m, shardManager),
        CheckGuildsCommand(m, shardManager),
        SameAvatarHashCommand(m, shardManager),
        SharedGuildsCommand(m, shardManager),
        CheckSimilarAvatarsCommand(m, shardManager),
        SameAvatarHashCommand(m, shardManager),
        SameAvatarUserCommand(m, shardManager),
        SusJoinsCommand(m, shardManager),
        SearchUsersCommand(shardManager),
        TopMutualUsersCommand(shardManager)
    )

    override fun onGuildReady(event: GuildReadyEvent) {
        if (event.guild.idLong != Constants.ELITE_PENGUIN_FORCE_GUILD_ID)
            return
        
        val guild = event.guild

        guild.upsertCommand(
            CommandData("guilds", "Mostra quais servidores o Floppa está")
        ).queue()

        guild.upsertCommand(
            CommandData("checkguild", "Verifique meliantes em um servidor")
                .addOption(OptionType.STRING, "guild_id", "ID do Servidor", true)
        ).queue()

        guild.upsertCommand(
            CommandData("checkguilds", "Verifique meliantes em todos os servidores que eu estou")
        ).queue()

        guild.upsertCommand(
            CommandData("checksimilaravatars", "Verifique meliantes com avatares similares")
                .addOption(OptionType.INTEGER, "page", "Página", false)
        ).queue()

        guild.upsertCommand(
            CommandData("sameavatar", "Verifique meliantes que possuem o mesmo avatar")
                .addSubcommands(
                    SubcommandData("hash", "Verifique meliantes que possuem o mesmo avatar pelo hash")
                        .addOption(OptionType.STRING, "hash", "O hash do avatar", true),
                    SubcommandData("user", "Verifique meliantes que possuem o mesmo avatar pelo usuário")
                        .addOption(OptionType.USER, "user", "O meliante", true)
                )
        ).queue()

        guild.upsertCommand(
            CommandData("sharedguilds", "Verifique aonde os meliantes estão na fuga")
                .addOption(OptionType.USER, "user", "O meliante", true)
        ).queue()

        guild.upsertCommand(
            CommandData("searchusers", "Busca usuários usando um RegEx")
                .addOption(OptionType.STRING, "pattern", "RegEx pattern do nome de usuário que você deseja procurar", true)
                .addOption(OptionType.BOOLEAN, "list", "Gera uma lista sem os detalhes", false)
                .addOptions(
                    OptionData(OptionType.STRING, "sort_by", "O que você quer ordenar os usuários por", false)
                        .addChoice("Nome", "alphabetically")
                        .addChoice("Criação", "creation_date")
                    )
        ).queue()

        guild.upsertCommand(
            CommandData("topmutualusers", "Mostra os top usuários que compartilham mais servidores com o Floppa")
        ).queue()

        guild.upsertCommand(
            CommandData("susjoins", "Verifique meliantes que entraram em vários servidores em seguida")
                .addOptions(
                    OptionData(OptionType.STRING, "time", "A diferença + e - de tempo que o meliante entrou, baseado no tempo de entrada no \"meio\" dos joins", true)
                        .addChoice("10 segundos", "5")
                        .addChoice("15 segundos", "7")
                        .addChoice("30 segundos", "15")
                        .addChoice("Um minuto", "30")
                        .addChoice("Dois minutos", "60")
                        .addChoice("Cinco minutos", "150")
                        .addChoice("Dez minutos", "300")
                        .addChoice("Trinta minutos", "900")
                )
                .addOptions(
                    OptionData(OptionType.STRING, "creation_time_filter", "Filtrar usuários que criaram a conta recentemente, acelera a pesquisa de usuários", true)
                        .addChoice("3 dias", "3")
                        .addChoice("7 dias", "7")
                        .addChoice("14 dias", "14")
                        .addChoice("30 dias", "30")
                        .addChoice("90 dias", "90")
                        .addChoice("180 dias", "180")
                        .addChoice("365 dias", "365")
                        .addChoice("Gosto de ver pegando fogo!", "36500")
                )
        ).queue()
    }

    override fun onSlashCommand(event: SlashCommandEvent) {
        logger.info { "Received Slash Command ${event.name} from ${event.user}" }

        val command = commands.firstOrNull { it.commandPath == event.commandPath }

        if (command != null) {
            command.execute(event)
            return
        }
    }

    override fun onButtonClick(event: ButtonClickEvent) {
        try {
            val uniqueId = UUID.fromString(event.componentId)
            m.slashCommandButtonInteractionCache[uniqueId]?.invoke(
                FloppaButtonClickEvent(m, uniqueId, event)
            )
        } catch (e: IllegalArgumentException) {}
    }
}