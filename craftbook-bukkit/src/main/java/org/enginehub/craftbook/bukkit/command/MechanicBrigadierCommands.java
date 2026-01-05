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
import com.sk89q.worldedit.util.formatting.component.InvalidComponentException;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.enginehub.craftbook.CraftBook;
import org.enginehub.craftbook.mechanic.CraftBookMechanic;
import org.enginehub.craftbook.mechanic.MechanicListBox;
import org.enginehub.craftbook.mechanic.MechanicManager;
import org.enginehub.craftbook.mechanic.MechanicType;
import org.enginehub.craftbook.mechanic.exception.MechanicInitializationException;

import java.util.Optional;

import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.getActor;
import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.integerArg;
import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.literal;
import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.stringArg;

/**
 * Brigadier-based implementation of Mechanic commands.
 */
public final class MechanicBrigadierCommands {

    private MechanicBrigadierCommands() {
    }

    /**
     * Register all Mechanic commands with the Paper command manager.
     */
    public static void register(PaperCommandManager commandManager) {
        // TODO: Convert MechanicType argument to Brigadier custom argument type
        // For now, using string argument as placeholder
        RequiredArgumentBuilder<CommandSourceStack, String> mechanicArg = stringArg("mechanic");

        LiteralArgumentBuilder<CommandSourceStack> mechanic = literal("mechanic")
            .then(literal("enable")
                .requires(source -> source.getSender().hasPermission("craftbook.mechanic.enable"))
                .then(mechanicArg
                    .executes(context -> {
                        executeEnable(context, getMechanicType(context, "mechanic"), 0);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(literal("-l")
                        .then(integerArg("page")
                            .executes(context -> {
                                int page = IntegerArgumentType.getInteger(context, "page");
                                executeEnable(context, getMechanicType(context, "mechanic"), page);
                                return Command.SINGLE_SUCCESS;
                            })))))
            .then(literal("disable")
                .requires(source -> source.getSender().hasPermission("craftbook.mechanic.disable"))
                .then(mechanicArg
                    .executes(context -> {
                        executeDisable(context, getMechanicType(context, "mechanic"), 0);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(literal("-l")
                        .then(integerArg("page")
                            .executes(context -> {
                                int page = IntegerArgumentType.getInteger(context, "page");
                                executeDisable(context, getMechanicType(context, "mechanic"), page);
                                return Command.SINGLE_SUCCESS;
                            })))))
            .then(literal("reload")
                .requires(source -> source.getSender().hasPermission("craftbook.mechanic.reload"))
                .then(mechanicArg
                    .executes(context -> {
                        executeReload(context, getMechanicType(context, "mechanic"));
                        return Command.SINGLE_SUCCESS;
                    })))
            .then(literal("list")
                .requires(source -> source.getSender().hasPermission("craftbook.mechanic.list"))
                .executes(context -> {
                    executeList(context, 1);
                    return Command.SINGLE_SUCCESS;
                })
                .then(integerArg("page")
                    .executes(context -> {
                        int page = IntegerArgumentType.getInteger(context, "page");
                        executeList(context, page);
                        return Command.SINGLE_SUCCESS;
                    })));

        // Register aliases
        LiteralArgumentBuilder<CommandSourceStack> mech = literal("mech")
            .redirect(mechanic.build());
        LiteralArgumentBuilder<CommandSourceStack> mechs = literal("mechs")
            .redirect(mechanic.build());
        LiteralArgumentBuilder<CommandSourceStack> mechanics = literal("mechanics")
            .redirect(mechanic.build());

        commandManager.register(mechanic);
        commandManager.register(mech);
        commandManager.register(mechs);
        commandManager.register(mechanics);
    }

    private static MechanicType<?> getMechanicType(CommandContext<CommandSourceStack> context, String argName) {
        // TODO: Implement proper MechanicType argument type
        String mechanicName = context.getArgument(argName, String.class);
        return MechanicType.REGISTRY.get(mechanicName);
    }

    private static void executeEnable(CommandContext<CommandSourceStack> context, MechanicType<?> mechanicType, int listBoxPage) {
        com.sk89q.worldedit.extension.platform.Actor actor = getActor(context);
        try {
            if (mechanicType == null) {
                actor.printError(TranslatableComponent.of("craftbook.mechanisms.unknown"));
                return;
            }

            CraftBook.getInstance().getPlatform().getMechanicManager().enableMechanic(mechanicType);
            CraftBook.getInstance().getPlatform().getConfiguration().enabledMechanics.add(mechanicType.id());
            CraftBook.getInstance().getPlatform().getConfiguration().save();

            CraftBook.getInstance().getPlatform().refreshPlayerCommandMaps();

            actor.printInfo(TranslatableComponent.of(
                "craftbook.mechanisms.enable-success",
                TextComponent.of(mechanicType.getName(), TextColor.WHITE)
            ));
        } catch (MechanicInitializationException e) {
            actor.printError(e.getRichMessage());
        }

        if (listBoxPage > 0) {
            try {
                showListBox(actor, listBoxPage);
            } catch (InvalidComponentException e) {
                actor.printError(TextComponent.of(e.getMessage()));
            }
        }
    }

    private static void executeDisable(CommandContext<CommandSourceStack> context, MechanicType<?> mechanicType, int listBoxPage) {
        com.sk89q.worldedit.extension.platform.Actor actor = getActor(context);
        if (mechanicType == null) {
            actor.printError(TranslatableComponent.of("craftbook.mechanisms.unknown"));
            return;
        }

        Optional<?> mech = CraftBook.getInstance().getPlatform().getMechanicManager().getMechanic(mechanicType);
        if (mech.isPresent() && CraftBook.getInstance().getPlatform().getMechanicManager().disableMechanic((CraftBookMechanic) mech.get())) {
            CraftBook.getInstance().getPlatform().getConfiguration().enabledMechanics.remove(mechanicType.id());
            CraftBook.getInstance().getPlatform().getConfiguration().save();

            CraftBook.getInstance().getPlatform().refreshPlayerCommandMaps();

            actor.printInfo(TranslatableComponent.of(
                "craftbook.mechanisms.disable-success",
                TextComponent.of(mechanicType.getName(), TextColor.WHITE)
            ));
        } else {
            actor.printError(TranslatableComponent.of(
                "craftbook.mechanisms.not-enabled",
                TextComponent.of(mechanicType.getName(), TextColor.WHITE)
            ));
        }

        if (listBoxPage > 0) {
            try {
                showListBox(actor, listBoxPage);
            } catch (InvalidComponentException e) {
                actor.printError(TextComponent.of(e.getMessage()));
            }
        }
    }

    private static void executeReload(CommandContext<CommandSourceStack> context, MechanicType<?> mechanicType) {
        com.sk89q.worldedit.extension.platform.Actor actor = getActor(context);
        if (mechanicType == null) {
            actor.printError(TranslatableComponent.of("craftbook.mechanisms.unknown"));
            return;
        }

        try {
            MechanicManager mechanicManager = CraftBook.getInstance().getPlatform().getMechanicManager();
            Optional<? extends CraftBookMechanic> mechanic = mechanicManager.getMechanic(mechanicType);
            if (mechanic.isEmpty()) {
                actor.printError(TranslatableComponent.of(
                    "craftbook.mechanisms.not-enabled",
                    TextComponent.of(mechanicType.getName(), TextColor.WHITE)
                ));
                return;
            }

            mechanicManager.reloadMechanic(mechanic.get());

            actor.printInfo(TranslatableComponent.of(
                "craftbook.mechanisms.reload-success",
                TextComponent.of(mechanicType.getName(), TextColor.WHITE)
            ));
        } catch (MechanicInitializationException e) {
            actor.printError(TranslatableComponent.of(
                "craftbook.mechanisms.reload-failed",
                TextComponent.of(mechanicType.getName(), TextColor.WHITE),
                e.getRichMessage()
            ));
        }
    }

    private static void executeList(CommandContext<CommandSourceStack> context, int page) {
        com.sk89q.worldedit.extension.platform.Actor actor = getActor(context);
        try {
            showListBox(actor, page);
        } catch (InvalidComponentException e) {
            actor.printError(TextComponent.of(e.getMessage()));
        }
    }

    private static void showListBox(com.sk89q.worldedit.extension.platform.Actor actor, int page) throws InvalidComponentException {
        MechanicListBox mechanicListBox = new MechanicListBox(actor);
        actor.print(mechanicListBox.create(page));
    }
}

