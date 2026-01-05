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

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.auth.AuthorizationException;
import com.sk89q.worldedit.util.formatting.component.PaginationBox;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import com.sk89q.worldedit.util.formatting.text.event.HoverEvent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.enginehub.craftbook.CraftBook;
import org.enginehub.craftbook.CraftBookPlayer;
import org.enginehub.craftbook.exception.CraftBookException;
import org.enginehub.craftbook.mechanics.variables.VariableKey;
import org.enginehub.craftbook.mechanics.variables.VariableManager;
import org.enginehub.craftbook.mechanics.variables.exception.InvalidVariableException;
import org.enginehub.craftbook.mechanics.variables.exception.UnknownVariableException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.getActor;
import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.greedyStringArg;
import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.integerArg;
import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.literal;
import static org.enginehub.craftbook.bukkit.command.BrigadierCommandBuilder.stringArg;

/**
 * Brigadier-based implementation of Variable commands.
 */
public final class VariableBrigadierCommands {

    private VariableBrigadierCommands() {
    }

    /**
     * Register all Variable commands with the Paper command manager.
     */
    public static void register(PaperCommandManager commandManager) {
        RequiredArgumentBuilder<CommandSourceStack, String> variableArg = stringArg("variable");
        RequiredArgumentBuilder<CommandSourceStack, String> valueArg = greedyStringArg("value");
        RequiredArgumentBuilder<CommandSourceStack, String> namespaceArg = stringArg("namespace");

        LiteralArgumentBuilder<CommandSourceStack> variables = literal("variables")
            // set command
            .then(literal("set")
                .requires(source -> source.getSender().hasPermission("craftbook.variables.set"))
                .then(stringArg("variable")
                    .then(greedyStringArg("value")
                        .executes(ctx -> executeSet(ctx, null))
                        .then(literal("-n")
                            .then(stringArg("namespace")
                                .executes(ctx -> {
                                    String ns = ctx.getArgument("namespace", String.class);
                                    return executeSet(ctx, ns);
                                }))))))
            // define command
            .then(literal("define")
                .requires(source -> source.getSender().hasPermission("craftbook.variables.define"))
                .then(stringArg("variable")
                    .then(greedyStringArg("value")
                        .executes(ctx -> executeDefine(ctx, null))
                        .then(literal("-n")
                            .then(stringArg("namespace")
                                .executes(ctx -> {
                                    String ns = ctx.getArgument("namespace", String.class);
                                    return executeDefine(ctx, ns);
                                }))))))
            // get command
            .then(literal("get")
                .requires(source -> source.getSender().hasPermission("craftbook.variables.get"))
                .then(stringArg("variable")
                    .executes(ctx -> executeGet(ctx, null))
                    .then(literal("-n")
                        .then(stringArg("namespace")
                            .executes(ctx -> {
                                String ns = ctx.getArgument("namespace", String.class);
                                return executeGet(ctx, ns);
                            })))))
            // list command
            .then(literal("list")
                .requires(source -> source.getSender().hasPermission("craftbook.variables.list"))
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
            // remove command
            .then(literal("remove")
                .requires(source -> source.getSender().hasPermission("craftbook.variables.remove"))
                .then(stringArg("variable")
                    .executes(ctx -> executeRemove(ctx, null))
                    .then(literal("-n")
                        .then(stringArg("namespace")
                            .executes(ctx -> {
                                String ns = ctx.getArgument("namespace", String.class);
                                return executeRemove(ctx, ns);
                            })))))
            // append command
            .then(literal("append")
                .requires(source -> source.getSender().hasPermission("craftbook.variables.append"))
                .then(stringArg("variable")
                    .then(greedyStringArg("value")
                        .executes(ctx -> executeAppend(ctx, null))
                        .then(literal("-n")
                            .then(stringArg("namespace")
                                .executes(ctx -> {
                                    String ns = ctx.getArgument("namespace", String.class);
                                    return executeAppend(ctx, ns);
                                }))))))
            // prepend command
            .then(literal("prepend")
                .requires(source -> source.getSender().hasPermission("craftbook.variables.prepend"))
                .then(stringArg("variable")
                    .then(greedyStringArg("value")
                        .executes(ctx -> executePrepend(ctx, null))
                        .then(literal("-n")
                            .then(stringArg("namespace")
                                .executes(ctx -> {
                                    String ns = ctx.getArgument("namespace", String.class);
                                    return executePrepend(ctx, ns);
                                }))))))
            // toggle command
            .then(literal("toggle")
                .requires(source -> source.getSender().hasPermission("craftbook.variables.toggle"))
                .then(stringArg("variable")
                    .executes(ctx -> executeToggle(ctx, null))
                    .then(literal("-n")
                        .then(stringArg("namespace")
                            .executes(ctx -> {
                                String ns = ctx.getArgument("namespace", String.class);
                                return executeToggle(ctx, ns);
                            })))));

        // Build the command first
        LiteralCommandNode<CommandSourceStack> variablesNode = variables.build();

        // Register aliases with redirects
        LiteralArgumentBuilder<CommandSourceStack> var = literal("var").redirect(variablesNode);
        LiteralArgumentBuilder<CommandSourceStack> variable = literal("variable").redirect(variablesNode);
        LiteralArgumentBuilder<CommandSourceStack> vars = literal("vars").redirect(variablesNode);

        // Register commands
        commandManager.register(variables);
        commandManager.register(var);
        commandManager.register(variable);
        commandManager.register(vars);
    }

    private static int executeSet(CommandContext<CommandSourceStack> context, String namespace) {
        Actor actor = getActor(context);
        try {
            String variable = context.getArgument("variable", String.class);
            String value = context.getArgument("value", String.class);
            VariableKey key = VariableKey.of(namespace, variable, actor);

            checkVariableExists(key);
            checkVariablePermission(actor, key, "modify");

            VariableManager.instance.setVariable(key, value);
            actor.printInfo(TranslatableComponent.of(
                "craftbook.variables.set",
                key.getRichName(),
                TextComponent.of(value, TextColor.WHITE)
            ));
            return Command.SINGLE_SUCCESS;
        } catch (CraftBookException e) {
            actor.printError(e.getRichMessage());
            return 0;
        } catch (AuthorizationException e) {
            actor.printError(TranslatableComponent.of("worldedit.command.permissions"));
            return 0;
        }
    }

    private static int executeDefine(CommandContext<CommandSourceStack> context, String namespace) {
        Actor actor = getActor(context);
        try {
            String variable = context.getArgument("variable", String.class);
            String value = context.getArgument("value", String.class);
            VariableKey key = VariableKey.of(namespace, variable, actor);

            checkVariableDoesNotExist(key);
            checkVariablePermission(actor, key, "define");

            VariableManager.instance.setVariable(key, value);
            actor.printInfo(TranslatableComponent.of(
                "craftbook.variables.defined",
                key.getRichName(),
                TextComponent.of(value, TextColor.WHITE)
            ));
            return Command.SINGLE_SUCCESS;
        } catch (CraftBookException e) {
            actor.printError(e.getRichMessage());
            return 0;
        } catch (AuthorizationException e) {
            actor.printError(TranslatableComponent.of("worldedit.command.permissions"));
            return 0;
        }
    }

    private static int executeGet(CommandContext<CommandSourceStack> context, String namespace) {
        Actor actor = getActor(context);
        try {
            String variable = context.getArgument("variable", String.class);
            VariableKey key = VariableKey.of(namespace, variable, actor);

            checkVariableExists(key);
            checkVariablePermission(actor, key, "get");

            String value = VariableManager.instance.getVariable(key);
            actor.printInfo(TranslatableComponent.of(
                "craftbook.variables.get",
                key.getRichName(),
                TextComponent.of(value, TextColor.WHITE)
            ));
            return Command.SINGLE_SUCCESS;
        } catch (CraftBookException e) {
            actor.printError(e.getRichMessage());
            return 0;
        } catch (AuthorizationException e) {
            actor.printError(TranslatableComponent.of("worldedit.command.permissions"));
            return 0;
        }
    }

    private static int executeList(CommandContext<CommandSourceStack> context, String namespace, boolean all, int page) {
        Actor actor = getActor(context);
        try {
            String effectiveNamespace = namespace;
            if (!all && namespace == null) {
                if (VariableManager.instance.isDefaultToGlobal() || !(actor instanceof CraftBookPlayer)) {
                    effectiveNamespace = VariableManager.GLOBAL_NAMESPACE;
                } else {
                    effectiveNamespace = actor.getUniqueId().toString();
                }
            } else if (all && namespace != null) {
                actor.printError(TranslatableComponent.of("craftbook.variables.list.all-and-namespace"));
                return 0;
            }

            List<VariableKey> variableKeys = new ArrayList<>();

            if (effectiveNamespace != null) {
                Set<String> variableNames = VariableManager.instance.getVariableStore()
                    .getOrDefault(effectiveNamespace, ImmutableMap.of()).keySet();
                for (String variableName : variableNames) {
                    variableKeys.add(VariableKey.of(effectiveNamespace, variableName, actor));
                }
            } else {
                for (Entry<String, Map<String, String>> namespaceEntry : VariableManager.instance.getVariableStore().entrySet()) {
                    for (String var : namespaceEntry.getValue().keySet()) {
                        variableKeys.add(VariableKey.of(namespaceEntry.getKey(), var, actor));
                    }
                }
            }

            String pageCommand = "/variables list " + page + " " + (effectiveNamespace == null ? "-a" : "-n " + effectiveNamespace);
            VariableListPaginationBox variableListBox = new VariableListPaginationBox(
                variableKeys,
                effectiveNamespace,
                pageCommand
            );
            actor.printInfo(variableListBox.create(page));
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            actor.printError(TextComponent.of("Failed to list variables: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeRemove(CommandContext<CommandSourceStack> context, String namespace) {
        Actor actor = getActor(context);
        try {
            String variable = context.getArgument("variable", String.class);
            VariableKey key = VariableKey.of(namespace, variable, actor);

            checkVariableExists(key);
            checkVariablePermission(actor, key, "remove");

            VariableManager.instance.removeVariable(key);
            actor.printInfo(TranslatableComponent.of(
                "craftbook.variables.remove",
                key.getRichName()
            ));
            return Command.SINGLE_SUCCESS;
        } catch (CraftBookException e) {
            actor.printError(e.getRichMessage());
            return 0;
        } catch (AuthorizationException e) {
            actor.printError(TranslatableComponent.of("worldedit.command.permissions"));
            return 0;
        }
    }

    private static int executeAppend(CommandContext<CommandSourceStack> context, String namespace) {
        Actor actor = getActor(context);
        try {
            String variable = context.getArgument("variable", String.class);
            String value = context.getArgument("value", String.class);
            VariableKey key = VariableKey.of(namespace, variable, actor);

            checkVariableExists(key);
            checkVariablePermission(actor, key, "modify");

            String existing = VariableManager.instance.getVariable(key);
            String newValue = existing + value;
            VariableManager.instance.setVariable(key, newValue);
            actor.printInfo(TranslatableComponent.of(
                "craftbook.variables.set",
                key.getRichName(),
                TextComponent.of(newValue, TextColor.WHITE)
            ));
            return Command.SINGLE_SUCCESS;
        } catch (CraftBookException e) {
            actor.printError(e.getRichMessage());
            return 0;
        } catch (AuthorizationException e) {
            actor.printError(TranslatableComponent.of("worldedit.command.permissions"));
            return 0;
        }
    }

    private static int executePrepend(CommandContext<CommandSourceStack> context, String namespace) {
        Actor actor = getActor(context);
        try {
            String variable = context.getArgument("variable", String.class);
            String value = context.getArgument("value", String.class);
            VariableKey key = VariableKey.of(namespace, variable, actor);

            checkVariableExists(key);
            checkVariablePermission(actor, key, "modify");

            String existing = VariableManager.instance.getVariable(key);
            String newValue = value + existing;
            VariableManager.instance.setVariable(key, newValue);
            actor.printInfo(TranslatableComponent.of(
                "craftbook.variables.set",
                key.getRichName(),
                TextComponent.of(newValue, TextColor.WHITE)
            ));
            return Command.SINGLE_SUCCESS;
        } catch (CraftBookException e) {
            actor.printError(e.getRichMessage());
            return 0;
        } catch (AuthorizationException e) {
            actor.printError(TranslatableComponent.of("worldedit.command.permissions"));
            return 0;
        }
    }

    private static int executeToggle(CommandContext<CommandSourceStack> context, String namespace) {
        Actor actor = getActor(context);
        try {
            String variable = context.getArgument("variable", String.class);
            VariableKey key = VariableKey.of(namespace, variable, actor);

            checkVariableExists(key);
            checkVariablePermission(actor, key, "modify");

            String var = VariableManager.instance.getVariable(key);
            if (var != null) {
                String result;
                if (var.equalsIgnoreCase("0") || var.equalsIgnoreCase("1")) {
                    result = var.equalsIgnoreCase("1") ? "0" : "1";
                } else if (var.equalsIgnoreCase("true") || var.equalsIgnoreCase("false")) {
                    result = var.equalsIgnoreCase("true") ? "false" : "true";
                } else if (var.equalsIgnoreCase("yes") || var.equalsIgnoreCase("no")) {
                    result = var.equalsIgnoreCase("yes") ? "no" : "yes";
                } else {
                    throw new InvalidVariableException(TranslatableComponent.of(
                        "craftbook.variables.not-boolean",
                        key.getRichName()
                    ));
                }

                VariableManager.instance.setVariable(key, result);
                actor.printInfo(TranslatableComponent.of(
                    "craftbook.variables.set",
                    key.getRichName(),
                    TextComponent.of(result, TextColor.WHITE)
                ));
            }
            return Command.SINGLE_SUCCESS;
        } catch (CraftBookException e) {
            actor.printError(e.getRichMessage());
            return 0;
        } catch (AuthorizationException e) {
            actor.printError(TranslatableComponent.of("worldedit.command.permissions"));
            return 0;
        }
    }

    private static void checkVariableExists(VariableKey key) throws UnknownVariableException {
        if (!VariableManager.instance.hasVariable(key)) {
            throw new UnknownVariableException(key);
        }
    }

    private static void checkVariableDoesNotExist(VariableKey key) throws CraftBookException {
        if (VariableManager.instance.hasVariable(key)) {
            throw new CraftBookException(TranslatableComponent.of(
                "craftbook.variables.already-exists",
                key.getRichName()
            ));
        }
    }

    private static void checkVariablePermission(Actor actor, VariableKey key, String action)
            throws AuthorizationException {
        String permission = "craftbook.variables." + action;
        if (key.getNamespace().equalsIgnoreCase(VariableManager.GLOBAL_NAMESPACE)) {
            permission += ".global";
        } else if (key.getNamespace().equalsIgnoreCase(actor.getUniqueId().toString())) {
            permission += ".self";
        } else {
            permission += ".other";
        }
        if (!actor.hasPermission(permission)) {
            throw new AuthorizationException();
        }
    }

    /**
     * Pagination box for variable listing.
     */
    private static class VariableListPaginationBox extends PaginationBox {

        private final List<VariableKey> variableKeys;
        private final boolean singleNamespace;

        private static String getFriendlyNamespaceName(String namespace) {
            if (namespace == null) {
                return "All";
            }
            if (namespace.contains("-")) {
                try {
                    org.enginehub.craftbook.util.profile.Profile profile =
                        CraftBook.getInstance().getProfileService().findByUuid(UUID.fromString(namespace));
                    if (profile != null) {
                        namespace = profile.getName();
                    }
                } catch (IOException | InterruptedException e) {
                    CraftBook.LOGGER.warn("Failed to lookup Minecraft profile", e);
                }
            }
            return namespace;
        }

        private VariableListPaginationBox(List<VariableKey> variableKeys, String namespace, String pageCommand) {
            super("Variables (" + getFriendlyNamespaceName(namespace) + ")", pageCommand);

            this.variableKeys = variableKeys;
            this.singleNamespace = namespace != null;
        }

        @Override
        public Component getComponent(int number) {
            checkArgument(number < this.variableKeys.size() && number >= 0);

            VariableKey key = this.variableKeys.get(number);

            String label = key.getVariable();
            if (!this.singleNamespace) {
                label = key.toString();
            }

            String varValue = VariableManager.instance.getVariable(key);
            Component value;
            TextColor valueColor = TextColor.GRAY;
            if (varValue == null) {
                value = TranslatableComponent.of("craftbook.variables.undefined");
                valueColor = TextColor.DARK_GRAY;
            } else {
                value = TextComponent.of(varValue);
            }

            TextComponent labelComponent = TextComponent.of(label)
                .color(TextColor.YELLOW)
                .clickEvent(ClickEvent.copyToClipboard(key.toString()));
            Component copyComponent = TranslatableComponent.of("craftbook.variables.list.copy");
            if (this.singleNamespace) {
                labelComponent = labelComponent
                    .hoverEvent(HoverEvent.showText(TextComponent.of(key.toString())
                        .append(TextComponent.newline().append(copyComponent))));
            } else {
                labelComponent = labelComponent
                    .hoverEvent(HoverEvent.showText(copyComponent));
            }

            return TextComponent.builder()
                .content("")
                .append(labelComponent)
                .append(TextComponent.of("="))
                .append(value.color(valueColor))
                .build();
        }

        @Override
        public int getComponentsSize() {
            return this.variableKeys.size();
        }
    }
}
