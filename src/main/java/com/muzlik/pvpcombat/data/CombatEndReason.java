package com.muzlik.pvpcombat.data;

/**
 * Enumeration of reasons why a combat session ended.
 * Used for proper cleanup and statistics recording.
 */
public enum CombatEndReason {
    
    /**
     * Combat timer expired naturally without death or disconnect.
     */
    TIMER_EXPIRED("Timer Expired", true),
    
    /**
     * One of the players died during combat.
     */
    DEATH("Death", true),
    
    /**
     * Player disconnected during combat (combat logging).
     */
    DISCONNECT("Disconnect", true),
    
    /**
     * Player used /combat forfeit command.
     */
    FORFEIT("Forfeit", true),
    
    /**
     * Admin used /combat clear command.
     */
    ADMIN_CLEAR("Admin Clear", false),
    
    /**
     * Player entered a safe zone.
     */
    SAFE_ZONE("Safe Zone Entry", false),
    
    /**
     * Server shutdown or plugin reload.
     */
    SERVER_SHUTDOWN("Server Shutdown", false),
    
    /**
     * Unknown or unspecified reason.
     */
    UNKNOWN("Unknown", false);
    
    private final String displayName;
    private final boolean recordStatistics;
    
    CombatEndReason(String displayName, boolean recordStatistics) {
        this.displayName = displayName;
        this.recordStatistics = recordStatistics;
    }
    
    /**
     * Get the display name for this end reason.
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if statistics should be recorded for this end reason.
     * Admin clears and server shutdowns don't count as wins/losses.
     */
    public boolean shouldRecordStatistics() {
        return recordStatistics;
    }
    
    /**
     * Check if this is a natural combat end (not forced by admin/server).
     */
    public boolean isNaturalEnd() {
        return this == TIMER_EXPIRED || this == DEATH || this == FORFEIT;
    }
    
    /**
     * Check if this is a punishable offense (combat logging).
     */
    public boolean isPunishable() {
        return this == DISCONNECT;
    }
}
