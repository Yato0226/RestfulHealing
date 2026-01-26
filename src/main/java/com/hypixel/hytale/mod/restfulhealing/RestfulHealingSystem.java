package com.hypixel.hytale.mod.restfulhealing;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.ChangeStatBehaviour;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.protocol.ValueType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.damage.DamageDataComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class RestfulHealingSystem extends EntityTickingSystem<EntityStore> {

    private final RestfulHealingPlugin plugin;
    private final HealingConfig config;
    private final Map<UUID, PlayerHealingState> healingStates;
    private final ComponentType<EntityStore, Player> playerComponentType;
    private final ComponentType<EntityStore, UUIDComponent> uuidComponentType;
    private final ComponentType<EntityStore, EntityStatMap> statMapComponentType;
    private final ComponentType<EntityStore, MovementStatesComponent> movementStatesComponentType;
    private final ComponentType<EntityStore, DamageDataComponent> damageDataComponentType;
    private final Query<EntityStore> query;

    public RestfulHealingSystem(RestfulHealingPlugin plugin, HealingConfig config, Map<UUID, PlayerHealingState> healingStates) {
        this.plugin = plugin;
        this.config = config;
        this.healingStates = healingStates;
        this.playerComponentType = Player.getComponentType();
        this.uuidComponentType = UUIDComponent.getComponentType();
        this.statMapComponentType = EntityStatMap.getComponentType();
        this.movementStatesComponentType = MovementStatesComponent.getComponentType();
        this.damageDataComponentType = DamageDataComponent.getComponentType();
        
        // We want to process entities that have Player, UUID, EntityStatMap, and DamageData components
        this.query = Query.and(playerComponentType, uuidComponentType, statMapComponentType, damageDataComponentType);
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    @Override
    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return false; 
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (!config.isEnabled()) {
            return;
        }

        UUIDComponent uuidComp = archetypeChunk.getComponent(index, uuidComponentType);
        EntityStatMap statMap = archetypeChunk.getComponent(index, statMapComponentType);
        
        if (uuidComp == null || statMap == null) {
            return;
        }

        UUID playerUuid = uuidComp.getUuid();
        PlayerHealingState state = healingStates.get(playerUuid);
        
        if (state == null) {
            return;
        }

        // Update movement state from component if available
        MovementStatesComponent movementComponent = archetypeChunk.getComponent(index, movementStatesComponentType);
        if (movementComponent != null) {
            MovementStates movementStates = movementComponent.getMovementStates();
            if (movementStates != null) {
                state.updateMovementState(movementStates.sitting, movementStates.sleeping);
                
                // Notify plugin of state change for logging/other logic
                plugin.getStateListener().onMovementStateChange(
                    playerUuid, 
                    movementStates.sitting, 
                    movementStates.sleeping, 
                    movementStates.walking, 
                    movementStates.running, 
                    movementStates.jumping
                );
            }
        }

        // Sync combat status from Hytale's DamageDataComponent
        DamageDataComponent damageData = archetypeChunk.getComponent(index, damageDataComponentType);
        if (damageData != null) {
            Instant lastCombat = damageData.getLastCombatAction();
            Instant lastDamage = damageData.getLastDamageTime();
            
            // Use the most recent of the two
            Instant mostRecentCombat = lastCombat.isAfter(lastDamage) ? lastCombat : lastDamage;
            
            if (!mostRecentCombat.equals(Instant.MIN)) {
                long lastCombatMillis = mostRecentCombat.toEpochMilli();
                if (lastCombatMillis > state.getLastCombatTime()) {
                    state.setLastCombatTime(lastCombatMillis);
                }
            }
        }

        // Check eligibility
        if (state.isInCombat(config.getCombatTimeout()) || !state.isResting()) {
            return;
        }

        // Check health threshold
        int healthStatIndex = DefaultEntityStatTypes.getHealth();
        EntityStatValue healthStat = statMap.get(healthStatIndex);
        if (healthStat == null) {
            return;
        }

        float currentHealth = healthStat.get();
        float maxHealth = healthStat.getMax();
        float threshold = maxHealth * config.getHealThreshold();

        if (currentHealth >= threshold) {
            return;
        }

        // Calculate healing rate
        float healingRate = state.wasSleeping() ? config.getSleepingHealRate() : config.getSittingHealRate();
        
        if (state.shouldAccelerate(config.getAccelerationTime())) {
            healingRate *= config.getAcceleratedRate();
        }

        // Apply healing scaled by dt (delta time in seconds)
        // healingRate is % per second
        float healPercent = healingRate * dt;
        float healAmount = maxHealth * (healPercent / 100.0f);

        // Don't heal past threshold
        if (currentHealth + healAmount > threshold) {
            healAmount = threshold - currentHealth;
        }

        if (healAmount > 0) {
            Int2FloatMap statChanges = new Int2FloatOpenHashMap();
            statChanges.put(healthStatIndex, healAmount);
            
            // Apply stat changes.
            // EntityStatMap.processStatChanges(Predictable predictable, Int2FloatMap entityStats, ValueType valueType, ChangeStatBehaviour changeStatBehaviour)
            statMap.processStatChanges(EntityStatMap.Predictable.SELF, statChanges, ValueType.Absolute, ChangeStatBehaviour.Add);
        }
    }
}
