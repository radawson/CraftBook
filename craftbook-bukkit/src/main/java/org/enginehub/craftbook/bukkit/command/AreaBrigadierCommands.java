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
import com.mojang.brigadier.context.CommandContext;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.auth.AuthorizationException;
import com.sk89q.worldedit.util.formatting.component.InvalidComponentException;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.formatting.text.event.HoverEvent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import com.sk89q.worldedit.util.formatting.text.format.TextDecoration;
import com.sk89q.worldedit.world.World;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.enginehub.craftbook.CraftBook;
import org.enginehub.craftbook.CraftBookPlayer;
import org.enginehub.craftbook.mechanic.MechanicTypes;
import org.enginehub.craftbook.mechanics.area.clipboard.AreaListBox;
import org.enginehub.craftbook.mechanics.area.clipboard.CopyManager;
import org.enginehub.craftbook.mechanics.area.clipboard.ToggleArea;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.getActor;
import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.getPlayer;
import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.integerArg;
import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.literal;
import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.stringArg;

/**
 * Brigadier-based implementation of Area commands.
 */
public final class AreaBrigadierCommands {

    private static final PathMatcher SCHEMATIC_FILTER = (path) -> {
        String[] extensions = ClipboardFormats.getFileExtensionArray();
        boolean found = false;
        String filename = path.getFileName().toString();
        for (String extension : extensions) {
            if (filename.endsWith("." + extension)) {
                found = true;
                break;
            }
        }
        return found;
    };

    private AreaBrigadierCommands() {
    }

    /**
     * Get the ToggleArea mechanic instance, if enabled.
     */
    private static Optional<ToggleArea> getToggleArea() {
        if (MechanicTypes.TOGGLE_AREA.get() == null) {
            return Optional.empty();
        }
        return CraftBook.getInstance().getPlatform().getMechanicManager()
            .getMechanic(MechanicTypes.TOGGLE_AREA.get());
    }

    /**
     * Register all Area commands with the Paper command manager.
     * Commands check at runtime if ToggleArea mechanic is enabled.
     */
    public static void register(PaperCommandManager commandManager) {
        LiteralArgumentBuilder<CommandSourceStack> area = literal("area")
            // save command
            .then(literal("save")
                .requires(source -> source.getSender().hasPermission("craftbook.togglearea.save"))
                .then(stringArg("name")
                    .executes(ctx -> executeSave(ctx, null, false, false))
                    .then(literal("-n")
                        .then(stringArg("namespace")
                            .executes(ctx -> {
                                String ns = ctx.getArgument("namespace", String.class);
                                return executeSave(ctx, ns, false, false);
                            })))
                    .then(literal("-b")
                        .executes(ctx -> executeSave(ctx, null, true, false)))
                    .then(literal("-e")
                        .executes(ctx -> executeSave(ctx, null, false, true)))))
            // list command
            .then(literal("list")
                .requires(source -> source.getSender().hasPermission("craftbook.togglearea.list"))
                .executes(ctx -> executeList(ctx, null, false, 1))
                .then(integerArg("page")
                    .executes(ctx -> {
                        int page = IntegerArgumentType.getInteger(ctx, "page");
                        return executeList(ctx, null, false, page);
                    }))
                .then(literal("-a")
                    .executes(ctx -> executeList(ctx, null, true, 1))
                    .then(integerArg("page")
                        .executes(ctx -> {
                            int page = IntegerArgumentType.getInteger(ctx, "page");
                            return executeList(ctx, null, true, page);
                        })))
                .then(literal("-n")
                    .then(stringArg("namespace")
                        .executes(ctx -> {
                            String ns = ctx.getArgument("namespace", String.class);
                            return executeList(ctx, ns, false, 1);
                        })
                        .then(integerArg("page")
                            .executes(ctx -> {
                                String ns = ctx.getArgument("namespace", String.class);
                                int page = IntegerArgumentType.getInteger(ctx, "page");
                                return executeList(ctx, ns, false, page);
                            })))))
            // toggle command
            .then(literal("toggle")
                .requires(source -> source.getSender().hasPermission("craftbook.togglearea.toggle-command"))
                .then(stringArg("position")
                    .executes(ctx -> executeToggle(ctx, false))
                    .then(literal("-s")
                        .executes(ctx -> executeToggle(ctx, true)))))
            // delete command
            .then(literal("delete")
                .requires(source -> source.getSender().hasPermission("craftbook.togglearea.delete"))
                .then(stringArg("name")
                    .executes(ctx -> executeDelete(ctx, null))
                    .then(literal("-n")
                        .then(stringArg("namespace")
                            .executes(ctx -> {
                                String ns = ctx.getArgument("namespace", String.class);
                                return executeDelete(ctx, ns);
                            })))))
            // delete-all command
            .then(literal("delete-all")
                .requires(source -> source.getSender().hasPermission("craftbook.togglearea.delete"))
                .then(stringArg("namespace")
                    .executes(AreaBrigadierCommands::executeDeleteAll)));

        // Register command
        commandManager.register(area);
    }

    private static Component makeFriendlyNamespace(Actor actor, String namespace) {
        Component namespaceComponent;
        if (!actor.isPlayer() || !namespace.equalsIgnoreCase(actor.getUniqueId().toString())) {
            namespaceComponent = TextComponent.of(namespace);
        } else {
            namespaceComponent = TranslatableComponent.of("~" + actor.getName(), TextColor.LIGHT_PURPLE, TextDecoration.ITALIC)
                .hoverEvent(HoverEvent.showText(TextComponent.of(namespace)));
        }
        return namespaceComponent;
    }

    private static int executeSave(CommandContext<CommandSourceStack> context, String namespace,
                                    boolean saveBiomes, boolean saveEntities) {
        CraftBookPlayer player = getPlayer(context);
        if (player == null) {
            getActor(context).printError(TranslatableComponent.of("craftbook.error.player-required"));
            return 0;
        }

        Optional<ToggleArea> optToggleArea = getToggleArea();
        if (optToggleArea.isEmpty()) {
            player.printError(TranslatableComponent.of("craftbook.mechanic.not-enabled"));
            return 0;
        }
        ToggleArea toggleArea = optToggleArea.get();

        String name = context.getArgument("name", String.class);
        boolean personal = true;

        try {
            if (namespace != null && !namespace.equalsIgnoreCase("self")) {
                if (!player.hasPermission("craftbook.togglearea.save." + namespace)) {
                    throw new AuthorizationException(TranslatableComponent.of(
                        "craftbook.togglearea.namespace-permissions",
                        TextComponent.of(namespace, TextColor.DARK_PURPLE)
                    ));
                }
                personal = false;
            } else {
                if (!player.hasPermission("craftbook.togglearea.save.self")) {
                    throw new AuthorizationException();
                }
                namespace = player.getUniqueId().toString();
            }

            if (!personal && !CopyManager.isValidNamespace(namespace)) {
                player.printError(TranslatableComponent.of("craftbook.togglearea.invalid-namespace"));
                return 0;
            }

            if (!CopyManager.isValidName(name)) {
                player.printError(TranslatableComponent.of("craftbook.togglearea.invalid-area-name"));
                return 0;
            }

            World world = player.getWorld();
            Region sel = WorldEdit.getInstance().getSessionManager().get(player).getSelection(world);
            if (sel == null) {
                player.printError(TranslatableComponent.of("craftbook.togglearea.missing-selection"));
                return 0;
            }

            if (toggleArea.maxAreaSize != -1 && sel.getVolume() > toggleArea.maxAreaSize) {
                player.printError(TranslatableComponent.of(
                    "craftbook.togglearea.selection-too-large",
                    TextComponent.of(toggleArea.maxAreaSize)
                ));
                return 0;
            }

            if (personal && toggleArea.maxAreasPerUser >= 0
                    && !player.hasPermission("craftbook.togglearea.bypass-area-limit")) {
                int count = CopyManager.meetsQuota(namespace, name, toggleArea.maxAreasPerUser);
                if (count > -1) {
                    player.printError(TranslatableComponent.of(
                        "craftbook.togglearea.too-many-areas",
                        TextComponent.of(toggleArea.maxAreasPerUser),
                        TextComponent.of(count)
                    ));
                    return 0;
                }
            }

            Clipboard copy = CopyManager.getInstance().copy(sel, world, saveEntities, saveBiomes);

            CraftBook.LOGGER.info(player.getName() + " saving toggle area with folder '"
                + namespace + "' and ID '" + name + "'.");

            try {
                CopyManager.getInstance().save(namespace, name.toLowerCase(Locale.ENGLISH), copy);
                player.printInfo(TranslatableComponent.of(
                    "craftbook.togglearea.saved",
                    TextComponent.of(name),
                    makeFriendlyNamespace(player, namespace)
                ));
            } catch (IOException e) {
                player.printError(TranslatableComponent.of(
                    "craftbook.togglearea.save-failed",
                    TextComponent.of(e.getMessage())
                ));
            }
        } catch (IncompleteRegionException e) {
            player.printError(TranslatableComponent.of("craftbook.togglearea.missing-selection"));
            return 0;
        } catch (AuthorizationException e) {
            player.printError(TranslatableComponent.of("worldedit.command.permissions"));
            return 0;
        } catch (WorldEditException e) {
            player.printError(e.getRichMessage());
            return 0;
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeList(CommandContext<CommandSourceStack> context, String namespace,
                                    boolean listAll, int page) {
        Actor actor = getActor(context);

        try {
            boolean personal = false;
            String effectiveNamespace = namespace;
            if (namespace != null) {
                if (!actor.hasPermission("craftbook.togglearea.list." + namespace)) {
                    throw new AuthorizationException(TranslatableComponent.of(
                        "craftbook.togglearea.namespace-permissions",
                        TextComponent.of(namespace)
                    ));
                }
            } else if (listAll && actor.hasPermission("craftbook.togglearea.list.all")) {
                effectiveNamespace = "";
            } else if (actor instanceof CraftBookPlayer) {
                if (!actor.hasPermission("craftbook.togglearea.list.self")) {
                    throw new AuthorizationException();
                }
                effectiveNamespace = actor.getUniqueId().toString();
                personal = true;
            } else {
                actor.printError(TranslatableComponent.of("craftbook.togglearea.player-or-namespace-required"));
                return 0;
            }

            Path areasPath = CopyManager.getAreaPath();

            if (!Files.exists(areasPath) || !Files.isDirectory(areasPath)) {
                actor.printError(TranslatableComponent.of("craftbook.togglearea.no-areas"));
                return 0;
            }

            if (!effectiveNamespace.isEmpty()) {
                areasPath = areasPath.resolve(effectiveNamespace);

                if (!Files.exists(areasPath) || !Files.isDirectory(areasPath)) {
                    actor.printError(TranslatableComponent.of(
                        "craftbook.togglearea.unknown-namespace",
                        TextComponent.of(effectiveNamespace, TextColor.DARK_PURPLE)
                    ));
                    return 0;
                }
            }

            try (var pathStream = Files.walk(areasPath)) {
                List<Path> areaList = pathStream.filter(SCHEMATIC_FILTER::matches).toList();

                if (!areaList.isEmpty()) {
                    AreaListBox areaListBox = new AreaListBox(actor, areaList, personal ? "" : effectiveNamespace, listAll);
                    actor.print(areaListBox.create(page));
                } else {
                    actor.printError(TranslatableComponent.of(
                        "craftbook.togglearea.no-areas-namespace",
                        TextComponent.of(effectiveNamespace)
                    ));
                }
            } catch (IOException | InvalidComponentException e) {
                throw new RuntimeException(e);
            }
        } catch (AuthorizationException e) {
            actor.printError(TranslatableComponent.of("worldedit.command.permissions"));
            return 0;
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeToggle(CommandContext<CommandSourceStack> context, boolean silent) {
        Actor actor = getActor(context);

        Optional<ToggleArea> optToggleArea = getToggleArea();
        if (optToggleArea.isEmpty()) {
            actor.printError(TranslatableComponent.of("craftbook.mechanic.not-enabled"));
            return 0;
        }
        ToggleArea toggleArea = optToggleArea.get();

        String positionStr = context.getArgument("position", String.class);

        World world = null;
        if (actor instanceof CraftBookPlayer player) {
            world = player.getWorld();
        }

        if (world == null) {
            actor.printError(TranslatableComponent.of("craftbook.togglearea.player-or-world-required"));
            return 0;
        }

        // Parse position string (x,y,z format)
        String[] parts = positionStr.split(",");
        if (parts.length != 3) {
            actor.printError(TranslatableComponent.of("craftbook.togglearea.invalid-position"));
            return 0;
        }

        try {
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int z = Integer.parseInt(parts[2].trim());
            BlockVector3 position = BlockVector3.at(x, y, z);

            Location location = new Location(world, position.toVector3());

            if (!toggleArea.toggleCold(actor, location)) {
                actor.printError(TranslatableComponent.of("craftbook.togglearea.toggle-failed"));
                return 0;
            }

            if (!silent) {
                actor.printInfo(TranslatableComponent.of("craftbook.togglearea.toggled"));
            }
        } catch (NumberFormatException e) {
            actor.printError(TranslatableComponent.of("craftbook.togglearea.invalid-position"));
            return 0;
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeDelete(CommandContext<CommandSourceStack> context, String namespace) {
        Actor actor = getActor(context);
        String name = context.getArgument("name", String.class);

        try {
            if (namespace != null && !namespace.equalsIgnoreCase("self")) {
                if (!actor.hasPermission("craftbook.togglearea.delete." + namespace)) {
                    actor.printError(TranslatableComponent.of(
                        "craftbook.togglearea.namespace-permissions",
                        TextComponent.of(namespace)
                    ));
                    return 0;
                }
            } else if (actor instanceof CraftBookPlayer) {
                if (!actor.hasPermission("craftbook.togglearea.delete.self")) {
                    throw new AuthorizationException();
                }
                namespace = actor.getUniqueId().toString();
            } else {
                actor.printError(TranslatableComponent.of("craftbook.togglearea.player-or-namespace-required"));
                return 0;
            }

            Path namespaceFolder = CopyManager.getAreaPath().resolve(namespace);
            Component namespaceComponent = makeFriendlyNamespace(actor, namespace);

            if (!Files.exists(namespaceFolder) || !Files.isDirectory(namespaceFolder)) {
                actor.printError(TranslatableComponent.of(
                    "craftbook.togglearea.unknown-namespace",
                    namespaceComponent
                ));
                return 0;
            }

            List<String> possibleFilenames = Arrays.stream(ClipboardFormats.getFileExtensionArray())
                .map(ext -> name + "." + ext)
                .toList();

            for (String filename : possibleFilenames) {
                Path areaPath = namespaceFolder.resolve(filename);
                if (Files.exists(areaPath)) {
                    try {
                        Files.delete(areaPath);
                        actor.printInfo(TranslatableComponent.of(
                            "craftbook.togglearea.deleted-area",
                            TextComponent.of(name),
                            namespaceComponent
                        ));
                    } catch (IOException e) {
                        actor.printError(TranslatableComponent.of(
                            "craftbook.togglearea.failed-delete",
                            TextComponent.of(name),
                            namespaceComponent
                        ));
                        return 0;
                    }
                    break;
                }
            }
        } catch (AuthorizationException e) {
            actor.printError(TranslatableComponent.of("worldedit.command.permissions"));
            return 0;
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeDeleteAll(CommandContext<CommandSourceStack> context) {
        Actor actor = getActor(context);
        String namespace = context.getArgument("namespace", String.class);

        try {
            if (namespace != null && !namespace.equalsIgnoreCase("self")) {
                if (!actor.hasPermission("craftbook.togglearea.delete." + namespace + ".all")) {
                    actor.printError(TranslatableComponent.of(
                        "craftbook.togglearea.namespace-permissions",
                        TextComponent.of(namespace)
                    ));
                    return 0;
                }
            } else if (actor instanceof CraftBookPlayer) {
                if (!actor.hasPermission("craftbook.togglearea.delete.self.all")) {
                    throw new AuthorizationException();
                }
                namespace = actor.getUniqueId().toString();
            } else {
                actor.printError(TranslatableComponent.of("craftbook.togglearea.player-or-namespace-required"));
                return 0;
            }

            Path namespaceFolder = CopyManager.getAreaPath().resolve(namespace);
            Component namespaceComponent = makeFriendlyNamespace(actor, namespace);

            if (!Files.exists(namespaceFolder) || !Files.isDirectory(namespaceFolder)) {
                actor.printError(TranslatableComponent.of(
                    "craftbook.togglearea.unknown-namespace",
                    namespaceComponent
                ));
                return 0;
            }

            try {
                deleteDir(namespaceFolder);
                actor.printInfo(TranslatableComponent.of(
                    "craftbook.togglearea.deleted-all-in-namespace",
                    namespaceComponent
                ));
            } catch (IOException e) {
                actor.printError(TranslatableComponent.of(
                    "craftbook.togglearea.failed-delete-all",
                    namespaceComponent
                ));
            }
        } catch (AuthorizationException e) {
            actor.printError(TranslatableComponent.of("worldedit.command.permissions"));
            return 0;
        }

        return Command.SINGLE_SUCCESS;
    }

    private static void deleteDir(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var listStream = Files.list(path)) {
                var children = listStream.filter(SCHEMATIC_FILTER::matches).toList();
                for (Path child : children) {
                    Files.delete(child);
                }
            }
        }
        Files.delete(path);
    }
}

