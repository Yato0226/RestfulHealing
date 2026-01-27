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

    public RestfulHealingPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getLogger().at(java.util.logging.Level.INFO).log("Setting up Restful Healing Mod...");

        this.healingStates = new ConcurrentHashMap<>();

        try {
            this.config = HealingConfig.createDefault();
        } catch (Exception e) {
            getLogger().at(java.util.logging.Level.SEVERE).log("Error loading config, using defaults.");
            this.config = HealingConfig.createDefault();
        }

        getEntityStoreRegistry().registerSystem(new RestfulHealingSystem(this, config, healingStates));

        getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

        getCommandRegistry().registerCommand(new RestfulHealingCommand(this));

        getLogger().at(java.util.logging.Level.INFO).log("Restful Healing Mod enabled.");
    }

    private void onPlayerReady(PlayerReadyEvent event) {
        Ref<EntityStore> ref = event.getPlayerRef();
        if (ref != null && ref.isValid()) {
            UUIDComponent uuidComp = ref.getStore().getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComp != null) {
                healingStates.put(uuidComp.getUuid(), new PlayerHealingState());
            }
        }
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (event.getPlayerRef() != null) {
            healingStates.remove(event.getPlayerRef().getUuid());
        }
    }

    public HealingConfig getConfig() { return config; }

    public void updateConfig(HealingConfig newConfig) {
        this.config = newConfig;
    }
}
