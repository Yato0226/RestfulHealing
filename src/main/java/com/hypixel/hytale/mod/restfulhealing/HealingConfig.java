package com.hypixel.hytale.mod.restfulhealing;

public class HealingConfig {
    // Healing rates (HP% per second)
    private final float sittingHealRate;        // Default: 1.0% per second
    private final float sleepingHealRate;       // Default: 2.0% per second
    
    // Timing settings (milliseconds)
    private final int combatTimeout;             // Default: 5000ms (5 seconds)
    private final int accelerationTime;          // Default: 10000ms (10 seconds)
    
    // Healing thresholds
    private final float healThreshold;          // Default: 0.8 (80% max HP)
    private final float acceleratedRate;         // Default: 2.0x multiplier
    
    // General settings
    private final boolean enabled;               // Default: true
    private final boolean debugMode;            // Default: false
    
    private HealingConfig(Builder builder) {
        this.sittingHealRate = builder.sittingHealRate;
        this.sleepingHealRate = builder.sleepingHealRate;
        this.combatTimeout = builder.combatTimeout;
        this.accelerationTime = builder.accelerationTime;
        this.healThreshold = builder.healThreshold;
        this.acceleratedRate = builder.acceleratedRate;
        this.enabled = builder.enabled;
        this.debugMode = builder.debugMode;
    }
    
    public static HealingConfig createDefault() {
        return new Builder().build();
    }
    
    // Getters
    public float getSittingHealRate() { return sittingHealRate; }
    public float getSleepingHealRate() { return sleepingHealRate; }
    public int getCombatTimeout() { return combatTimeout; }
    public int getAccelerationTime() { return accelerationTime; }
    public float getHealThreshold() { return healThreshold; }
    public float getAcceleratedRate() { return acceleratedRate; }
    public boolean isEnabled() { return enabled; }
    public boolean isDebugMode() { return debugMode; }
    
    public static class Builder {
        private float sittingHealRate = 1.0f;
        private float sleepingHealRate = 2.0f;
        private int combatTimeout = 5000;
        private int accelerationTime = 10000;
        private float healThreshold = 0.8f;
        private float acceleratedRate = 2.0f;
        private boolean enabled = true;
        private boolean debugMode = true;
        
        public Builder sittingHealRate(float rate) {
            this.sittingHealRate = Math.max(0, rate);
            return this;
        }
        
        public Builder sleepingHealRate(float rate) {
            this.sleepingHealRate = Math.max(0, rate);
            return this;
        }
        
        public Builder combatTimeout(int timeout) {
            this.combatTimeout = Math.max(0, timeout);
            return this;
        }
        
        public Builder accelerationTime(int time) {
            this.accelerationTime = Math.max(0, time);
            return this;
        }
        
        public Builder healThreshold(float threshold) {
            this.healThreshold = Math.max(0, Math.min(1.0f, threshold));
            return this;
        }
        
        public Builder acceleratedRate(float rate) {
            this.acceleratedRate = Math.max(1.0f, rate);
            return this;
        }
        
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public Builder debugMode(boolean debug) {
            this.debugMode = debug;
            return this;
        }
        
        public HealingConfig build() {
            return new HealingConfig(this);
        }
    }
    
    @Override
    public String toString() {
        return "HealingConfig{" +
                "sittingHealRate=" + sittingHealRate +
                ", sleepingHealRate=" + sleepingHealRate +
                ", combatTimeout=" + combatTimeout +
                ", accelerationTime=" + accelerationTime +
                ", healThreshold=" + healThreshold +
                ", acceleratedRate=" + acceleratedRate +
                ", enabled=" + enabled +
                ", debugMode=" + debugMode +
                '}';
    }
}