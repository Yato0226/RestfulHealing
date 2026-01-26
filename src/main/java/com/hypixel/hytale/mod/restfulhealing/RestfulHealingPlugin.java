package com.hypixel.hytale.mod.restfulhealing;

import com.hypixel.hytale.server.core.entity.UUIDComponent;
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
        getLogger().at(java.util.logging.Level.INFO).log("Setting up Restful Healing Mod...");

        this.healingStates = new ConcurrentHashMap<>();

        loadConfiguration();

        this.stateListener = new PlayerStateListener(this, getLogger());

        this.combatListener = new CombatListener(this);

        registerEvents();

        getLogger().at(java.util.logging.Level.INFO).log("Restful Healing Mod setup completed!");
    }

    @Override
    protected void start() {
        getLogger().at(java.util.logging.Level.INFO).log("Starting Restful Healing Mod...");

        startHealingTask();

        getLogger().at(java.util.logging.Level.INFO).log("Restful Healing Mod started successfully!");
    }

    @Override
    protected void shutdown() {
        getLogger().at(java.util.logging.Level.INFO).log("Disabling Restful Healing Mod...");

        if (healingTaskRegistration != null) {
            healingTaskRegistration.unregister();
            healingTaskRegistration = null;
        }

        if (healingStates != null) {
            healingStates.clear();
        }

        getLogger().at(java.util.logging.Level.INFO).log("Restful Healing Mod disabled!");
    }
    
    private void loadConfiguration() {
        try {
            this.config = HealingConfig.createDefault();
            getLogger().at(java.util.logging.Level.INFO).log("Configuration loaded successfully!");
            getLogger().at(java.util.logging.Level.INFO).log("Config: " + config.toString());

        } catch (Exception e) {
            getLogger().at(java.util.logging.Level.SEVERE).log("Failed to load configuration, using defaults: " + e.getMessage());
            this.config = HealingConfig.createDefault();
        }
    }
    
    private void startHealingTask() {
        this.healingTask = new HealingTask(this, config, healingStates, getLogger());

        // Schedule task to run on the WorldThread every 20 ticks (1 second)
        this.healingTaskRegistration = getTaskRegistry().registerRepeatingTask(
            healingTask::run,
            20L
        );

        getLogger().at(java.util.logging.Level.INFO).log("Healing task started on main thread!");
    }
    
    private void registerEvents() {
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        getEventRegistry().registerGlobal(PlayerInteractEvent.class, this::onPlayerInteract);

        getLogger().at(java.util.logging.Level.INFO).log("Events registered successfully!");
    }

    private void onPlayerReady(PlayerReadyEvent event) {
        UUID playerUuid = event.getPlayer().getUuid();
        healingStates.put(playerUuid, new PlayerHealingState());
        stateListener.onPlayerJoin(playerUuid);
        getLogger().at(java.util.logging.Level.FINE).log("Player joined: " + playerUuid);
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        UUID playerUuid = event.getPlayerRef().getUuid();
        healingStates.remove(playerUuid);
        stateListener.onPlayerLeave(playerUuid);
        getLogger().at(java.util.logging.Level.FINE).log("Player left: " + playerUuid);
    }

    private void onPlayerInteract(PlayerInteractEvent event) {
        UUID playerUuid = event.getPlayer().getUuid();
        PlayerHealingState healingState = healingStates.get(playerUuid);
        if (healingState != null) {
            healingState.updateCombatTime();
            getLogger().at(java.util.logging.Level.FINE).log("Player " + playerUuid + " entered combat");
        }
    }

    public void onPlayerStateChange(java.util.UUID playerUuid, boolean isSitting, boolean isSleeping,
                               boolean isWalking, boolean isRunning, boolean isJumping) {
        stateListener.onMovementStateChange(playerUuid, isSitting, isSleeping, isWalking, isRunning, isJumping);
    }

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