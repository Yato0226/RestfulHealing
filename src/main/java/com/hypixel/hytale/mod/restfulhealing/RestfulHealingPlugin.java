package com.hypixel.hytale.mod.restfulhealing;

// For Phase 1 testing, we'll create a simplified version
// In later phases, we'll extend the actual JavaPlugin from Hytale API
public class RestfulHealingPlugin {
    
    private HealingConfig config;
    private java.util.Map<java.util.UUID, PlayerHealingState> healingStates;
    private HealingTask healingTask;
    private PlayerStateListener stateListener;
    
    public RestfulHealingPlugin() {
        // Constructor for Phase 1
    }
    
    public void initialize() {
        System.out.println("Initializing Restful Healing Mod...");
        
        // Initialize healing states tracking
        this.healingStates = new java.util.concurrent.ConcurrentHashMap<>();
        
        // Load configuration
        loadConfiguration();
        
        // Initialize state listener
        this.stateListener = new PlayerStateListener(this);
        
        // Start healing task
        startHealingTask();
        
        System.out.println("Restful Healing Mod initialized successfully!");
    }
    
    public void shutdown() {
        System.out.println("Disabling Restful Healing Mod...");
        
        // Stop healing task
        if (healingTask != null) {
            healingTask.cancel();
            healingTask = null;
        }
        
        // Clear healing states
        if (healingStates != null) {
            healingStates.clear();
        }
        
        System.out.println("Restful Healing Mod disabled!");
    }
    
    private void loadConfiguration() {
        try {
            // For Phase 1, use default config
            this.config = HealingConfig.createDefault();
            System.out.println("Configuration loaded successfully!");
            System.out.println("Config: " + config.toString());
            
        } catch (Exception e) {
            System.err.println("Failed to load configuration, using defaults: " + e.getMessage());
            this.config = HealingConfig.createDefault();
        }
    }
    
    private void startHealingTask() {
        this.healingTask = new HealingTask(this, config, healingStates);
        // For Phase 1, we'll simulate the task running
        final HealingTask task = this.healingTask; // Copy to avoid null reference
        new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(1000); // Wait 1 second
                    if (!task.isCancelled()) {
                        task.run();
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
        System.out.println("Healing task started!");
    }
    
    // Event handlers with state tracking integration
    public void onPlayerJoin(java.util.UUID playerUuid) {
        healingStates.put(playerUuid, new PlayerHealingState());
        stateListener.onPlayerJoin(playerUuid);
        System.out.println("Player joined: " + playerUuid);
    }
    
    public void onPlayerLeave(java.util.UUID playerUuid) {
        healingStates.remove(playerUuid);
        stateListener.onPlayerLeave(playerUuid);
        System.out.println("Player left: " + playerUuid);
    }
    
    // State change methods
    public void onPlayerStateChange(java.util.UUID playerUuid, boolean isSitting, boolean isSleeping, 
                               boolean isWalking, boolean isRunning, boolean isJumping) {
        stateListener.onMovementStateChange(playerUuid, isSitting, isSleeping, isWalking, isRunning, isJumping);
    }
    
    // Getters for other classes
    public HealingConfig getConfig() {
        return config;
    }
    
    public java.util.Map<java.util.UUID, PlayerHealingState> getHealingStates() {
        return healingStates;
    }
    
    public PlayerStateListener getStateListener() {
        return stateListener;
    }
    
    // Main method for Phase 1 testing
    public static void main(String[] args) {
        RestfulHealingPlugin plugin = new RestfulHealingPlugin();
        plugin.initialize();
        
        // Simulate some players joining
        java.util.UUID player1 = java.util.UUID.randomUUID();
        java.util.UUID player2 = java.util.UUID.randomUUID();
        
        plugin.onPlayerJoin(player1);
        plugin.onPlayerJoin(player2);
        
        // Wait a bit
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        plugin.onPlayerLeave(player1);
        plugin.shutdown();
    }
}