package com.hypixel.hytale.mod.restfulhealing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.protocol.packets.player.ClientMovement;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

public class MovementStateListener {
    
    private final RestfulHealingPlugin plugin;
    
    public MovementStateListener(RestfulHealingPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void onClientMovement(Player player, ClientMovement clientMovement, Ref<EntityStore> playerRef) {
        UUID playerUuid = null;
        if (playerRef != null && playerRef.isValid()) {
            UUIDComponent uuidComp = playerRef.getStore().getComponent(playerRef, UUIDComponent.getComponentType());
            if (uuidComp != null) {
                playerUuid = uuidComp.getUuid();
            }
        }
        
        if (playerUuid == null) {
            return;
        }

        MovementStates movementStates = clientMovement.movementStates;
        if (movementStates == null) {
            return;
        }

        boolean isSitting = movementStates.sitting;
        boolean isSleeping = movementStates.sleeping;
        boolean isWalking = movementStates.walking;
        boolean isRunning = movementStates.running;
        boolean isJumping = movementStates.jumping;

        plugin.onPlayerStateChange(playerUuid, isSitting, isSleeping, isWalking, isRunning, isJumping);

        if (plugin.getConfig().isDebugMode()) {
            plugin.getLogger().at(java.util.logging.Level.FINE).log("Movement state update for " + playerUuid +
                ": sitting=" + isSitting + ", sleeping=" + isSleeping +
                ", walking=" + isWalking + ", running=" + isRunning +
                ", jumping=" + isJumping);
        }
    }
    
    public void checkPlayerMovementState(Ref<EntityStore> playerRef) {
        MovementStatesComponent movementComponent = 
            playerRef.getStore().getComponent(playerRef, MovementStatesComponent.getComponentType());
            
        if (movementComponent != null) {
            MovementStates movementStates = movementComponent.getMovementStates();
            if (movementStates != null) {
                Player player = playerRef.getStore().getComponent(playerRef, Player.getComponentType());
                
                if (player != null) {
                    UUID playerUuid = null;
                    if (playerRef.isValid()) {
                        UUIDComponent uuidComp = playerRef.getStore().getComponent(playerRef, UUIDComponent.getComponentType());
                        if (uuidComp != null) {
                            playerUuid = uuidComp.getUuid();
                        }
                    }
                    
                    if (playerUuid == null) {
                        return;
                    }

                    boolean isSitting = movementStates.sitting;
                    boolean isSleeping = movementStates.sleeping;
                    boolean isWalking = movementStates.walking;
                    boolean isRunning = movementStates.running;
                    boolean isJumping = movementStates.jumping;

                    plugin.onPlayerStateChange(playerUuid, isSitting, isSleeping, isWalking, isRunning, isJumping);

                    if (plugin.getConfig().isDebugMode()) {
                        plugin.getLogger().at(java.util.logging.Level.FINE).log("Component-based movement state update for " + playerUuid +
                            ": sitting=" + isSitting + ", sleeping=" + isSleeping +
                            ", walking=" + isWalking + ", running=" + isRunning +
                            ", jumping=" + isJumping);
                    }
                }
            }
        }
    }
}