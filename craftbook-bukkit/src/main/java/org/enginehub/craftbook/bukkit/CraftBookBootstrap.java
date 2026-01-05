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

package org.enginehub.craftbook.bukkit;

import com.mojang.brigadier.CommandDispatcher;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.enginehub.craftbook.bukkit.command.PaperCommandManager;

/**
 * Bootstrap class for CraftBook to register commands via Paper's lifecycle system.
 */
public class CraftBookBootstrap implements PluginBootstrap {

    private static PaperCommandManager commandManager;

    @Override
    public void bootstrap(BootstrapContext context) {
        commandManager = new PaperCommandManager();

        // Register command registration handler
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            // Get the command dispatcher from the commands registrar
            CommandDispatcher<CommandSourceStack> dispatcher = commands.registrar().getDispatcher();
            if (commandManager != null) {
                // Register CraftBook commands
                org.enginehub.craftbook.bukkit.command.CraftBookBrigadierCommands.register(commandManager);
                org.enginehub.craftbook.bukkit.command.MechanicBrigadierCommands.register(commandManager);
                org.enginehub.craftbook.bukkit.command.VariableBrigadierCommands.register(commandManager);
                org.enginehub.craftbook.bukkit.command.AreaBrigadierCommands.register(commandManager);
                org.enginehub.craftbook.bukkit.command.SignEditBrigadierCommands.register(commandManager);
                commandManager.registerAll(dispatcher);
            }
        });
    }

    public static PaperCommandManager getCommandManager() {
        return commandManager;
    }
}

