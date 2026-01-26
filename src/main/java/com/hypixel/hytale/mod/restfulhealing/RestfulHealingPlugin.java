package com.hypixel.hytale.mod.restfulhealing;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.task.TaskRegistry;
import com.hypixel.hytale.server.core.task.TaskRegistration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RestfulHealingPlugin extends JavaPlugin {

    private HealingConfig config;
    private Map<UUID, PlayerHealingState> healingStates;
    private HealingTask healingTask;
    private PlayerStateListener stateListener;
    private CombatListener combatListener;
    private TaskRegistration healingTaskRegistration;

    public RestfulHealingPlugin(JavaPluginInit init) {
        super(init);
    }
    
    @Override
    protected void setup() {
        getLogger().info("Setting up Restful Healing Mod...");

        // Initialize healing states tracking
        this.healingStates = new ConcurrentHashMap<>();

        // Load configuration
        loadConfiguration();

        // Initialize state listener with logger
        this.stateListener = new PlayerStateListener(this, getLogger());

        // Initialize combat listener
        this.combatListener = new CombatListener(this);

        // Register events
        registerEvents();

        getLogger().info("Restful Healing Mod setup completed!");
    }

    @Override
    protected void start() {
        getLogger().info("Starting Restful Healing Mod...");

        // Start healing task
        startHealingTask();

        getLogger().info("Restful Healing Mod started successfully!");
    }

    @Override
    protected void shutdown() {
        getLogger().info("Disabling Restful Healing Mod...");

        // Stop healing task
        if (healingTaskRegistration != null) {
            healingTaskRegistration.unregister();
            healingTaskRegistration = null;
        }

        // Clear healing states
        if (healingStates != null) {
            healingStates.clear();
        }

        getLogger().info("Restful Healing Mod disabled!");
    }
    
    private void loadConfiguration() {
        try {
            // For now, use default config - in the future we can load from plugin config
            this.config = HealingConfig.createDefault();
            getLogger().info("Configuration loaded successfully!");
            getLogger().info("Config: " + config.toString());

        } catch (Exception e) {
            getLogger().error("Failed to load configuration, using defaults: " + e.getMessage());
            this.config = HealingConfig.createDefault();
        }
    }
    
    private void startHealingTask() {
        // Create the healing task
        this.healingTask = new HealingTask(this, config, healingStates, getLogger());

        // Schedule the task using the server's task registry
        // Run healing calculation every 1 second
        this.healingTaskRegistration = getTaskRegistry().registerTask(
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        healingTask.run();
                        Thread.sleep(1000); // Sleep for 1 second between checks
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        getLogger().error("Error in healing task: " + e.getMessage(), e);
                    }
                }
            })
        );

        getLogger().info("Healing task started!");
    }
    
    private void registerEvents() {
        // Register for player events
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        getEventRegistry().registerGlobal(PlayerInteractEvent.class, this::onPlayerInteract);

        getLogger().info("Events registered successfully!");
    }

    // Event handlers with state tracking integration
    private void onPlayerReady(PlayerReadyEvent event) {
        UUID playerUuid = event.getPlayer().getUuid();
        healingStates.put(playerUuid, new PlayerHealingState());
        stateListener.onPlayerJoin(playerUuid);
        getLogger().debug("Player joined: " + playerUuid);
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        UUID playerUuid = event.getPlayer().getUuid();
        healingStates.remove(playerUuid);
        stateListener.onPlayerLeave(playerUuid);
        getLogger().debug("Player left: " + playerUuid);
    }
    
    // Combat detection method
    private void onPlayerInteract(PlayerInteractEvent event) {
        // When a player interacts (potentially taking damage), update their combat time
        UUID playerUuid = event.getPlayer().getUuid();
        PlayerHealingState healingState = healingStates.get(playerUuid);
        if (healingState != null) {
            healingState.updateCombatTime();
            getLogger().debug("Player " + playerUuid + " entered combat");
        }
    }

    // State change methods (for testing purposes)
    public void onPlayerStateChange(java.util.UUID playerUuid, boolean isSitting, boolean isSleeping,
                               boolean isWalking, boolean isRunning, boolean isJumping) {
        stateListener.onMovementStateChange(playerUuid, isSitting, isSleeping, isWalking, isRunning, isJumping);
    }

    // Getters for other classes
    public HealingConfig getConfig() {
        return config;
    }

    public Map<UUID, PlayerHealingState> getHealingStates() {
        return healingStates;
    }

    public PlayerStateListener getStateListener() {
        return stateListener;
    }
}