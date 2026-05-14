package com.muzlik.pvpcombat.interfaces;

import com.muzlik.pvpcombat.combat.CombatTracker;
import org.bukkit.entity.Player;
import java.util.UUID;

/**
 * Interface for managing combat states and sessions.
 */
public interface ICombatManager {

    /**
     * Starts a new combat session between two players.
     *
     * @param attacker The attacking player
     * @param defender The defending player
     * @return The UUID of the created combat session, or null if failed
     */
    UUID startCombat(Player attacker, Player defender);

    /**
     * Ends a combat session for the given player.
     *
     * @param playerId The UUID of the player
     * @return true if a session was ended, false otherwise
     */
    boolean endCombat(UUID playerId);

    /**
     * Checks if a player is currently in combat.
     *
     * @param player The player to check
     * @return true if in combat, false otherwise
     */
    boolean isInCombat(Player player);

    /**
     * Resets the timer for a combat session.
     *
     * @param sessionId The session UUID
     * @return true if reset, false if session not found
     */
    boolean resetTimer(UUID sessionId);

    /**
     * Gets the opponent of a player in combat.
     *
     * @param player The player
     * @return The opponent player, or null if not in combat
     */
    Player getOpponent(Player player);
}