package com.hypixel.hytale.mod.restfulhealing;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class HealingTask implements Runnable {
    
    private final RestfulHealingPlugin plugin;
    private final HealingConfig config;
    private final Map<UUID, PlayerHealingState> healingStates;
    private volatile boolean cancelled = false;
    
    public HealingTask(RestfulHealingPlugin plugin, HealingConfig config, Map<UUID, PlayerHealingState> healingStates) {
        this.plugin = plugin;
        this.config = config;
        this.healingStates = healingStates;
    }
    
    @Override
    public void run() {
        if (cancelled || !config.isEnabled()) {
            return;
        }
        
        try {
            // For Phase 1, we'll just log the task is running
            // In later phases, we'll implement the actual healing logic here
            if (config.isDebugMode()) {
                Logger.getLogger("RestfulHealing").info("Healing task running for " + healingStates.size() + " players");
            }
            
            // Placeholder for healing logic - will be implemented in Phase 4
            for (Map.Entry<UUID, PlayerHealingState> entry : healingStates.entrySet()) {
                UUID playerUuid = entry.getKey();
                PlayerHealingState state = entry.getValue();
                
                // Debug logging for state tracking
                if (config.isDebugMode()) {
                    Logger.getLogger("RestfulHealing").fine("Player state: " + state.toString());
                }
            }
            
        } catch (Exception e) {
            Logger.getLogger("RestfulHealing").severe("Error in healing task: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void cancel() {
        this.cancelled = true;
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
}