package com.hypixel.hytale.mod.restfulhealing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.protocol.packets.player.ClientMovement;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;
import java.util.UUID;

public class CombatListener {

    private final RestfulHealingPlugin plugin;

    public CombatListener(RestfulHealingPlugin plugin) {
        this.plugin = plugin;
    }

    private final Map<UUID, Long> lastCombatLogTime = new java.util.concurrent.ConcurrentHashMap<>();

    public void onPlayerInteract(PlayerInteractEvent event) {
        UUID playerUuid = event.getPlayer().getUuid();
        PlayerHealingState healingState = plugin.getHealingStates().get(playerUuid);
        if (healingState != null) {
            healingState.updateCombatTime();

            if (plugin.getConfig().isDebugMode()) {
                long currentTime = System.currentTimeMillis();
                Long lastLogTime = lastCombatLogTime.get(playerUuid);

                if (lastLogTime == null || (currentTime - lastLogTime) >= 5000) {
                    plugin.getLogger().at(java.util.logging.Level.FINE).log("Player " + playerUuid + " entered combat due to interaction");
                    lastCombatLogTime.put(playerUuid, currentTime);
                }
            }
        }
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