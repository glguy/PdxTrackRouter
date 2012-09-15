package com.gmail.emertens.pdxtrackrouter.events;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event representing a player activating a sign with right-click
 * @author Eric Mertens
 *
 */
public final class PlayerUseCommandSignEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final Block block;
    private final Sign sign;

    /**
     * Construct a new PlayerUseCommandSignEvent.
     * @param player The player who used the sign
     * @param block The block of the sign used
     * @param sign The details of the sign used
     */
    public PlayerUseCommandSignEvent(final Player player, final Block block, final Sign sign) {
    	this.player = player;
    	this.block = block;
    	this.sign = sign;
    }

    /**
     * Returns the list of handlers for this event.
     * @return the list of handlers for this event
     */
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	/**
     * Returns the list of handlers for this event.
     * @return the list of handlers for this event
     */
	public static HandlerList getHandlerList() {
		return handlers;
	}

	/**
	 * Returns the player who used the sign
	 * @return the player who used the sign
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * Return the block used
	 * @return the block used
	 */
	public Block getBlock() {
		return block;
	}

	/**
	 * Return the sign data used.
	 * @return the sign data used
	 */
	public Sign getSign() {
		return sign;
	}
	
	private boolean cancelled = false;
	
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}
}
