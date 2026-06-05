package fr.miuby.lib.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.miuby.lib.log.MLLogManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.logging.Level;

/**
 * Sous-commande générique de gestion des logs {@link MLLogManager}.
 *
 * <p>Tout plugin MiubyLib peut l'insérer dans son arbre de commandes pour obtenir gratuitement
 * la gestion des tags, niveaux et modes de log, sans dépendance à ses propres enums de tags.</p>
 *
 * <p><b>Intégration</b></p>
 * <pre>{@code
 * // Dans XxxCommand.createCommand() :
 * return Commands.literal("monplugin")
 *         .requires(s -> s.getSender().isOp())
 *         .then(MLLogCommand.create())   // expose /monplugin log ...
 *         .then(Commands.literal("autre")...);
 * }</pre>
 *
 * <p><b>Sous-commandes exposées</b></p>
 * <ul>
 *   <li>{@code log status} — affiche l'état de tous les tags et levels</li>
 *   <li>{@code log tag toggle|enable|disable <TAG>} — bascule un tag</li>
 *   <li>{@code log level toggle|enable|disable <LEVEL>} — bascule un level JUL</li>
 *   <li>{@code log mode production|debug|quiet} — presets rapides</li>
 * </ul>
 */
public final class MLLogCommand {
    private static final String TAG_ARG   = "tag";
    private static final String LEVEL_ARG = "level";

    private MLLogCommand() {}

    /**
     * Retourne le sous-arbre Brigadier {@code log} prêt à être passé à un {@code .then()}.
     *
     * @return le builder Brigadier de la sous-commande {@code log}
     */
    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("log")
                .then(buildStatus())
                .then(buildTag())
                .then(buildLevel())
                .then(buildMode());
    }

    // =========================================================================
    // /... log status
    // =========================================================================

    private static LiteralArgumentBuilder<CommandSourceStack> buildStatus() {
        return Commands.literal("status")
                .executes(ctx -> {
                    var sender = ctx.getSource().getSender();
                    var lm = MLLogManager.getInstance();

                    sender.sendMessage(Component.text("╔═══════════════════════════╗", NamedTextColor.GOLD));
                    sender.sendMessage(Component.text("║    LOG STATUS             ║", NamedTextColor.GOLD));
                    sender.sendMessage(Component.text("╠═══════════════════════════╣", NamedTextColor.GOLD));

                    sender.sendMessage(Component.text("║ TAGS:", NamedTextColor.YELLOW));
                    for (var entry : lm.getAllTagStates().entrySet()) {
                        NamedTextColor color = Boolean.TRUE.equals(entry.getValue()) ? NamedTextColor.GREEN : NamedTextColor.RED;
                        String icon = Boolean.TRUE.equals(entry.getValue()) ? "✓" : "✗";
                        sender.sendMessage(Component.text("║   " + icon + " ", color).append(Component.text(entry.getKey(), NamedTextColor.WHITE)));
                    }

                    sender.sendMessage(Component.text("║", NamedTextColor.GOLD));

                    sender.sendMessage(Component.text("║ LEVELS:", NamedTextColor.YELLOW));
                    for (var entry : lm.getAllLevelStates().entrySet()) {
                        NamedTextColor color = Boolean.TRUE.equals(entry.getValue()) ? NamedTextColor.GREEN : NamedTextColor.RED;
                        String icon = Boolean.TRUE.equals(entry.getValue()) ? "✓" : "✗";
                        sender.sendMessage(Component.text("║   " + icon + " ", color).append(Component.text(entry.getKey().getName(), NamedTextColor.WHITE)));
                    }

                    sender.sendMessage(Component.text("╚═══════════════════════════╝", NamedTextColor.GOLD));
                    return Command.SINGLE_SUCCESS;
                });
    }

    // =========================================================================
    // /... log tag toggle|enable|disable <TAG>
    // =========================================================================

    private static LiteralArgumentBuilder<CommandSourceStack> buildTag() {
        return Commands.literal("tag")
                .then(Commands.literal("toggle")
                        .then(Commands.argument(TAG_ARG, StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    MLLogManager.getInstance().getAllTagStates().keySet().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, TAG_ARG);
                                    MLLogManager.getInstance().toggleTag(name);
                                    boolean enabled = MLLogManager.getInstance().isTagEnabled(name);
                                    ctx.getSource().getSender().sendMessage(
                                            Component.text("Tag [" + name + "] ", NamedTextColor.YELLOW)
                                                    .append(Component.text(enabled ? "activé" : "désactivé", enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                                    );
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(Commands.literal("enable")
                        .then(Commands.argument(TAG_ARG, StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    MLLogManager.getInstance().getAllTagStates().keySet().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, TAG_ARG);
                                    MLLogManager.getInstance().setTagEnabled(name, true);
                                    ctx.getSource().getSender().sendMessage(Component.text("Tag [" + name + "] activé", NamedTextColor.GREEN));
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(Commands.literal("disable")
                        .then(Commands.argument(TAG_ARG, StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    MLLogManager.getInstance().getAllTagStates().keySet().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, TAG_ARG);
                                    MLLogManager.getInstance().setTagEnabled(name, false);
                                    ctx.getSource().getSender().sendMessage(Component.text("Tag [" + name + "] désactivé", NamedTextColor.RED));
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }

    // =========================================================================
    // /... log level toggle|enable|disable <LEVEL>
    // =========================================================================

    private static LiteralArgumentBuilder<CommandSourceStack> buildLevel() {
        return Commands.literal("level")
                .then(Commands.literal("toggle")
                        .then(Commands.argument(LEVEL_ARG, StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("INFO"); builder.suggest("WARNING"); builder.suggest("SEVERE");
                                    builder.suggest("CONFIG"); builder.suggest("FINE"); builder.suggest("FINER"); builder.suggest("FINEST");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    Level level = Level.parse(StringArgumentType.getString(ctx, LEVEL_ARG));
                                    MLLogManager.getInstance().toggleLevel(level);
                                    boolean enabled = MLLogManager.getInstance().isLevelEnabled(level);
                                    ctx.getSource().getSender().sendMessage(
                                            Component.text("Level [" + level.getName() + "] ", NamedTextColor.YELLOW)
                                                    .append(Component.text(enabled ? "activé" : "désactivé", enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                                    );
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(Commands.literal("enable")
                        .then(Commands.argument(LEVEL_ARG, StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("INFO"); builder.suggest("WARNING"); builder.suggest("SEVERE");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    Level level = Level.parse(StringArgumentType.getString(ctx, LEVEL_ARG));
                                    MLLogManager.getInstance().setLevelEnabled(level, true);
                                    ctx.getSource().getSender().sendMessage(Component.text("Level [" + level.getName() + "] activé", NamedTextColor.GREEN));
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(Commands.literal("disable")
                        .then(Commands.argument(LEVEL_ARG, StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("INFO"); builder.suggest("WARNING"); builder.suggest("SEVERE");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    Level level = Level.parse(StringArgumentType.getString(ctx, LEVEL_ARG));
                                    MLLogManager.getInstance().setLevelEnabled(level, false);
                                    ctx.getSource().getSender().sendMessage(Component.text("Level [" + level.getName() + "] désactivé", NamedTextColor.RED));
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }

    // =========================================================================
    // /... log mode production|debug|quiet
    // =========================================================================

    private static LiteralArgumentBuilder<CommandSourceStack> buildMode() {
        return Commands.literal("mode")
                .then(Commands.literal("production")
                        .executes(ctx -> {
                            MLLogManager.getInstance().setProductionMode();
                            ctx.getSource().getSender().sendMessage(Component.text("Mode PRODUCTION activé (WARNING + SEVERE)", NamedTextColor.GREEN));
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(Commands.literal("debug")
                        .executes(ctx -> {
                            MLLogManager.getInstance().setDebugMode();
                            ctx.getSource().getSender().sendMessage(Component.text("Mode DEBUG activé (tout)", NamedTextColor.GREEN));
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(Commands.literal("quiet")
                        .executes(ctx -> {
                            MLLogManager.getInstance().setQuietMode();
                            ctx.getSource().getSender().sendMessage(Component.text("Mode QUIET activé (seulement SEVERE)", NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }
}
