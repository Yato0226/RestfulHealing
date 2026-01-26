package com.hypixel.hytale.mod.restfulhealing;

import java.util.UUID;

public class StateDetectionTest {
    
    public static void main(String[] args) {
        System.out.println("=== Phase 2: State Detection Test ===\n");
        
        // Initialize plugin and state listener
        RestfulHealingPlugin plugin = new RestfulHealingPlugin();
        plugin.initialize();
        
        // Create test players
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        
        // Players join
        System.out.println("1. Players joining:");
        plugin.onPlayerJoin(player1);
        plugin.onPlayerJoin(player2);
        
        // Test basic state changes
        System.out.println("\n2. Testing basic state changes:");
        testPlayerAction(plugin, player1, "sit", "Player 1 sits down");
        testPlayerAction(plugin, player2, "sleep", "Player 2 goes to sleep");
        
        // Test movement cancellation
        System.out.println("\n3. Testing movement cancellation:");
        testPlayerAction(plugin, player1, "walk", "Player 1 starts walking (should stop healing)");
        testPlayerAction(plugin, player1, "sit", "Player 1 sits again");
        
        // Test state transitions
        System.out.println("\n4. Testing state transitions:");
        testPlayerAction(plugin, player1, "stand", "Player 1 stands up");
        testPlayerAction(plugin, player1, "run", "Player 1 starts running");
        testPlayerAction(plugin, player1, "sit", "Player 1 sits again");
        testPlayerAction(plugin, player1, "sleep", "Player 1 transitions to sleep");
        
        // Test jumping/movement from rest
        System.out.println("\n5. Testing movement from rest:");
        testPlayerAction(plugin, player2, "jump", "Player 2 jumps while sleeping (should stop healing)");
        testPlayerAction(plugin, player2, "sleep", "Player 2 goes back to sleep");
        
        // Display final states
        System.out.println("\n6. Final player states:");
        displayPlayerStates(plugin, player1, player2);
        
        // Cleanup
        plugin.onPlayerLeave(player1);
        plugin.onPlayerLeave(player2);
        plugin.shutdown();
        
        System.out.println("\n=== State Detection Test Complete ===");
    }
    
    private static void testPlayerAction(RestfulHealingPlugin plugin, UUID playerUuid, String action, String description) {
        System.out.println("  " + description);
        plugin.getStateListener().simulatePlayerAction(playerUuid, action);
        
        // Brief pause to simulate game timing
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static void displayPlayerStates(RestfulHealingPlugin plugin, UUID player1, UUID player2) {
        PlayerHealingState state1 = plugin.getHealingStates().get(player1);
        PlayerHealingState state2 = plugin.getHealingStates().get(player2);
        
        MovementStateChecker move1 = plugin.getStateListener().getPlayerState(player1);
        MovementStateChecker move2 = plugin.getStateListener().getPlayerState(player2);
        
        System.out.println("  Player 1:");
        System.out.println("    Healing State: " + state1);
        System.out.println("    Movement State: " + move1);
        
        System.out.println("  Player 2:");
        System.out.println("    Healing State: " + state2);
        System.out.println("    Movement State: " + move2);
    }
}