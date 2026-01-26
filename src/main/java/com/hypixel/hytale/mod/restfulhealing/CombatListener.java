package com.hypixel.hytale.mod.restfulhealing;

import java.util.UUID;

public class CombatListener {

    private final RestfulHealingPlugin plugin;

    public CombatListener(RestfulHealingPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isInCombat(UUID playerUuid) {
        PlayerHealingState healingState = plugin.getHealingStates().get(playerUuid);
        if (healingState != null) {
            return healingState.isInCombat(plugin.getConfig().getCombatTimeout());
        }
        return false; 
    }

    public long getTimeSinceLastCombat(UUID playerUuid) {
        PlayerHealingState healingState = plugin.getHealingStates().get(playerUuid);
        if (healingState != null) {
            return System.currentTimeMillis() - healingState.getLastCombatTime();
        }
        return Long.MAX_VALUE; 
    }
}
