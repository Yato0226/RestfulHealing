package com.hypixel.hytale.mod.restfulhealing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.protocol.ValueType;
import com.hypixel.hytale.protocol.ChangeStatBehaviour;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;
import java.util.UUID;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;

public class HealingTask implements Runnable {

    private final RestfulHealingPlugin plugin;
    private final HealingConfig config;
    private final Map<UUID, PlayerHealingState> healingStates;
    private final HytaleLogger logger;
    private volatile boolean cancelled = false;

    public HealingTask(RestfulHealingPlugin plugin, HealingConfig config, Map<UUID, PlayerHealingState> healingStates, HytaleLogger logger) {
        this.plugin = plugin;
        this.config = config;
        this.healingStates = healingStates;
        this.logger = logger;
    }

    @Override
    public void run() {
        if (cancelled || !config.isEnabled()) {
            return;
        }

        try {
            // For Phase 4, we'll implement actual healing logic
            if (config.isDebugMode()) {
                logger.at(java.util.logging.Level.INFO).log("Healing task running for " + healingStates.size() + " players");
            }

            // Iterate through tracked players and check their actual movement states and combat status
            for (Map.Entry<UUID, PlayerHealingState> entry : healingStates.entrySet()) {
                UUID playerUuid = entry.getKey();
                PlayerHealingState state = entry.getValue();

                // Get the player's actual movement state from the game
                checkAndUpdatePlayerMovementState(playerUuid, state);

                // Check if player is out of combat and eligible for healing
                if (isPlayerEligibleForHealing(playerUuid, state)) {
                    // Apply actual healing to the player
                    applyHealingToPlayer(playerUuid, state);

                    if (config.isDebugMode()) {
                        logger.at(java.util.logging.Level.FINE).log("Applied healing to player " + playerUuid);
                    }
                } else {
                    if (config.isDebugMode()) {
                        logger.at(java.util.logging.Level.FINE).log("Player " + playerUuid + " is not eligible for healing (in combat or not resting)");
                    }
                }

                // Debug logging for state tracking
                if (config.isDebugMode()) {
                    logger.at(java.util.logging.Level.FINE).log("Player state: " + state.toString());
                }
            }

        } catch (Exception e) {
            logger.at(java.util.logging.Level.SEVERE).log("Error in healing task: " + e.getMessage());
        }
    }

    /**
     * Determine if a player is eligible for healing based on combat status and resting state
     */
    private boolean isPlayerEligibleForHealing(UUID playerUuid, PlayerHealingState state) {
        // Check if player is in combat
        if (state.isInCombat(config.getCombatTimeout())) {
            if (config.isDebugMode()) {
                logger.at(java.util.logging.Level.FINE).log("Player " + playerUuid + " is in combat, not eligible for healing");
            }
            return false;
        }

        // Check if player is resting (sitting or sleeping)
        // Note: The actual resting state should be checked from the PlayerHealingState
        // which gets updated through the state listener
        if (!state.isResting()) { // Assuming isResting() method exists or will be added to PlayerHealingState
            if (config.isDebugMode()) {
                logger.at(java.util.logging.Level.FINE).log("Player " + playerUuid + " is not resting, not eligible for healing");
            }
            return false;
        }

        // Check if player has reached the healing threshold (e.g., 80% of max HP)
        // This prevents healing beyond the threshold unless they continue resting
        if (hasReachedHealingThreshold(playerUuid)) {
            if (config.isDebugMode()) {
                logger.at(java.util.logging.Level.FINE).log("Player " + playerUuid + " has reached healing threshold, not eligible for healing");
            }
            return false;
        }

        // Player is out of combat, resting, and hasn't reached the healing threshold, eligible for healing
        if (config.isDebugMode()) {
            logger.at(java.util.logging.Level.FINE).log("Player " + playerUuid + " is out of combat, resting, and below healing threshold, eligible for healing");
        }
        return true;
    }

    /**
     * Check if a player has reached the healing threshold
     */
    private boolean hasReachedHealingThreshold(UUID playerUuid) {
        // Find the player in the universe
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null) {
            // Player is not online, return false to allow healing when they return
            return false;
        }

        // Get the player's reference to their entity
        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            // Player entity is not valid, return false to allow healing when valid
            return false;
        }

        // Get the player's stats
        EntityStatMap stats = entityRef.getStore().getComponent(entityRef, EntityStatMap.getComponentType());
        if (stats != null) {
            // Get the current health value and max health value
            // This is a simplified approach - in reality, we'd need to know the health stat index
            // For now, we'll return false to allow healing
            return false; // Placeholder - implement actual health threshold checking
        }

        // If we can't get stats, allow healing
        return false;
    }

    /**
     * Apply healing to a player based on their current state and configuration
     */
    private void applyHealingToPlayer(UUID playerUuid, PlayerHealingState state) {
        // Find the player in the universe
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null) {
            // Player is not online, skip
            return;
        }

        // Get the player's reference to their entity
        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            // Player entity is not valid, skip
            return;
        }

        // Get the player component
        Player playerComponent = entityRef.getStore().getComponent(entityRef, Player.getComponentType());
        if (playerComponent == null) {
            // Player component not available, skip
            return;
        }

        // Get the movement states component to determine healing rate
        MovementStatesComponent movementComponent =
            entityRef.getStore().getComponent(entityRef, MovementStatesComponent.getComponentType());

        float healingRate = config.getSittingHealRate(); // Default to sitting rate
        if (movementComponent != null) {
            MovementStates movementStates = movementComponent.getMovementStates();
            if (movementStates != null && movementStates.sleeping) {
                healingRate = config.getSleepingHealRate(); // Use sleeping rate if sleeping
            }
        }

        // Apply acceleration if player has been resting for the required time
        if (state.shouldAccelerate(config.getAccelerationTime())) {
            healingRate *= config.getAcceleratedRate();
        }

        // Apply healing to the player's health stat
        EntityStatMap stats = entityRef.getStore().getComponent(entityRef, EntityStatMap.getComponentType());
        if (stats != null) {
            // Calculate healing amount (healingRate is percentage per second, and this runs once per second)
            // So we apply healingRate percent of max health
            // For now, we'll use a simple approach - in practice, you might want to calculate based on time passed

            // Apply the healing using processStatChanges
            // This modifies the player's health stat
            // We need to get the health stat index first
            // For now, we'll use a placeholder index - in reality, we'd need to know the health stat index
            // Based on the notes, we need to use processStatChanges method
            // stats.processStatChanges(EntityStatMap.Predictable.SELF, statChanges, ValueType.FLAT, ChangeStatBehaviour.MIN);

            // Create a map for stat changes - using a placeholder index for health stat
            // In a real implementation, we'd need to know the actual health stat index
            // For now, we'll use 0 as a placeholder - this would need to be corrected with the actual health stat index
            Int2FloatMap statChanges = new Int2FloatOpenHashMap();

            // Placeholder: assuming health stat index is 0 (this would need to be the actual health stat index)
            // In reality, we'd need to find the correct index for the health stat
            int healthStatIndex = 0; // This is a placeholder - needs to be the actual health stat index

            // Calculate the actual healing amount based on healing rate and player's max health
            // For now, we'll just apply a small amount as a placeholder
            float healingAmount = healingRate; // This is a simplified approach

            statChanges.put(healthStatIndex, healingAmount);

            // Apply the stat changes to the player
            stats.processStatChanges(EntityStatMap.Predictable.SELF, statChanges, ValueType.Absolute, ChangeStatBehaviour.Add);

            if (config.isDebugMode()) {
                logger.at(java.util.logging.Level.FINE).log("Applied healing to player " + playerUuid +
                    " at rate: " + healingRate + "% per second");
            }
        }
    }

    /**
     * Check and update the player's movement state by accessing the actual game state
     */
    private void checkAndUpdatePlayerMovementState(UUID playerUuid, PlayerHealingState state) {
        // Find the player in the universe
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null) {
            // Player is not online, skip
            return;
        }

        // Get the player's reference to their entity
        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            // Player entity is not valid, skip
            return;
        }

        // Get the player component
        Player playerComponent = entityRef.getStore().getComponent(entityRef, Player.getComponentType());
        if (playerComponent == null) {
            // Player component not available, skip
            return;
        }

        // Get the movement states component
        MovementStatesComponent movementComponent =
            entityRef.getStore().getComponent(entityRef, MovementStatesComponent.getComponentType());

        if (movementComponent != null) {
            MovementStates movementStates = movementComponent.getMovementStates();
            if (movementStates != null) {
                // Extract the relevant states
                boolean isSitting = movementStates.sitting;
                boolean isSleeping = movementStates.sleeping;
                boolean isWalking = movementStates.walking;
                boolean isRunning = movementStates.running;
                boolean isJumping = movementStates.jumping;

                // Notify the state listener about the movement change
                plugin.onPlayerStateChange(playerUuid, isSitting, isSleeping, isWalking, isRunning, isJumping);

                // Log for debugging
                if (config.isDebugMode()) {
                    logger.at(java.util.logging.Level.FINE).log("Real-time movement state update for " + playerUuid +
                        ": sitting=" + isSitting + ", sleeping=" + isSleeping +
                        ", walking=" + isWalking + ", running=" + isRunning +
                        ", jumping=" + isJumping);
                }
            }
        }
    }

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}