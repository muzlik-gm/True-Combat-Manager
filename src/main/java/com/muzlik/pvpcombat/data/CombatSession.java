package com.muzlik.pvpcombat.data;

import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents an active combat session between two players.
 */
public class CombatSession {
    private final UUID sessionId;
    private final Player attacker;
    private final Player defender;
    private final long startTime;
    private final AtomicLong lastActivityTime;
    private final AtomicBoolean active;
    private int timerSeconds;
    private BossBar bossBar;
    private CombatState state;
    private TimerData timerData;
    private String currentTheme;
    private boolean visualsEnabled;
    private InterferenceData interferenceData;
    
    // Session-specific damage tracking
    private double attackerDamageDealt;
    private double defenderDamageDealt;
    
    // Session-specific hit tracking
    private int attackerHitsLanded;
    private int defenderHitsLanded;
    
    // Combat end tracking
    private CombatEndReason endReason;
    private long endTime;

    public CombatSession(UUID sessionId, Player attacker, Player defender, int initialTimer) {
        this.sessionId = sessionId;
        this.attacker = attacker;
        this.defender = defender;
        this.startTime = System.currentTimeMillis();
        this.lastActivityTime = new AtomicLong(startTime);
        this.timerSeconds = initialTimer;
        this.active = new AtomicBoolean(true);
        this.state = CombatState.ACTIVE_COMBAT;
        this.timerData = new TimerData(sessionId, initialTimer);
        this.currentTheme = "default";
        this.visualsEnabled = true;
        this.interferenceData = new InterferenceData();
        this.attackerDamageDealt = 0.0;
        this.defenderDamageDealt = 0.0;
        this.attackerHitsLanded = 0;
        this.defenderHitsLanded = 0;
        this.endReason = null;
        this.endTime = 0;
    }

    // Getters and setters
    public UUID getSessionId() { return sessionId; }

    public Player getAttacker() { return attacker; }

    public Player getDefender() { return defender; }

    public long getStartTime() { return startTime; }

    public int getTimerSeconds() { return timerSeconds; }
    public void setTimerSeconds(int timerSeconds) { this.timerSeconds = timerSeconds; }

    public BossBar getBossBar() { return bossBar; }
    public void setBossBar(BossBar bossBar) { this.bossBar = bossBar; }

    public boolean isActive() { return active.get(); }
    public void setActive(boolean active) { this.active.set(active); }

    public CombatState getState() { return state; }
    public void setState(CombatState state) { this.state = state; }

    public TimerData getTimerData() { return timerData; }
    public void setTimerData(TimerData timerData) { this.timerData = timerData; }

    public String getCurrentTheme() { return currentTheme; }
    public void setCurrentTheme(String currentTheme) { this.currentTheme = currentTheme; }

    public boolean isVisualsEnabled() { return visualsEnabled; }
    public void setVisualsEnabled(boolean visualsEnabled) { this.visualsEnabled = visualsEnabled; }

    public InterferenceData getInterferenceData() { return interferenceData; }
    public void setInterferenceData(InterferenceData interferenceData) { this.interferenceData = interferenceData; }

    /**
     * Gets the opponent of a given player in this session.
     */
    public Player getOpponent(Player player) {
        if (player.equals(attacker)) return defender;
        if (player.equals(defender)) return attacker;
        return null;
    }

    /**
     * Checks if a player is part of this session.
     */
    public boolean involvesPlayer(Player player) {
        return player.equals(attacker) || player.equals(defender);
    }

    /**
     * Gets the remaining time in seconds.
     */
    public int getRemainingTime() {
        return timerData != null ? timerData.getRemainingSeconds() : 0;
    }

    /**
     * Checks if the combat timer has expired.
     */
    public boolean isExpired() {
        return timerData != null && timerData.isExpired();
    }

    /**
     * Resets the combat timer.
     */
    public void resetTimer() {
        if (timerData != null) {
            timerData.reset();
        }
        timerSeconds = timerData.getInitialDurationSeconds();
    }

    /**
     * Records an interference incident in this session.
     */
    public void recordInterference(Player interferer, Player target) {
        if (interferenceData != null) {
            Player opponent = getOpponent(target);
            interferenceData.recordInterference(interferer, target, opponent);
        }
    }

    /**
     * Gets the interference count for this session.
     */
    public int getInterferenceCount() {
        return interferenceData != null ? interferenceData.getInterferenceCount() : 0;
    }

    /**
     * Checks if this session has recent interference.
     */
    public boolean hasRecentInterference(long timeWindowMs) {
        return interferenceData != null && interferenceData.hasRecentInterference(timeWindowMs);
    }

    /**
     * Updates timer elapsed time and returns if expired.
     * Also updates last activity time for cleanup tracking.
     */
    public boolean updateTimer() {
        lastActivityTime.set(System.currentTimeMillis()); // Update activity time

        if (timerData != null) {
            boolean expired = timerData.updateElapsedTime();
            timerSeconds = timerData.getRemainingSeconds();
            return expired;
        }
        return false;
    }

    /**
     * Records activity in this session (for cleanup tracking).
     */
    public void recordActivity() {
        lastActivityTime.set(System.currentTimeMillis());
    }

    /**
     * Gets the last activity time for cleanup purposes.
     */
    public long getLastActivityTime() {
        return lastActivityTime.get();
    }

    /**
     * Checks if this session has been inactive for too long.
     */
    public boolean isInactive(long maxInactiveTimeMs) {
        return (System.currentTimeMillis() - lastActivityTime.get()) > maxInactiveTimeMs;
    }
    
    /**
     * Records damage dealt by a player in this session.
     */
    public void recordDamage(Player damager, double damage) {
        // Update damage and hit stats
        if (damager.equals(attacker)) {
            attackerDamageDealt += damage;
            attackerHitsLanded++;
        } else if (damager.equals(defender)) {
            defenderDamageDealt += damage;
            defenderHitsLanded++;
        }
    }
    
    /**
     * Gets hits landed by a specific player in this session.
     */
    public int getHitsLanded(Player player) {
        if (player.equals(attacker)) {
            return attackerHitsLanded;
        } else if (player.equals(defender)) {
            return defenderHitsLanded;
        }
        return 0;
    }
    
    /**
     * Gets damage dealt by a specific player in this session.
     */
    public double getDamageDealt(Player player) {
        if (player.equals(attacker)) {
            return attackerDamageDealt;
        } else if (player.equals(defender)) {
            return defenderDamageDealt;
        }
        return 0.0;
    }
    
    /**
     * Gets damage received by a specific player in this session.
     */
    public double getDamageReceived(Player player) {
        if (player.equals(attacker)) {
            return defenderDamageDealt; // Attacker received damage from defender
        } else if (player.equals(defender)) {
            return attackerDamageDealt; // Defender received damage from attacker
        }
        return 0.0;
    }
    
    /**
     * Get the reason this combat ended.
     */
    public CombatEndReason getEndReason() {
        return endReason;
    }
    
    /**
     * Set the reason this combat ended.
     */
    public void setEndReason(CombatEndReason endReason) {
        this.endReason = endReason;
        this.endTime = System.currentTimeMillis();
    }
    
    /**
     * Get the time when this combat ended.
     */
    public long getEndTime() {
        return endTime;
    }
    
    /**
     * Get the duration of this combat session in milliseconds.
     */
    public long getDuration() {
        long end = endTime > 0 ? endTime : System.currentTimeMillis();
        return end - startTime;
    }
    
    /**
     * Check if this session has ended.
     */
    public boolean hasEnded() {
        return endReason != null;
    }
}
