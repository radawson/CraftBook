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

package org.enginehub.craftbook.mechanics.ic.gates.world.entity;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.enginehub.craftbook.bukkit.BukkitChangedSign;
import org.enginehub.craftbook.mechanics.ic.AbstractICFactory;
import org.enginehub.craftbook.mechanics.ic.AbstractSelfTriggeredIC;
import org.enginehub.craftbook.mechanics.ic.ChipState;
import org.enginehub.craftbook.mechanics.ic.IC;
import org.enginehub.craftbook.mechanics.ic.ICFactory;
import org.enginehub.craftbook.mechanics.ic.ICVerificationException;
import org.enginehub.craftbook.CraftBook;
import org.enginehub.craftbook.bukkit.CraftBookPlugin;
import org.enginehub.craftbook.mechanics.ic.ICMechanic;
import org.enginehub.craftbook.util.HistoryHashMap;
import org.enginehub.craftbook.util.PlayerType;
import org.enginehub.craftbook.util.RegexUtil;
import org.enginehub.craftbook.util.SearchArea;
import org.enginehub.craftbook.util.Tuple2;
import org.enginehub.craftbook.util.persistence.YamlStorage;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

public class TeleportTransmitter extends AbstractSelfTriggeredIC {

    public TeleportTransmitter(Server server, BukkitChangedSign sign, ICFactory factory) {

        super(server, sign, factory);
    }

    protected static final HistoryHashMap<String, Tuple2<Long, String>> memory = new HistoryHashMap<>(50);
    protected static HistoryHashMap<String, Location> lastKnownLocations = new HistoryHashMap<>(50);

    protected String band;

    @Override
    public String getTitle() {

        return "Teleport Transmitter";
    }

    @Override
    public String getSignTitle() {

        return "TELEPORT OUT";
    }

    SearchArea area;
    PlayerType type;
    String typeData;

    @Override
    public void load() {

        band = RegexUtil.PIPE_PATTERN.split(getLine(2))[0];
        if (getLine(2).contains("|")) {
            type = PlayerType.getFromChar(RegexUtil.PIPE_PATTERN.split(getLine(2))[1].charAt(0));
            typeData = RegexUtil.COLON_PATTERN.split(RegexUtil.PIPE_PATTERN.split(getLine(2))[1])[1];
        }
        area = SearchArea.createArea(getSign().getBlock(), getLine(3));
    }

    @Override
    public void trigger(ChipState chip) {

        if (chip.getInput(0))
            chip.setOutput(0, sendPlayer());
    }

    @Override
    public void think(ChipState chip) {

        if (chip.getInput(0))
            chip.setOutput(0, sendPlayer());
    }

    public boolean sendPlayer() {

        Player closest = null;

        for (Player e : area.getPlayersInArea()) {
            if (e == null || !e.isValid() || e.isDead())
                continue;

            if (type != null && !type.doesPlayerPass(e, typeData))
                continue;

            if (closest == null) closest = e;
            if (area.getCenter() == null) break;
            else if (closest.getWorld() == area.getWorld() && closest.getLocation().distanceSquared(area.getCenter()) >= e.getLocation().distanceSquared(area.getCenter()))
                closest = e;
        }
        if (closest != null && lastKnownLocations.containsKey(band))
            lastKnownLocations.get(band).getChunk().load();
        if (closest != null && !setValue(band, new Tuple2<>(System.currentTimeMillis(), closest.getName())))
            closest.sendMessage(ChatColor.RED + "This Teleporter Frequency is currently busy! Try again soon (3s)!");
        else
            return true;
        return false;
    }

    public static Tuple2<Long, String> getValue(String band) {

        if (memory.containsKey(band)) {
            long time = System.currentTimeMillis() - memory.get(band).left();
            int seconds = (int) (time / 1000) % 60;
            if (seconds > 5) { // Expired.
                memory.remove(band);
                return null;
            }
        }
        Tuple2<Long, String> val = memory.get(band);
        memory.remove(band); // Remove on teleport.
        return val;
    }

    public static boolean setValue(String band, Tuple2<Long, String> val) {

        if (memory.containsKey(band)) {
            long time = System.currentTimeMillis() - memory.get(band).left();
            int seconds = (int) (time / 1000) % 60;
            if (seconds > 3) { // Expired.
                memory.remove(band);
            } else return false;
        }
        memory.put(band, val);
        return true;
    }

    public static class Factory extends AbstractICFactory {

        private static File storageFile;
        private static YamlConfiguration storageConfig;

        public Factory(Server server) {

            super(server);
            storageFile = new File(CraftBookPlugin.inst().getDataFolder(), "teleport-locations.yml");
        }

        @Override
        public IC create(BukkitChangedSign sign) {

            return new TeleportTransmitter(getServer(), sign, this);
        }

        @Override
        public String[] getLongDescription() {

            return new String[] {
                "The '''MC1112''' teleports a player located within IC's radius to a receiver ([[../MC1113/]]) tuned to the same ''frequency''.",
                "This IC requires the recieving chunk to be loaded for the initial teleport, future teleports should not require the chunk to be loaded."
            };
        }

        @Override
        public String getShortDescription() {

            return "Transmitter for the teleportation network.";
        }

        @Override
        public String[] getPinDescription(ChipState state) {

            return new String[] {
                "Trigger IC",//Inputs
                "High on successful teleport queue",//Outputs
            };
        }

        @Override
        public void verify(BukkitChangedSign sign) throws ICVerificationException {

            if (!SearchArea.isValidArea(sign.getBlock(), PlainTextComponentSerializer.plainText().serialize(sign.getLine(3))))
                throw new ICVerificationException("Invalid SearchArea on 4th line!");
        }

        @Override
        public void load() {
            super.load();
            loadPersistentData();
        }

        private void loadPersistentData() {
            // Try to migrate from old binary format
            File oldFile = new File(CraftBookPlugin.inst().getDataFolder(), "teleport-locations.dat");
            if (oldFile.exists()) {
                try (DataInputStream stream = new DataInputStream(new FileInputStream(oldFile))) {
                    int count = stream.readInt();
                    for (int i = 0; i < count; i++) {
                        String band = stream.readUTF();
                        String worldName = stream.readUTF();
                        double x = stream.readDouble();
                        double y = stream.readDouble();
                        double z = stream.readDouble();
                        float yaw = stream.readFloat();
                        float pitch = stream.readFloat();
                        
                        World world = Bukkit.getWorld(worldName);
                        if (world != null) {
                            Location location = new Location(world, x, y, z, yaw, pitch);
                            lastKnownLocations.put(band, location);
                        }
                    }
                    CraftBook.LOGGER.info("Migrated teleport locations from binary format to YAML");
                    // Delete old file after successful migration
                    oldFile.delete();
                } catch (IOException e) {
                    CraftBook.LOGGER.warn("Failed to migrate teleport locations from old format, starting fresh", e);
                }
            }

            // Load from YAML
            storageConfig = YamlStorage.loadConfiguration(storageFile);
            Map<String, Location> loadedLocations = YamlStorage.getLocationMap(storageConfig, "teleport-locations");
            if (!loadedLocations.isEmpty()) {
                lastKnownLocations.clear();
                lastKnownLocations.putAll(loadedLocations);
            }
        }

        @Override
        public void unload() {
            if (ICMechanic.instance != null && ICMechanic.instance.savePersistentData) {
                if (storageConfig == null) {
                    storageConfig = YamlStorage.loadConfiguration(storageFile);
                }
                YamlStorage.setLocationMap(storageConfig, "teleport-locations", lastKnownLocations);
                YamlStorage.saveConfiguration(storageConfig, storageFile);
            }
        }

        @Override
        public String[] getLineHelp() {

            return new String[] { "Frequency|PlayerType", "SearchArea" };
        }
    }
}