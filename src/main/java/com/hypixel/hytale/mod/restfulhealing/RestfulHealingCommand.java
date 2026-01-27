package com.hypixel.hytale.mod.restfulhealing;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;

import javax.annotation.Nonnull;

public class RestfulHealingCommand extends AbstractCommandCollection {

    private final RestfulHealingPlugin plugin;

    public RestfulHealingCommand(RestfulHealingPlugin plugin) {
        super("restfulhealing", "Restful Healing commands");
        this.plugin = plugin;

        this.addAliases("rh");

        this.addSubCommand(new DebugCommand(plugin));
    }

    private static class DebugCommand extends CommandBase {
        private final RestfulHealingPlugin plugin;
        private final RequiredArg<String> actionArg;

        public DebugCommand(RestfulHealingPlugin plugin) {
            super("debug", "Toggle debug mode for Restful Healing");
            this.plugin = plugin;

            this.actionArg = this.withRequiredArg("action", "Enable or disable debug mode", ArgTypes.STRING);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String action = context.get(actionArg);

            boolean debugEnabled;
            if (action.equalsIgnoreCase("on")) {
                debugEnabled = true;
            } else if (action.equalsIgnoreCase("off")) {
                debugEnabled = false;
            } else {
                context.sender().sendMessage(Message.raw("§cInvalid option. Use §eon§c or §eoff§c."));
                return;
            }

            HealingConfig currentConfig = plugin.getConfig();
            HealingConfig newConfig = new HealingConfig.Builder()
                .sittingHealRate(currentConfig.getSittingHealRate())
                .sleepingHealRate(currentConfig.getSleepingHealRate())
                .combatTimeout(currentConfig.getCombatTimeout())
                .accelerationTime(currentConfig.getAccelerationTime())
                .healThreshold(currentConfig.getHealThreshold())
                .acceleratedRate(currentConfig.getAcceleratedRate())
                .enabled(currentConfig.isEnabled())
                .debugMode(debugEnabled)
                .build();

            plugin.updateConfig(newConfig);

            if (debugEnabled) {
                context.sender().sendMessage(Message.raw("§aDebug mode §eenabled§a. You will now see healing debug messages including TPS impact and player states."));
                context.sender().sendMessage(Message.raw("§eInfo:§r Debug will show number of healing players, TPS estimates, and individual healing stats."));
            } else {
                context.sender().sendMessage(Message.raw("§aDebug mode §cdisabled§a. Healing debug messages turned off."));
            }
        }
    }
}