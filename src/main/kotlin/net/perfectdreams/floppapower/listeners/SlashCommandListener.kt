package net.perfectdreams.floppapower.listeners

import kotlinx.coroutines.sync.Mutex
import mu.KotlinLogging
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.sharding.ShardManager
import net.perfectdreams.floppapower.FloppaPower
import net.perfectdreams.floppapower.commands.impl.*
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
        LeaveGuildCommand(m, shardManager),
        CheckGuildCommand(m, shardManager),
        CheckGuildsCommand(m, shardManager),
        SameAvatarHashCommand(m, shardManager),
        SharedGuildsCommand(m, shardManager),
        CheckSimilarAvatarsCommand(m, shardManager),
        SameAvatarHashCommand(m, shardManager),
        SameAvatarUserCommand(m, shardManager),
        SusJoinsCommand(m, shardManager),
        SearchUsersCommand(shardManager),
        TopMutualUsersCommand(shardManager),
        PingCommand(m, shardManager)
    )

    override fun onGuildReady(event: GuildReadyEvent) {
        if (event.guild.idLong != Constants.ELITE_PENGUIN_FORCE_GUILD_ID)
            return
        
        val guild = event.guild

        guild.updateCommands()
            .addCommands(
                Commands.slash("guilds", "Mostra quais servidores o Floppa está"),
                Commands.slash("leaveguild", "Faça o Floppa sair de um servidor em que ele está")
                    .addOption(OptionType.STRING, "guild_id", "ID do Servidor", true),
                Commands.slash("checkguild", "Verifique meliantes em um servidor")
                    .addOption(OptionType.STRING, "guild_id", "ID do Servidor", true),
                Commands.slash("checkguilds", "Verifique meliantes em todos os servidores que eu estou"),
                Commands.slash("checksimilaravatars", "Verifique meliantes com avatares similares")
                    .addOption(OptionType.INTEGER, "page", "Página", false),
                Commands.slash("sameavatar", "Verifique meliantes que possuem o mesmo avatar")
                    .addSubcommands(
                        SubcommandData("hash", "Verifique meliantes que possuem o mesmo avatar pelo hash")
                            .addOption(OptionType.STRING, "hash", "O hash do avatar", true),
                        SubcommandData("user", "Verifique meliantes que possuem o mesmo avatar pelo usuário")
                            .addOption(OptionType.USER, "user", "O meliante", true)
                    ),
                Commands.slash("sharedguilds", "Verifique aonde os meliantes estão na fuga")
                    .addOption(OptionType.USER, "user", "O meliante", true),
                Commands.slash("searchusers", "Busca usuários usando um RegEx")
                    .addOption(OptionType.STRING, "pattern", "RegEx pattern do nome de usuário que você deseja procurar", true)
                    .addOption(OptionType.BOOLEAN, "list", "Gera uma lista sem os detalhes", false)
                    .addOptions(
                        OptionData(OptionType.STRING, "sort_by", "O que você quer ordenar os usuários por", false)
                            .addChoice("Nome", "alphabetically")
                            .addChoice("Criação", "creation_date")
                            .addChoice("Mutual Guilds", "mutual_guilds")

                    )
                    .addOptions(
                        OptionData(OptionType.STRING, "creation_time_filter", "Filtrar usuários que criaram a conta recentemente, acelera a pesquisa de usuários", false)
                            .addChoice("3 dias", "3")
                            .addChoice("7 dias", "7")
                            .addChoice("14 dias", "14")
                            .addChoice("30 dias", "30")
                            .addChoice("90 dias", "90")
                            .addChoice("180 dias", "180")
                            .addChoice("365 dias", "365")
                            .addChoice("Gosto de ver pegando fogo!", "36500")
                    ),
                Commands.slash("topmutualusers", "Mostra os top usuários que compartilham mais servidores com o Floppa"),
                Commands.slash("susjoins", "Verifique meliantes que entraram em vários servidores em seguida")
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
                    ),
                Commands.slash("ping", "Verifica se o Floppa está online")
            )
            .queue()
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        m.executor.execute {
            logger.info { "Received Slash Command ${event.name} from ${event.user}" }

            val command = commands.firstOrNull { it.commandPath == event.fullCommandName }

            if (command != null) {
                command.execute(event)
                return@execute
            }
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        try {
            val uniqueId = UUID.fromString(event.componentId)
            m.slashCommandButtonInteractionCache[uniqueId]?.invoke(
                FloppaButtonClickEvent(m, uniqueId, event)
            )
        } catch (e: IllegalArgumentException) {}
    }
}