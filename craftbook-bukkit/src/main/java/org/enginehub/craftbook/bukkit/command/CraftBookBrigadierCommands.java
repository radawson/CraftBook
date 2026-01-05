/*
 * CraftBook Copyright (C) EngineHub and Contributors <https://enginehub.org/>
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package org.enginehub.craftbook.bukkit.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.sk89q.worldedit.util.auth.AuthorizationException;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import com.sk89q.worldedit.util.paste.ActorCallbackPaste;
import com.sk89q.worldedit.util.report.ReportList;
import com.sk89q.worldedit.util.report.SystemInfoReport;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.enginehub.craftbook.CraftBook;
import org.enginehub.craftbook.util.report.GlobalConfigReport;
import org.enginehub.craftbook.util.report.ReportFlag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.getActor;
import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.literal;

/**
 * Brigadier-based implementation of CraftBook commands.
 */
public final class CraftBookBrigadierCommands {

    private CraftBookBrigadierCommands() {
    }

    /**
     * Register all CraftBook commands with the Paper command manager.
     */
    public static void register(PaperCommandManager commandManager) {
        LiteralArgumentBuilder<CommandSourceStack> craftbook = literal("craftbook")
            .then(literal("reload")
                .requires(source -> source.getSender().hasPermission("craftbook.reload"))
                .executes(context -> {
                    executeReload(context);
                    return Command.SINGLE_SUCCESS;
                }))
            .then(literal("version")
                .requires(source -> source.getSender().hasPermission("craftbook.version"))
                .executes(context -> {
                    executeVersion(context);
                    return Command.SINGLE_SUCCESS;
                }))
            .then(literal("ver")
                .requires(source -> source.getSender().hasPermission("craftbook.version"))
                .executes(context -> {
                    executeVersion(context);
                    return Command.SINGLE_SUCCESS;
                }))
            .then(literal("report")
                .requires(source -> source.getSender().hasPermission("craftbook.report"))
                .executes(context -> {
                    executeReport(context, false, false);
                    return Command.SINGLE_SUCCESS;
                })
                .then(literal("-i")
                    .executes(context -> {
                        executeReport(context, true, false);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(literal("-p")
                        .requires(source -> source.getSender().hasPermission("craftbook.report.pastebin"))
                        .executes(context -> {
                            executeReport(context, true, true);
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("-p")
                    .requires(source -> source.getSender().hasPermission("craftbook.report.pastebin"))
                    .executes(context -> {
                        executeReport(context, false, true);
                        return Command.SINGLE_SUCCESS;
                    })));

        // Register alias
        LiteralArgumentBuilder<CommandSourceStack> cb = literal("cb")
            .redirect(craftbook.build());

        commandManager.register(craftbook);
        commandManager.register(cb);
    }

    private static void executeReload(CommandContext<CommandSourceStack> context) {
        com.sk89q.worldedit.extension.platform.Actor actor = getActor(context);
        try {
            CraftBook.getInstance().getPlatform().reloadConfiguration();
        } catch (Throwable e) {
            CraftBook.LOGGER.error(e);
            actor.printError(TranslatableComponent.of("craftbook.reload.failed"));
            return;
        }
        actor.printInfo(TranslatableComponent.of("craftbook.reload.reloaded"));
    }

    private static void executeVersion(CommandContext<CommandSourceStack> context) {
        com.sk89q.worldedit.extension.platform.Actor actor = getActor(context);
        actor.printInfo(TranslatableComponent.of("craftbook.version.version",
            TextComponent.of(CraftBook.getInstance().getPlatform().getPlatformName())));
        actor.printInfo(
            TextComponent.of("https://github.com/EngineHub/CraftBook/")
                .clickEvent(ClickEvent.openUrl("https://github.com/EngineHub/CraftBook/"))
        );
    }

    private static void executeReport(CommandContext<CommandSourceStack> context, boolean loadedIcReport, boolean pastebin) {
        com.sk89q.worldedit.extension.platform.Actor actor = getActor(context);
        try {
            ReportList report = new ReportList("Report");
            EnumSet<ReportFlag> reportFlags = EnumSet.noneOf(ReportFlag.class);

            if (loadedIcReport) {
                reportFlags.add(ReportFlag.IC_REPORT);
            }

            report.add(new SystemInfoReport());
            report.add(new GlobalConfigReport());

            CraftBook.getInstance().getPlatform().addPlatformReports(report, reportFlags.toArray(new ReportFlag[0]));

            String result = report.toString();

            Path dest = CraftBook.getInstance().getPlatform().getWorkingDirectory().resolve("report.txt");
            Files.writeString(dest, result, StandardCharsets.UTF_8);
            actor.printInfo(TranslatableComponent.of("craftbook.report.written",
                TextComponent.of(dest.toAbsolutePath().toString())));

            if (pastebin) {
                if (!actor.hasPermission("craftbook.report.pastebin")) {
                    throw new AuthorizationException();
                }

                ActorCallbackPaste.pastebin(
                    CraftBook.getInstance().getSupervisor(),
                    actor,
                    result,
                    TranslatableComponent.builder("craftbook.report.success")
                );
            }
        } catch (IOException e) {
            actor.printError(TranslatableComponent.of("craftbook.report.error",
                TextComponent.of(e.getMessage())));
        } catch (AuthorizationException e) {
            actor.printError(TextComponent.of(e.getMessage()));
        }
    }
}

