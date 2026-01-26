package com.hypixel.hytale.mod.restfulhealing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.EventListener;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.protocol.packets.player.ClientMovement;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

/**
 * CombatListener handles combat-related events to track when players take damage
 */
public class CombatListener {
    
    private final RestfulHealingPlugin plugin;
    
    public CombatListener(RestfulHealingPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Called when a player interacts (which could include taking damage)
     */
    public void onPlayerInteract(PlayerInteractEvent event) {
        UUID playerUuid = event.getPlayer().getUuid();
        PlayerHealingState healingState = plugin.getHealingStates().get(playerUuid);
        if (healingState != null) {
            healingState.updateCombatTime();
            plugin.getLogger().debug("Player " + playerUuid + " entered combat");
        }
    }
}