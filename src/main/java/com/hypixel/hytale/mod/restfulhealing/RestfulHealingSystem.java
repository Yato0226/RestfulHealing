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

    // Component Types
    private final ComponentType<EntityStore, UUIDComponent> uuidType;
    private final ComponentType<EntityStore, EntityStatMap> statMapType;
    private final ComponentType<EntityStore, MovementStatesComponent> moveType;
    private final ComponentType<EntityStore, DamageDataComponent> damageType;
    private final Query<EntityStore> query;

    public RestfulHealingSystem(RestfulHealingPlugin plugin, HealingConfig config, Map<UUID, PlayerHealingState> healingStates) {
        this.plugin = plugin;
        this.config = config;
        this.healingStates = healingStates;

        this.uuidType = UUIDComponent.getComponentType();
        this.statMapType = EntityStatMap.getComponentType();
        this.moveType = MovementStatesComponent.getComponentType();
        this.damageType = DamageDataComponent.getComponentType();

        // Only tick entities that have ALL these components (Players)
        this.query = Query.and(Player.getComponentType(), uuidType, statMapType, moveType, damageType);
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (!config.isEnabled()) return;

        // 1. Get Core Components
        UUIDComponent uuidComp = archetypeChunk.getComponent(index, uuidType);
        EntityStatMap statMap = archetypeChunk.getComponent(index, statMapType);

        if (uuidComp == null || statMap == null) return;

        UUID playerUuid = uuidComp.getUuid();
        PlayerHealingState state = healingStates.get(playerUuid);
        if (state == null) return;

        // 2. Check Movement State directly from component
        MovementStatesComponent moveComp = archetypeChunk.getComponent(index, moveType);
        MovementStates moves = moveComp.getMovementStates();

        // If movement states are null or player is invalid, stop here
        if (moves == null) {
            state.stopResting();
            return;
        }

        boolean isSitting = moves.sitting;
        boolean isSleeping = moves.sleeping;
        boolean isResting = isSitting || isSleeping;

        // 3. Check Combat State directly from component
        DamageDataComponent damageComp = archetypeChunk.getComponent(index, damageType);
        boolean inCombat = false;

        if (damageComp != null) {
            // Check both last action (attacking) and last damage (getting hit)
            Instant lastAction = damageComp.getLastCombatAction();
            Instant lastDamage = damageComp.getLastDamageTime();
            Instant mostRecent = lastAction.isAfter(lastDamage) ? lastAction : lastDamage;

            if (!mostRecent.equals(Instant.MIN)) {
                long timeSinceCombat = System.currentTimeMillis() - mostRecent.toEpochMilli();
                if (timeSinceCombat < config.getCombatTimeout()) {
                    inCombat = true;
                }
            }
        }

        // 4. Determine if we should heal
        if (!isResting || inCombat) {
            state.stopResting();
            return;
        }

        // Start tracking time if not already
        state.startResting();

        // 5. Check Health Threshold
        int healthIndex = DefaultEntityStatTypes.getHealth();
        EntityStatValue healthStat = statMap.get(healthIndex);
        if (healthStat == null) return;

        float currentHealth = healthStat.get();
        float maxHealth = healthStat.getMax();
        float threshold = maxHealth * config.getHealThreshold();

        if (currentHealth >= threshold) {
            return; // Don't heal past threshold, but don't reset rest timer either
        }

        // 6. Calculate Healing Amount
        float baseRate = isSleeping ? config.getSleepingHealRate() : config.getSittingHealRate();

        // Acceleration Curve
        long durationMs = state.getRestDuration();
        float multiplier = 1.0f;
        if (durationMs > config.getAccelerationTime()) {
            float rampProgress = (float)(durationMs - config.getAccelerationTime()) / 5000f; // Ramp over 5s
            multiplier = 1.0f + (config.getAcceleratedRate() - 1.0f) * Math.min(1.0f, rampProgress);
        }

        float healPerSecond = baseRate * multiplier; // % per second
        float healPercentTick = healPerSecond * dt;
        float healAmountTick = maxHealth * (healPercentTick / 100.0f);

        // Debug logging (only occasional to prevent spam)
        if (config.isDebugMode() && Math.random() < 0.05) { // Log ~once per second
            plugin.getLogger().at(java.util.logging.Level.FINE).log(String.format("Heal Tick: Rate=%.1f%%, Mult=%.1fx, Amt=%.2f", baseRate, multiplier, healAmountTick));
        }

        // 7. Accumulate and Apply
        state.addAccumulator(healAmountTick);

        // Only apply if we have >= 1 HP stored up (Saves Network/TPS)
        if (state.getAccumulator() >= 1.0f) {
            float amountToApply = state.getAccumulator();

            // Cap at threshold
            if (currentHealth + amountToApply > threshold) {
                amountToApply = threshold - currentHealth;
            }

            if (amountToApply > 0) {
                Int2FloatMap statChanges = new Int2FloatOpenHashMap();
                statChanges.put(healthIndex, amountToApply);
                statMap.processStatChanges(EntityStatMap.Predictable.SELF, statChanges, ValueType.Absolute, ChangeStatBehaviour.Add);
            }

            state.setAccumulator(0.0f);
        }
    }
}
