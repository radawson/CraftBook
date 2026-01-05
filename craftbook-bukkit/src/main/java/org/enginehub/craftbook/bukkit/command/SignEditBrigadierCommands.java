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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.enginehub.craftbook.CraftBook;
import org.enginehub.craftbook.CraftBookPlayer;
import org.enginehub.craftbook.exception.CraftBookException;
import org.enginehub.craftbook.mechanic.MechanicTypes;
import org.enginehub.craftbook.mechanics.signcopier.SignCopier;
import org.enginehub.craftbook.util.AdventureText3Adapter;

import java.util.Optional;

import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.getActor;
import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.getPlayer;
import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.greedyStringArg;
import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.integerArg;
import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.literal;

/**
 * Brigadier-based implementation of SignEdit commands.
 */
public final class SignEditBrigadierCommands {

    private SignEditBrigadierCommands() {
    }

    /**
     * Get the SignCopier mechanic instance, if enabled.
     */
    private static Optional<SignCopier> getSignCopier() {
        if (MechanicTypes.SIGN_COPIER.get() == null) {
            return Optional.empty();
        }
        return CraftBook.getInstance().getPlatform().getMechanicManager()
            .getMechanic(MechanicTypes.SIGN_COPIER.get())
            .map(m -> (SignCopier) m);
    }

    /**
     * Register all SignEdit commands with the Paper command manager.
     * Commands check at runtime if SignCopier mechanic is enabled.
     */
    public static void register(PaperCommandManager commandManager) {
        RequiredArgumentBuilder<CommandSourceStack, Integer> lineArg = integerArg("line");
        RequiredArgumentBuilder<CommandSourceStack, String> textArg = greedyStringArg("text");

        LiteralArgumentBuilder<CommandSourceStack> signedit = literal("signedit")
            .then(literal("edit")
                .requires(source -> source.getSender().hasPermission("craftbook.signcopier.edit"))
                .then(lineArg
                    .then(textArg
                        .executes(context -> {
                            executeEdit(context);
                            return Command.SINGLE_SUCCESS;
                        }))))
            .then(literal("clear")
                .requires(source -> source.getSender().hasPermission("craftbook.signcopier.clear"))
                .executes(context -> {
                    executeClear(context);
                    return Command.SINGLE_SUCCESS;
                }));

        // Register aliases
        LiteralArgumentBuilder<CommandSourceStack> edsign = literal("edsign")
            .redirect(signedit.build());
        LiteralArgumentBuilder<CommandSourceStack> signcopy = literal("signcopy")
            .redirect(signedit.build());

        commandManager.register(signedit);
        commandManager.register(edsign);
        commandManager.register(signcopy);
    }

    private static void executeEdit(CommandContext<CommandSourceStack> context) {
        CraftBookPlayer player = getPlayer(context);
        if (player == null) {
            getActor(context).printError(TranslatableComponent.of("craftbook.signcopier.player-only"));
            return;
        }

        Optional<SignCopier> optSignCopier = getSignCopier();
        if (optSignCopier.isEmpty()) {
            player.printError(TranslatableComponent.of("craftbook.mechanic.not-enabled"));
            return;
        }
        SignCopier signCopier = optSignCopier.get();

        try {
            int line = IntegerArgumentType.getInteger(context, "line");
            String text = context.getArgument("text", String.class);

            if (!signCopier.hasSign(player.getUniqueId())) {
                throw new CraftBookException(TranslatableComponent.of("craftbook.signcopier.no-copy"));
            }

            if (line < 1 || line > 4) {
                throw new CraftBookException(TranslatableComponent.of("craftbook.signcopier.invalid-line"));
            }

            Component message = MiniMessage.miniMessage().deserialize(text);
            signCopier.setSignLine(player.getUniqueId(), line - 1, message);
            player.printInfo(TranslatableComponent.of(
                "craftbook.signcopier.edited",
                TextComponent.of(line),
                AdventureText3Adapter.fromAdventure(message)
            ));
        } catch (CraftBookException e) {
            player.printError(e.getRichMessage());
        }
    }

    private static void executeClear(CommandContext<CommandSourceStack> context) {
        CraftBookPlayer player = getPlayer(context);
        if (player == null) {
            getActor(context).printError(TranslatableComponent.of("craftbook.signcopier.player-only"));
            return;
        }

        Optional<SignCopier> optSignCopier = getSignCopier();
        if (optSignCopier.isEmpty()) {
            player.printError(TranslatableComponent.of("craftbook.mechanic.not-enabled"));
            return;
        }
        SignCopier signCopier = optSignCopier.get();

        try {
            if (!signCopier.hasSign(player.getUniqueId())) {
                throw new CraftBookException(TranslatableComponent.of("craftbook.signcopier.no-copy"));
            }

            signCopier.clearSign(player.getUniqueId());
            player.printInfo(TranslatableComponent.of("craftbook.signcopier.cleared"));
        } catch (CraftBookException e) {
            player.printError(e.getRichMessage());
        }
    }
}

