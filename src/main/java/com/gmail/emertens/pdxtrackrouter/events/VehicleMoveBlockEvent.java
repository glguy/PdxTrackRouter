package com.gmail.emertens.pdxtrackrouter.events;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is used when a vehicle moves into a new block.
 * @author Eric Mertens
 *
 */
public final class VehicleMoveBlockEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Block block;
    private final BlockFace direction;
    private final Vehicle vehicle;

    /**
     * Construct a new VehicleMoveBlockEvent.
     * @param block Block vehicle moved into
     * @param direction Direction vehicle is traveling
     * @param vehicle Vehicle which moved
     */
    public VehicleMoveBlockEvent(final Block block, final BlockFace direction, final Vehicle vehicle) {
    	this.block = block;
    	this.direction = direction;
    	this.vehicle = vehicle;
    }

    /**
     * Return the list of handlers for this event.
     * @return the list of handlers for this event
     */
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	/**
     * Return the list of handlers for this event.
     * @return the list of handlers for this event
     */
	public static HandlerList getHandlerList() {
		return handlers;
	}

	/**
     * Return the block the vehicle moved into.
     * @return the block the vehicle moved into
     */
	public Block getBlock() {
		return block;
	}

	/**
	 * Return the direction the vehicle is traveling.
	 * @return the direction the vehicle is traveling
	 */
	public BlockFace getDirection() {
		return direction;
	}

	/**
	 * Return the vehicle that moved.
	 * @return the vehicle that moved
	 */
	public Vehicle getVehicle() {
		return vehicle;
	}

}
