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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Paper Brigadier command manager for CraftBook.
 * This replaces the Piston-based command system with Paper's native Brigadier API.
 */
public class PaperCommandManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private final List<LiteralArgumentBuilder<CommandSourceStack>> registeredCommands = new ArrayList<>();

    /**
     * Register a command builder with this manager.
     * Commands should be registered during the COMMANDS lifecycle event.
     */
    public void register(LiteralArgumentBuilder<CommandSourceStack> command) {
        registeredCommands.add(command);
    }

    /**
     * Register all commands with the given command dispatcher.
     * This should be called from the COMMANDS lifecycle event handler.
     */
    public void registerAll(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (LiteralArgumentBuilder<CommandSourceStack> command : registeredCommands) {
            dispatcher.register(command);
        }
        LOGGER.info("Registered {} commands with Paper Brigadier", registeredCommands.size());
    }

    /**
     * Build a literal command node.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    /**
     * Get all registered commands.
     */
    public List<LiteralArgumentBuilder<CommandSourceStack>> getRegisteredCommands() {
        return new ArrayList<>(registeredCommands);
    }
}

