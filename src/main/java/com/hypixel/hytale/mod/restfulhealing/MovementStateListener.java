package com.hypixel.hytale.mod.restfulhealing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.protocol.packets.player.ClientMovement;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

/**
 * MovementStateListener handles real-time movement state changes from ClientMovement packets
 * This connects to the actual Hytale movement system to detect sitting/sleeping states
 */
public class MovementStateListener {
    
    private final RestfulHealingPlugin plugin;
    
    public MovementStateListener(RestfulHealingPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Process a ClientMovement packet to detect state changes
     * @param player The player whose movement state changed
     * @param clientMovement The movement packet containing state information
     * @param playerRef Reference to the player's entity
     */
    public void onClientMovement(Player player, ClientMovement clientMovement, Ref<EntityStore> playerRef) {
        UUID playerUuid = player.getUuid();
        
        // Get the movement states from the packet
        MovementStates movementStates = clientMovement.movementStates;
        if (movementStates == null) {
            return; // No movement state data in this packet
        }
        
        // Extract the relevant states
        boolean isSitting = movementStates.sitting;
        boolean isSleeping = movementStates.sleeping;
        boolean isWalking = movementStates.walking;
        boolean isRunning = movementStates.running;
        boolean isJumping = movementStates.jumping;
        
        // Notify the state listener about the movement change
        plugin.onPlayerStateChange(playerUuid, isSitting, isSleeping, isWalking, isRunning, isJumping);
        
        // Log for debugging
        if (plugin.getConfig().isDebugMode()) {
            plugin.getLogger().at(java.util.logging.Level.FINE).log("Movement state update for " + playerUuid +
                ": sitting=" + isSitting + ", sleeping=" + isSleeping +
                ", walking=" + isWalking + ", running=" + isRunning +
                ", jumping=" + isJumping);
        }
    }
    
    /**
     * Alternative method to get movement states directly from the player's MovementStatesComponent
     * @param playerRef Reference to the player's entity
     */
    public void checkPlayerMovementState(Ref<EntityStore> playerRef) {
        // Get the movement states component from the player
        MovementStatesComponent movementComponent = 
            playerRef.getStore().getComponent(playerRef, MovementStatesComponent.getComponentType());
            
        if (movementComponent != null) {
            MovementStates movementStates = movementComponent.getMovementStates();
            if (movementStates != null) {
                Player player = playerRef.getStore().getComponent(playerRef, Player.getComponentType());
                
                if (player != null) {
                    UUID playerUuid = player.getUuid();
                    
                    // Extract the relevant states
                    boolean isSitting = movementStates.sitting;
                    boolean isSleeping = movementStates.sleeping;
                    boolean isWalking = movementStates.walking;
                    boolean isRunning = movementStates.running;
                    boolean isJumping = movementStates.jumping;
                    
                    // Notify the state listener about the movement change
                    plugin.onPlayerStateChange(playerUuid, isSitting, isSleeping, isWalking, isRunning, isJumping);
                    
                    // Log for debugging
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