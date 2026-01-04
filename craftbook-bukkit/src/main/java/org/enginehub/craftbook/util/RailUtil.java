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

package org.enginehub.craftbook.util;

import com.sk89q.worldedit.world.block.BlockCategories;
import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.BlockInventoryHolder;
import org.enginehub.craftbook.CraftBook;
import org.enginehub.craftbook.mechanic.MechanicTypes;
import org.enginehub.craftbook.mechanics.minecart.blocks.CartMechanismBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class RailUtil {

    private RailUtil() {
    }

    public static List<BlockInventoryHolder> getNearbyInventoryBlocks(CartMechanismBlocks blocks) {
        // Use streams to lazy-evaluate container checks, reducing unnecessary block state lookups
        List<Block> searchLocations = new ArrayList<>();
        if (blocks.hasBase()) {
            searchLocations.add(blocks.base());
        }
        if (blocks.hasRail()) {
            searchLocations.add(blocks.rail());
        }
        if (blocks.hasSign()) {
            searchLocations.add(blocks.sign());
        }

        return searchLocations.stream()
            .flatMap(body -> {
                int x = body.getX();
                int y = body.getY();
                int z = body.getZ();
                World world = body.getWorld();

                // Generate stream of offsets to check: center, then cardinal directions with optional second block
                return Stream.of(
                    // Center block
                    new int[] { x, y, z },
                    // Negative X direction (up to 2 blocks)
                    new int[] { x - 1, y, z },
                    new int[] { x - 2, y, z },
                    // Positive X direction (up to 2 blocks)
                    new int[] { x + 1, y, z },
                    new int[] { x + 2, y, z },
                    // Negative Z direction (up to 2 blocks)
                    new int[] { x, y, z - 1 },
                    new int[] { x, y, z - 2 },
                    // Positive Z direction (up to 2 blocks)
                    new int[] { x, y, z + 1 },
                    new int[] { x, y, z + 2 }
                )
                .map(offset -> world.getBlockAt(offset[0], offset[1], offset[2]))
                .filter(InventoryUtil::doesBlockHaveInventory)
                .map(block -> (BlockInventoryHolder) block.getState(false));
            })
            .toList();
    }

    public static boolean isTrack(BlockType blockType) {
        if (CraftBook.getInstance().getPlatform().getMechanicManager().getMechanic(MechanicTypes.MORE_RAILS.get())
            .map(moreRails -> moreRails.isValidRail(blockType))
            .orElse(false)) {
            return true;
        }

        return BlockCategories.RAILS.contains(blockType);
    }
}
