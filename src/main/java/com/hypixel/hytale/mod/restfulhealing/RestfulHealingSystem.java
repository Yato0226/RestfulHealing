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
    private final Map<UUID, PlayerHealingState> healingStates;

    private final ComponentType<EntityStore, UUIDComponent> uuidType;
    private final ComponentType<EntityStore, EntityStatMap> statMapType;
    private final ComponentType<EntityStore, MovementStatesComponent> moveType;
    private final ComponentType<EntityStore, DamageDataComponent> damageType;
    private final Query<EntityStore> query;

    private int tickCounter = 0;

    public RestfulHealingSystem(RestfulHealingPlugin plugin, HealingConfig config, Map<UUID, PlayerHealingState> healingStates) {
        this.plugin = plugin;
        this.healingStates = healingStates;

        this.uuidType = UUIDComponent.getComponentType();
        this.statMapType = EntityStatMap.getComponentType();
        this.moveType = MovementStatesComponent.getComponentType();
        this.damageType = DamageDataComponent.getComponentType();

        this.query = Query.and(Player.getComponentType(), uuidType, statMapType, moveType, damageType);
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        HealingConfig currentConfig = plugin.getConfig();

        if (!currentConfig.isEnabled()) return;

        UUIDComponent uuidComp = archetypeChunk.getComponent(index, uuidType);
        EntityStatMap statMap = archetypeChunk.getComponent(index, statMapType);

        if (uuidComp == null || statMap == null) return;

        UUID playerUuid = uuidComp.getUuid();
        PlayerHealingState state = healingStates.get(playerUuid);
        if (state == null) return;

        MovementStatesComponent moveComp = archetypeChunk.getComponent(index, moveType);
        MovementStates moves = moveComp.getMovementStates();

        if (moves == null) {
            state.stopResting();
            return;
        }

        boolean isSitting = moves.sitting;
        boolean isSleeping = moves.sleeping;
        boolean isResting = isSitting || isSleeping;

        DamageDataComponent damageComp = archetypeChunk.getComponent(index, damageType);
        boolean inCombat = false;

        if (damageComp != null) {
            Instant lastAction = damageComp.getLastCombatAction();
            Instant lastDamage = damageComp.getLastDamageTime();
            Instant mostRecent = lastAction.isAfter(lastDamage) ? lastAction : lastDamage;

            if (!mostRecent.equals(Instant.MIN)) {
                long timeSinceCombat = System.currentTimeMillis() - mostRecent.toEpochMilli();
                if (timeSinceCombat < currentConfig.getCombatTimeout()) {
                    inCombat = true;
                }
            }
        }

        if (!isResting || inCombat) {
            state.stopResting();
            return;
        }

        state.startResting();

        int healthIndex = DefaultEntityStatTypes.getHealth();
        EntityStatValue healthStat = statMap.get(healthIndex);
        if (healthStat == null) return;

        float currentHealth = healthStat.get();
        float maxHealth = healthStat.getMax();
        float threshold = maxHealth * currentConfig.getHealThreshold();

        if (currentHealth >= threshold) {
            return;
        }

        float baseRate = isSleeping ? currentConfig.getSleepingHealRate() : currentConfig.getSittingHealRate();

        long durationMs = state.getRestDuration();
        float multiplier = 1.0f;
        if (durationMs > currentConfig.getAccelerationTime()) {
            float rampProgress = (float)(durationMs - currentConfig.getAccelerationTime()) / 5000f;
            multiplier = 1.0f + (currentConfig.getAcceleratedRate() - 1.0f) * Math.min(1.0f, rampProgress);
        }

        float healPerSecond = baseRate * multiplier;
        float healPercentTick = healPerSecond * dt;
        float healAmountTick = maxHealth * (healPercentTick / 100.0f);

        if (currentConfig.isDebugMode()) {
            tickCounter++;

            if (tickCounter % 100 == 0) {
                plugin.getLogger().at(java.util.logging.Level.INFO).log(
                    String.format("[DEBUG] RestfulHealing System - Tracking %d players. TPS Delta: %.4f",
                    healingStates.size(), dt)
                );
            }

            if (tickCounter % 20 == 0) {
                plugin.getLogger().at(java.util.logging.Level.INFO).log(
                    String.format("[DEBUG] Healing %s: %s | Rate: %.1f%% | Duration: %ds | HP: %.1f/%.1f",
                    playerUuid.toString().substring(0, 8),
                    isSleeping ? "Sleep" : "Sit",
                    healPerSecond,
                    (int)(durationMs / 1000),
                    currentHealth,
                    maxHealth)
                );
            }
        }

        state.addAccumulator(healAmountTick);

        if (state.getAccumulator() >= 1.0f) {
            float amountToApply = state.getAccumulator();

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
