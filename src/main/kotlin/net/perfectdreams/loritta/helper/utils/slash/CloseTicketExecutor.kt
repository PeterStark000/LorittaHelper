package net.perfectdreams.loritta.helper.utils.slash

import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.optional.optional
import dev.kord.common.entity.optional.value
import dev.kord.rest.json.request.ChannelModifyPatchRequest
import net.perfectdreams.discordinteraktions.common.commands.slash.SlashCommandExecutor
import net.perfectdreams.discordinteraktions.common.context.commands.ApplicationCommandContext
import net.perfectdreams.discordinteraktions.common.context.commands.slash.SlashCommandArguments
import net.perfectdreams.discordinteraktions.declarations.commands.slash.SlashCommandExecutorDeclaration
import net.perfectdreams.loritta.helper.LorittaHelperKord
import net.perfectdreams.loritta.helper.i18n.I18nKeysData
import net.perfectdreams.loritta.helper.utils.tickets.isEnglishHelpDeskChannel
import net.perfectdreams.loritta.helper.utils.tickets.isPortugueseHelpDeskChannel

class CloseTicketExecutor(val helper: LorittaHelperKord) : SlashCommandExecutor() {
    companion object : SlashCommandExecutorDeclaration(CloseTicketExecutor::class)

    override suspend fun execute(context: ApplicationCommandContext, args: SlashCommandArguments) {
        val channelId = context.channelId
        val channel = helper.channelsCache.getChannel(channelId)
        if (channel.type != ChannelType.PrivateThread) {
            context.sendEphemeralMessage {
                content = "You aren't in a ticket!"
            }
            return
        }

        val parentChannelId = channel.parentId.value ?: return

        val i18nContext = when {
            isPortugueseHelpDeskChannel(parentChannelId) -> {
                helper.languageManager.getI18nContextById("pt")
            }
            isEnglishHelpDeskChannel(parentChannelId) -> {
                helper.languageManager.getI18nContextById("en")
            }
            else -> {
                context.sendEphemeralMessage {
                    content = "You aren't in a ticket!"
                }
                return
            }
        }

        context.sendEphemeralMessage {
            content = i18nContext.get(I18nKeysData.Tickets.ClosingYourTicket)
        }

        context.sendMessage {
            content = i18nContext.get(I18nKeysData.Tickets.TicketClosed("<@${context.sender.id.value}>"))
        }

        helper.helperRest.channel.patchThread(
            context.channelId,
            ChannelModifyPatchRequest(
                archived = true.optional()
            ),
            "Archival request via command by ${context.sender.name}#${context.sender.discriminator} (${context.sender.id.value})"
        )
    }
}