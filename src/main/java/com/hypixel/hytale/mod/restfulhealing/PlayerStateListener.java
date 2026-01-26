package com.hypixel.hytale.mod.restfulhealing;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// PlayerStateListener monitors and handles player state changes
// In production, this will listen to actual Hytale movement events
public class PlayerStateListener {
    
    private final RestfulHealingPlugin plugin;
    private final Map<UUID, MovementStateChecker> playerStates;
    private final Map<UUID, Long> lastUpdateTimes;
    
    public PlayerStateListener(RestfulHealingPlugin plugin) {
        this.plugin = plugin;
        this.playerStates = new ConcurrentHashMap<>();
        this.lastUpdateTimes = new ConcurrentHashMap<>();
    }
    
    public void onPlayerJoin(UUID playerUuid) {
        // Initialize player state
        MovementStateChecker initialState = new MovementStateChecker();
        playerStates.put(playerUuid, initialState);
        lastUpdateTimes.put(playerUuid, System.currentTimeMillis());
        
        System.out.println("PlayerStateListener: Player " + playerUuid + " joined with initial state: " + initialState);
    }
    
    public void onPlayerLeave(UUID playerUuid) {
        // Clean up player state
        playerStates.remove(playerUuid);
        lastUpdateTimes.remove(playerUuid);
        
        System.out.println("PlayerStateListener: Player " + playerUuid + " left");
    }
    
    public void onMovementStateChange(UUID playerUuid, boolean isSitting, boolean isSleeping, 
                                  boolean isWalking, boolean isRunning, boolean isJumping) {
        MovementStateChecker currentState = playerStates.get(playerUuid);
        if (currentState == null) {
            // Player not tracked yet, initialize
            currentState = new MovementStateChecker(isSitting, isSleeping);
            playerStates.put(playerUuid, currentState);
        }
        
        // Create new state for comparison
        MovementStateChecker newState = new MovementStateChecker();
        newState.setSitting(isSitting);
        newState.setSleeping(isSleeping);
        newState.setWalking(isWalking);
        newState.setRunning(isRunning);
        newState.setJumping(isJumping);
        
        // Check if state actually changed
        if (currentState.hasStateChanged(newState)) {
            handleStateChange(playerUuid, currentState, newState);
            
            // Update stored state
            currentState.setSitting(isSitting);
            currentState.setSleeping(isSleeping);
            currentState.setWalking(isWalking);
            currentState.setRunning(isRunning);
            currentState.setJumping(isJumping);
        }
        
        lastUpdateTimes.put(playerUuid, System.currentTimeMillis());
    }
    
    private void handleStateChange(UUID playerUuid, MovementStateChecker oldState, MovementStateChecker newState) {
        PlayerHealingState healingState = plugin.getHealingStates().get(playerUuid);
        if (healingState == null) {
            return; // Player not initialized yet
        }
        
        HealingConfig config = plugin.getConfig();
        
        System.out.println("PlayerStateListener: State change for " + playerUuid);
        System.out.println("  Old: " + oldState);
        System.out.println("  New: " + newState);
        
        // Handle starting to rest
        if (newState.isResting() && !oldState.isResting()) {
            healingState.startResting();
            healingState.updateMovementState(newState.isSitting(), newState.isSleeping());
            System.out.println("Player " + playerUuid + " started resting");
        }
        
        // Handle stopping rest due to movement
        else if (oldState.isResting() && newState.isMoving()) {
            healingState.stopResting();
            healingState.updateMovementState(false, false);
            System.out.println("Player " + playerUuid + " stopped resting (movement detected)");
        }
        
        // Handle state change while resting
        else if (oldState.isResting() && newState.isResting()) {
            healingState.updateMovementState(newState.isSitting(), newState.isSleeping());
            System.out.println("Player " + playerUuid + " changed resting state: " + 
                (newState.isSleeping() ? "Sleeping" : "Sitting"));
        }
        
        // Handle stopping rest due to standing up
        else if (oldState.isResting() && !newState.isResting()) {
            healingState.stopResting();
            healingState.updateMovementState(false, false);
            System.out.println("Player " + playerUuid + " stopped resting (stood up)");
        }
        
        // Always update movement state
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
    
    // Utility method to simulate state changes for testing
    public void simulatePlayerAction(UUID playerUuid, String action) {
        MovementStateChecker currentState = playerStates.get(playerUuid);
        if (currentState == null) {
            System.err.println("Cannot simulate action for untracked player: " + playerUuid);
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
                System.err.println("Unknown action: " + action);
        }
    }
}