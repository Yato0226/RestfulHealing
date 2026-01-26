package com.hypixel.hytale.mod.restfulhealing;

public class PlayerHealingState {
    private long lastCombatTime;
    private long restStartTime;
    private boolean isHealing;
    private float healingProgress;
    private boolean wasSitting;
    private boolean wasSleeping;
    private long lastStateCheck;
    
    public PlayerHealingState() {
        this.lastCombatTime = 0;
        this.restStartTime = 0;
        this.isHealing = false;
        this.healingProgress = 0.0f;
        this.wasSitting = false;
        this.wasSleeping = false;
        this.lastStateCheck = System.currentTimeMillis();
    }
    
    // Combat management
    public void updateCombatTime() {
        this.lastCombatTime = System.currentTimeMillis();
        this.isHealing = false;
        this.restStartTime = 0;
        this.healingProgress = 0.0f;
    }
    
    public boolean isInCombat(long combatTimeout) {
        return System.currentTimeMillis() - lastCombatTime < combatTimeout;
    }
    
    // Healing state management
    public void startResting() {
        if (!isHealing) {
            this.restStartTime = System.currentTimeMillis();
            this.isHealing = true;
            this.healingProgress = 0.0f;
        }
    }
    
    public void stopResting() {
        this.isHealing = false;
        this.restStartTime = 0;
        this.healingProgress = 0.0f;
    }
    
    // Time calculations
    public long getRestDuration() {
        return isHealing ? System.currentTimeMillis() - restStartTime : 0;
    }
    
    public boolean shouldAccelerate(int accelerationTime) {
        return isHealing && (System.currentTimeMillis() - restStartTime) >= accelerationTime;
    }
    
    // State change tracking
    public void updateMovementState(boolean isSitting, boolean isSleeping) {
        this.wasSitting = isSitting;
        this.wasSleeping = isSleeping;
        this.lastStateCheck = System.currentTimeMillis();
    }
    
    public boolean isResting() {
        return wasSitting || wasSleeping;
    }
    
    // Getters and setters
    public long getLastCombatTime() { return lastCombatTime; }
    
    public void setLastCombatTime(long lastCombatTime) { this.lastCombatTime = lastCombatTime; }
    
    public long getRestStartTime() { return restStartTime; }
    
    public void setRestStartTime(long restStartTime) { this.restStartTime = restStartTime; }
    
    public boolean isHealing() { return isHealing; }
    
    public void setHealing(boolean healing) { isHealing = healing; }
    
    public float getHealingProgress() { return healingProgress; }
    
    public void setHealingProgress(float healingProgress) { 
        this.healingProgress = Math.max(0.0f, Math.min(1.0f, healingProgress)); 
    }
    
    public boolean wasSitting() { return wasSitting; }
    
    public void setWasSitting(boolean wasSitting) { this.wasSitting = wasSitting; }
    
    public boolean wasSleeping() { return wasSleeping; }
    
    public void setWasSleeping(boolean wasSleeping) { this.wasSleeping = wasSleeping; }
    
    public long getLastStateCheck() { return lastStateCheck; }
    
    public void setLastStateCheck(long lastStateCheck) { this.lastStateCheck = lastStateCheck; }
    
    @Override
    public String toString() {
        return "PlayerHealingState{" +
                "lastCombatTime=" + lastCombatTime +
                ", restStartTime=" + restStartTime +
                ", isHealing=" + isHealing +
                ", healingProgress=" + healingProgress +
                ", wasSitting=" + wasSitting +
                ", wasSleeping=" + wasSleeping +
                ", lastStateCheck=" + lastStateCheck +
                '}';
    }
}