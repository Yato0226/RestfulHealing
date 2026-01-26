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
            if (config.isDebugMode() && shouldLogPeriodically(10000)) { 
                logger.at(java.util.logging.Level.INFO).log("Healing task running for " + healingStates.size() + " players");
            }

            for (Map.Entry<UUID, PlayerHealingState> entry : healingStates.entrySet()) {
                UUID playerUuid = entry.getKey();
                PlayerHealingState state = entry.getValue();

                checkAndUpdatePlayerMovementState(playerUuid, state);

                if (isPlayerEligibleForHealing(playerUuid, state)) {
                    applyHealingToPlayer(playerUuid, state);

                    if (config.isDebugMode() && shouldLogPlayerEvent(playerUuid, "healing", 5000)) { 
                        logger.at(java.util.logging.Level.FINE).log("Applied healing to player " + playerUuid);
                    }
                } else {
                    if (config.isDebugMode() && shouldLogPlayerEvent(playerUuid, "ineligible", 15000)) { 
                        logger.at(java.util.logging.Level.FINE).log("Player " + playerUuid + " is not eligible for healing (in combat or not resting)");
                    }
                }

                if (config.isDebugMode() && shouldLogPlayerEvent(playerUuid, "state", 30000)) { 
                    logger.at(java.util.logging.Level.FINE).log("Player state: " + state.toString());
                }
            }

        } catch (Exception e) {
            logger.at(java.util.logging.Level.SEVERE).log("Error in healing task: " + e.getMessage());
        }
    }

    private boolean isPlayerEligibleForHealing(UUID playerUuid, PlayerHealingState state) {
        if (state.isInCombat(config.getCombatTimeout())) {
            if (config.isDebugMode()) {
                logger.at(java.util.logging.Level.FINE).log("Player " + playerUuid + " is in combat, not eligible for healing");
            }
            return false;
        }

        if (!state.isResting()) { 
            if (config.isDebugMode()) {
                logger.at(java.util.logging.Level.FINE).log("Player " + playerUuid + " is not resting, not eligible for healing");
            }
            return false;
        }

        if (hasReachedHealingThreshold(playerUuid)) {
            if (config.isDebugMode()) {
                logger.at(java.util.logging.Level.FINE).log("Player " + playerUuid + " has reached healing threshold, not eligible for healing");
            }
            return false;
        }

        if (config.isDebugMode()) {
            logger.at(java.util.logging.Level.FINE).log("Player " + playerUuid + " is out of combat, resting, and below healing threshold, eligible for healing");
        }
        return true;
    }

    private boolean hasReachedHealingThreshold(UUID playerUuid) {
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null) {
            return false;
        }

        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return false;
        }

        EntityStatMap stats = entityRef.getStore().getComponent(entityRef, EntityStatMap.getComponentType());
        if (stats != null) {
            return false; 
        }

        return false;
    }

    private void applyHealingToPlayer(UUID playerUuid, PlayerHealingState state) {
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null) {
            return;
        }

        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Player playerComponent = entityRef.getStore().getComponent(entityRef, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }

        MovementStatesComponent movementComponent =
            entityRef.getStore().getComponent(entityRef, MovementStatesComponent.getComponentType());

        float healingRate = config.getSittingHealRate(); 
        if (movementComponent != null) {
            MovementStates movementStates = movementComponent.getMovementStates();
            if (movementStates != null && movementStates.sleeping) {
                healingRate = config.getSleepingHealRate(); 
            }
        }

        if (state.shouldAccelerate(config.getAccelerationTime())) {
            healingRate *= config.getAcceleratedRate();
        }

        EntityStatMap stats = entityRef.getStore().getComponent(entityRef, EntityStatMap.getComponentType());
        if (stats != null) {


            Int2FloatMap statChanges = new Int2FloatOpenHashMap();

            int healthStatIndex = 0; 

            float healingAmount = healingRate; 

            statChanges.put(healthStatIndex, healingAmount);

            stats.processStatChanges(EntityStatMap.Predictable.SELF, statChanges, ValueType.Absolute, ChangeStatBehaviour.Add);

            if (config.isDebugMode()) {
                logger.at(java.util.logging.Level.FINE).log("Applied healing to player " + playerUuid +
                    " at rate: " + healingRate + "% per second");
            }
        }
    }

    private void checkAndUpdatePlayerMovementState(UUID playerUuid, PlayerHealingState state) {
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null) {
            return;
        }

        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Player playerComponent = entityRef.getStore().getComponent(entityRef, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }

        MovementStatesComponent movementComponent =
            entityRef.getStore().getComponent(entityRef, MovementStatesComponent.getComponentType());

        if (movementComponent != null) {
            MovementStates movementStates = movementComponent.getMovementStates();
            if (movementStates != null) {
                boolean isSitting = movementStates.sitting;
                boolean isSleeping = movementStates.sleeping;
                boolean isWalking = movementStates.walking;
                boolean isRunning = movementStates.running;
                boolean isJumping = movementStates.jumping;

                plugin.onPlayerStateChange(playerUuid, isSitting, isSleeping, isWalking, isRunning, isJumping);

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

    private final Map<String, Long> lastLogTime = new java.util.concurrent.ConcurrentHashMap<>();

    private boolean shouldLogPeriodically(long intervalMs) {
        String key = "general";
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastLogTime.get(key);

        if (lastTime == null || (currentTime - lastTime) >= intervalMs) {
            lastLogTime.put(key, currentTime);
            return true;
        }
        return false;
    }

    private boolean shouldLogPlayerEvent(UUID playerUuid, String eventType, long intervalMs) {
        String key = playerUuid.toString() + "_" + eventType;
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastLogTime.get(key);

        if (lastTime == null || (currentTime - lastTime) >= intervalMs) {
            lastLogTime.put(key, currentTime);
            return true;
        }
        return false;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}