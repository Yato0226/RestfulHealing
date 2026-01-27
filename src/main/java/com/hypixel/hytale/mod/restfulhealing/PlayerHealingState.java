package com.hypixel.hytale.mod.restfulhealing;

public class PlayerHealingState {
    private long restStartTime;
    private float healingAccumulator;

    public PlayerHealingState() {
        this.restStartTime = -1;
        this.healingAccumulator = 0.0f;
    }

    public void startResting() {
        if (this.restStartTime == -1) {
            this.restStartTime = System.currentTimeMillis();
        }
    }

    public void stopResting() {
        this.restStartTime = -1;
        this.healingAccumulator = 0.0f;
    }

    public boolean isResting() {
        return restStartTime != -1;
    }

    public long getRestDuration() {
        return isResting() ? System.currentTimeMillis() - restStartTime : 0;
    }

    public float getAccumulator() {
        return healingAccumulator;
    }

    public void addAccumulator(float amount) {
        this.healingAccumulator += amount;
    }

    public void setAccumulator(float amount) {
        this.healingAccumulator = amount;
    }
}