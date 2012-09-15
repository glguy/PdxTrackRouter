package com.gmail.emertens.pdxtrackrouter.events;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class VehicleMoveBlockEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Block block;
    private final BlockFace direction;
    private final Vehicle vehicle;

    public VehicleMoveBlockEvent(final Block block, final BlockFace direction, final Vehicle vehicle) {
    	this.block = block;
    	this.direction = direction;
    	this.vehicle = vehicle;
    }

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public Block getBlock() {
		return block;
	}

	public BlockFace getDirection() {
		return direction;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

}
