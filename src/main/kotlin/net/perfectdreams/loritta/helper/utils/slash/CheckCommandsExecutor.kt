package net.perfectdreams.loritta.helper.utils.slash

import net.perfectdreams.discordinteraktions.common.context.commands.SlashCommandArguments
import net.perfectdreams.discordinteraktions.common.context.commands.SlashCommandContext
import net.perfectdreams.discordinteraktions.declarations.slash.SlashCommandExecutorDeclaration
import net.perfectdreams.discordinteraktions.declarations.slash.options.CommandOptions
import net.perfectdreams.loritta.helper.LorittaHelper
import net.perfectdreams.loritta.helper.tables.ExecutedCommandsLog
import net.perfectdreams.loritta.helper.utils.dailycatcher.DailyCatcherManager
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class CheckCommandsExecutor(helper: LorittaHelper) : HelperSlashExecutor(helper) {
    companion object : SlashCommandExecutorDeclaration(CheckCommandsExecutor::class) {
        override val options = Options

        object Options : CommandOptions() {
            val user = user("user", "Usuário a ser verificado")
                .register()
        }
    }

    override suspend fun executeHelper(context: SlashCommandContext, args: SlashCommandArguments) {
        context.deferReply(false)
        val user = args[options.user]

        val commandCountField = ExecutedCommandsLog.command.count()

        val commands = transaction(helper.databases.lorittaDatabase) {
            ExecutedCommandsLog.slice(ExecutedCommandsLog.command, commandCountField)
                .select {
                    ExecutedCommandsLog.userId eq user.id.value
                }
                .groupBy(ExecutedCommandsLog.command)
                .orderBy(commandCountField, SortOrder.DESC)
                .limit(15)
                .toList()
        }

        var input = "**Stats de comandos de ${user.id.value}**\n"
        input += "**Quantidade de comandos executados:** ${commands.sumBy { it[commandCountField].toInt() }}\n"
        input += "**Comandos de economia executados:** ${
            commands.filter { it[ExecutedCommandsLog.command] in DailyCatcherManager.ECONOMY_COMMANDS }
                .sumBy { it[commandCountField].toInt() }
        }\n"
        input += "\n"

        for (command in commands) {
            input += "**`${command[ExecutedCommandsLog.command]}`:** ${command[commandCountField]}\n"
        }

        context.sendMessage {
            content = input
        }
    }
}