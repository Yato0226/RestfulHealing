package com.hypixel.hytale.mod.restfulhealing;

public class MovementStateChecker {
    
    private boolean idle;
    private boolean horizontalIdle;
    private boolean jumping;
    private boolean flying;
    private boolean walking;
    private boolean running;
    private boolean sprinting;
    private boolean crouching;
    private boolean forcedCrouching;
    private boolean falling;
    private boolean climbing;
    private boolean inFluid;
    private boolean swimming;
    private boolean swimJumping;
    private boolean onGround;
    private boolean mantling;
    private boolean sliding;
    private boolean mounting;
    private boolean rolling;
    private boolean sitting;
    private boolean gliding;
    private boolean sleeping;
    
    public MovementStateChecker() {
        this.idle = true;
        this.sitting = false;
        this.sleeping = false;
        this.onGround = true;
        this.walking = false;
        this.running = false;
    }
    
    public MovementStateChecker(boolean sitting, boolean sleeping) {
        this();
        this.sitting = sitting;
        this.sleeping = sleeping;
        if (sitting || sleeping) {
            this.idle = true;
            this.walking = false;
            this.running = false;
        }
    }
    
    public boolean isResting() {
        return sitting || sleeping;
    }
    
    public boolean isSitting() {
        return sitting;
    }
    
    public boolean isSleeping() {
        return sleeping;
    }
    
    public boolean isMoving() {
        return walking || running || sprinting || jumping || swimming || 
               climbing || mantling || sliding || rolling || swimJumping;
    }
    
    public boolean isStationary() {
        return !isMoving();
    }
    
    public boolean isOnGround() {
        return onGround;
    }
    
    public void setSitting(boolean sitting) {
        this.sitting = sitting;
        if (sitting) {
            this.idle = true;
            this.walking = false;
            this.running = false;
            this.sleeping = false;
        }
    }
    
    public void setSleeping(boolean sleeping) {
        this.sleeping = sleeping;
        if (sleeping) {
            this.idle = true;
            this.walking = false;
            this.running = false;
            this.sitting = false;
        }
    }
    
    public void setWalking(boolean walking) {
        this.walking = walking;
        if (walking) {
            this.idle = false;
            this.sitting = false;
            this.sleeping = false;
        }
    }
    
    public void setRunning(boolean running) {
        this.running = running;
        if (running) {
            this.idle = false;
            this.sitting = false;
            this.sleeping = false;
            this.walking = false;
        }
    }
    
    public void setJumping(boolean jumping) {
        this.jumping = jumping;
        if (jumping) {
            this.idle = false;
        }
    }
    
    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }
    
    public boolean hasStateChanged(MovementStateChecker other) {
        return this.sitting != other.sitting || 
               this.sleeping != other.sleeping ||
               this.walking != other.walking ||
               this.running != other.running ||
               this.jumping != other.jumping ||
               this.idle != other.idle;
    }
    
    @Override
    public String toString() {
        return "MovementStateChecker{" +
                "idle=" + idle +
                ", sitting=" + sitting +
                ", sleeping=" + sleeping +
                ", walking=" + walking +
                ", running=" + running +
                ", jumping=" + jumping +
                ", onGround=" + onGround +
                ", isMoving=" + isMoving() +
                ", isResting=" + isResting() +
                '}';
    }
}