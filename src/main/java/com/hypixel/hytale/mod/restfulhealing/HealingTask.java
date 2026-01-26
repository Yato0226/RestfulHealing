package com.hypixel.hytale.mod.restfulhealing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;
import java.util.UUID;

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
            // For Phase 2, we'll check the actual movement states from the game
            if (config.isDebugMode()) {
                logger.info("Healing task running for " + healingStates.size() + " players");
            }

            // Iterate through tracked players and check their actual movement states
            for (Map.Entry<UUID, PlayerHealingState> entry : healingStates.entrySet()) {
                UUID playerUuid = entry.getKey();
                PlayerHealingState state = entry.getValue();

                // Get the player's actual movement state from the game
                checkAndUpdatePlayerMovementState(playerUuid, state);

                // Debug logging for state tracking
                if (config.isDebugMode()) {
                    logger.debug("Player state: " + state.toString());
                }
            }

        } catch (Exception e) {
            logger.error("Error in healing task: " + e.getMessage(), e);
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
                    logger.debug("Real-time movement state update for " + playerUuid +
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