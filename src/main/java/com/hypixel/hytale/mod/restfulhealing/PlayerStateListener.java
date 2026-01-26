package com.hypixel.hytale.mod.restfulhealing;

import com.hypixel.hytale.logger.HytaleLogger;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStateListener {

    private final RestfulHealingPlugin plugin;
    private final Map<UUID, MovementStateChecker> playerStates;
    private final Map<UUID, Long> lastUpdateTimes;
    private final HytaleLogger logger;

    public PlayerStateListener(RestfulHealingPlugin plugin, HytaleLogger logger) {
        this.plugin = plugin;
        this.playerStates = new ConcurrentHashMap<>();
        this.lastUpdateTimes = new ConcurrentHashMap<>();
        this.logger = logger;
    }

    public void onPlayerJoin(UUID playerUuid) {
        MovementStateChecker initialState = new MovementStateChecker();
        playerStates.put(playerUuid, initialState);
        lastUpdateTimes.put(playerUuid, System.currentTimeMillis());

        if (logger != null) {
            logger.at(java.util.logging.Level.FINE).log("PlayerStateListener: Player " + playerUuid + " joined with initial state: " + initialState);
        }
    }

    public void onPlayerLeave(UUID playerUuid) {
        playerStates.remove(playerUuid);
        lastUpdateTimes.remove(playerUuid);

        if (logger != null) {
            logger.at(java.util.logging.Level.FINE).log("PlayerStateListener: Player " + playerUuid + " left");
        }
    }
    
    public void onMovementStateChange(UUID playerUuid, boolean isSitting, boolean isSleeping,
                                  boolean isWalking, boolean isRunning, boolean isJumping) {
        MovementStateChecker currentState = playerStates.get(playerUuid);
        if (currentState == null) {
            currentState = new MovementStateChecker(isSitting, isSleeping);
            playerStates.put(playerUuid, currentState);
        }

        MovementStateChecker newState = new MovementStateChecker();
        newState.setSitting(isSitting);
        newState.setSleeping(isSleeping);
        newState.setWalking(isWalking);
        newState.setRunning(isRunning);
        newState.setJumping(isJumping);

        if (currentState.hasStateChanged(newState)) {
            handleStateChange(playerUuid, currentState, newState);

            currentState.setSitting(isSitting);
            currentState.setSleeping(isSleeping);
            currentState.setWalking(isWalking);
            currentState.setRunning(isRunning);
            currentState.setJumping(isJumping);
        }

        lastUpdateTimes.put(playerUuid, System.currentTimeMillis());
    }

    private final Map<UUID, Long> lastStateChangeLogTime = new java.util.concurrent.ConcurrentHashMap<>();

    private void handleStateChange(UUID playerUuid, MovementStateChecker oldState, MovementStateChecker newState) {
        PlayerHealingState healingState = plugin.getHealingStates().get(playerUuid);
        if (healingState == null) {
            return; 
        }

        HealingConfig config = plugin.getConfig();

        if (logger != null && config.isDebugMode()) {
            long currentTime = System.currentTimeMillis();
            Long lastLogTime = lastStateChangeLogTime.get(playerUuid);

            if (lastLogTime == null || (currentTime - lastLogTime) >= 5000) {
                logger.at(java.util.logging.Level.FINE).log("PlayerStateListener: State change for " + playerUuid);
                logger.at(java.util.logging.Level.FINE).log("  Old: " + oldState);
                logger.at(java.util.logging.Level.FINE).log("  New: " + newState);
                lastStateChangeLogTime.put(playerUuid, currentTime);
            }
        }

        if (newState.isResting() && !oldState.isResting()) {
            healingState.startResting();
            healingState.updateMovementState(newState.isSitting(), newState.isSleeping());
            if (logger != null) {
                logger.at(java.util.logging.Level.FINE).log("Player " + playerUuid + " started resting");
            }
        }

        else if (oldState.isResting() && newState.isMoving()) {
            healingState.stopResting();
            healingState.updateMovementState(false, false);
            if (logger != null) {
                logger.at(java.util.logging.Level.FINE).log("Player " + playerUuid + " stopped resting (movement detected)");
            }
        }

        else if (oldState.isResting() && newState.isResting()) {
            healingState.updateMovementState(newState.isSitting(), newState.isSleeping());
            if (logger != null) {
                logger.at(java.util.logging.Level.FINE).log("Player " + playerUuid + " changed resting state: " +
                    (newState.isSleeping() ? "Sleeping" : "Sitting"));
            }
        }

        else if (oldState.isResting() && !newState.isResting()) {
            healingState.stopResting();
            healingState.updateMovementState(false, false);
            if (logger != null) {
                logger.at(java.util.logging.Level.FINE).log("Player " + playerUuid + " stopped resting (stood up)");
            }
        }

        healingState.updateMovementState(newState.isSitting(), newState.isSleeping());
    }

    public MovementStateChecker getPlayerState(UUID playerUuid) {
        return playerStates.get(playerUuid);
    }

    public boolean isPlayerTracked(UUID playerUuid) {
        return playerStates.containsKey(playerUuid);
    }

    public int getTrackedPlayerCount() {
        return playerStates.size();
    }

    public void simulatePlayerAction(UUID playerUuid, String action) {
        MovementStateChecker currentState = playerStates.get(playerUuid);
        if (currentState == null) {
            logger.at(java.util.logging.Level.SEVERE).log("Cannot simulate action for untracked player: " + playerUuid);
            return;
        }

        switch (action.toLowerCase()) {
            case "sit":
                onMovementStateChange(playerUuid, true, false, false, false, false);
                break;
            case "sleep":
                onMovementStateChange(playerUuid, false, true, false, false, false);
                break;
            case "stand":
                onMovementStateChange(playerUuid, false, false, false, false, false);
                break;
            case "walk":
                onMovementStateChange(playerUuid, false, false, true, false, false);
                break;
            case "run":
                onMovementStateChange(playerUuid, false, false, false, true, false);
                break;
            case "jump":
                onMovementStateChange(playerUuid, false, false, false, false, true);
                break;
            default:
                logger.at(java.util.logging.Level.SEVERE).log("Unknown action: " + action);
        }
    }
}