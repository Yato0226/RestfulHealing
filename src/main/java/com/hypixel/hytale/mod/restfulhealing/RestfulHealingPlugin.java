package com.hypixel.hytale.mod.restfulhealing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RestfulHealingPlugin extends JavaPlugin {

    private HealingConfig config;
    private Map<UUID, PlayerHealingState> healingStates;
    private PlayerStateListener stateListener;
    private CombatListener combatListener;

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

        // Register the ticking system - THIS IS THE CORRECT WAY
        getEntityStoreRegistry().registerSystem(new RestfulHealingSystem(this, config, healingStates));

        registerEvents();

        getLogger().at(java.util.logging.Level.INFO).log("Restful Healing Mod setup completed!");
    }

    @Override
    protected void start() {
        getLogger().at(java.util.logging.Level.INFO).log("Starting Restful Healing Mod...");
    }

    @Override
    protected void shutdown() {
        getLogger().at(java.util.logging.Level.INFO).log("Disabling Restful Healing Mod...");

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
    
    private void registerEvents() {
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

        getLogger().at(java.util.logging.Level.INFO).log("Events registered successfully!");
    }

    private void onPlayerReady(PlayerReadyEvent event) {
        Ref<EntityStore> ref = event.getPlayerRef();
        UUID playerUuid = null;
        if (ref != null && ref.isValid()) {
            UUIDComponent uuidComp = ref.getStore().getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComp != null) {
                playerUuid = uuidComp.getUuid();
            }
        }
        
        if (playerUuid == null) {
            return;
        }

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
