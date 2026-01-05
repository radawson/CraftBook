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

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extension.platform.Actor;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.enginehub.craftbook.CraftBookPlayer;
import org.enginehub.craftbook.bukkit.BukkitCraftBookCommandSender;
import org.enginehub.craftbook.bukkit.BukkitCraftBookPlayer;
import org.enginehub.craftbook.bukkit.CraftBookPlugin;

/**
 * Adapter between Paper's CommandSourceStack and CraftBook's Actor system.
 */
public class CraftBookCommandSourceStack {

    private final CommandSourceStack source;
    private Actor actor;
    private CraftBookPlayer craftBookPlayer;

    public CraftBookCommandSourceStack(CommandSourceStack source) {
        this.source = source;
        this.actor = createActor();
    }

    private Actor createActor() {
        CommandSender sender = source.getSender();
        WorldEditPlugin worldEdit = CraftBookPlugin.plugins.getWorldEdit();

        if (sender instanceof Player player) {
            this.craftBookPlayer = new BukkitCraftBookPlayer(CraftBookPlugin.inst(), player);
            return this.craftBookPlayer;
        }
        // Use existing BukkitCraftBookCommandSender for console/other sources
        return new BukkitCraftBookCommandSender(worldEdit, sender);
    }

    public CommandSourceStack getSource() {
        return source;
    }

    public Actor getActor() {
        return actor;
    }

    public CraftBookPlayer getCraftBookPlayer() {
        return craftBookPlayer;
    }

    public boolean hasPermission(String permission) {
        CommandSender sender = source.getSender();
        return sender.hasPermission(permission);
    }
}

