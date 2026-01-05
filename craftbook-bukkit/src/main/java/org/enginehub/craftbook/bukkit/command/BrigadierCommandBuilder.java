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

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.enginehub.craftbook.bukkit.command.CraftBookCommandSourceStack;

/**
 * Utility class for building Brigadier command trees.
 */
public final class BrigadierCommandBuilder {

    private BrigadierCommandBuilder() {
    }

    /**
     * Create a literal command node.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    /**
     * Create a string argument.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, String> stringArg(String name) {
        return RequiredArgumentBuilder.argument(name, StringArgumentType.word());
    }

    /**
     * Create a greedy string argument (for remaining text).
     */
    public static RequiredArgumentBuilder<CommandSourceStack, String> greedyStringArg(String name) {
        return RequiredArgumentBuilder.argument(name, StringArgumentType.greedyString());
    }

    /**
     * Create an integer argument.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, Integer> integerArg(String name) {
        return RequiredArgumentBuilder.argument(name, IntegerArgumentType.integer());
    }

    /**
     * Get the CraftBook actor from a command context.
     */
    public static com.sk89q.worldedit.extension.platform.Actor getActor(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CraftBookCommandSourceStack craftBookSource = new CraftBookCommandSourceStack(source);
        return craftBookSource.getActor();
    }

    /**
     * Get the CraftBook player from a command context, or null if not a player.
     */
    public static org.enginehub.craftbook.CraftBookPlayer getPlayer(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CraftBookCommandSourceStack craftBookSource = new CraftBookCommandSourceStack(source);
        return craftBookSource.getCraftBookPlayer();
    }

    /**
     * Require a permission for a command node.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> requiresPermission(
            LiteralArgumentBuilder<CommandSourceStack> builder, String permission) {
        return builder.requires(source -> source.getSender().hasPermission(permission));
    }
}

