package com.gmail.emertens.pdxtrackrouter.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.plugin.PluginManager;

import com.gmail.emertens.pdxtrackrouter.events.VehicleMoveBlockEvent;

/**
 * The listener generates {@link VehicleMoveBlockEvent} events.
 * @author Eric Mertens
 *
 */
public final class VehicleMoveBlockListener implements Listener {
	private final PluginManager pluginManager = Bukkit.getServer().getPluginManager();

	@EventHandler
	public void onVehicleMoveEvent(final VehicleMoveEvent event) {

		final Location fromLocation = event.getFrom();
		final Location toLocation   = event.getTo();

		if (fromLocation.getBlockX() == toLocation.getBlockX()
				&& fromLocation.getBlockY() == toLocation.getBlockY()
				&& fromLocation.getBlockZ() == toLocation.getBlockZ()) {
			return;
		}

		final Block from = fromLocation.getBlock();
		final Block to = toLocation.getBlock();
		final BlockFace direction = calculateTravelingDirection(from, to);

		if (direction == null) {
			return;
		}

		final Vehicle vehicle = event.getVehicle();
		final Event subevent = new VehicleMoveBlockEvent(to, direction, vehicle);
		pluginManager.callEvent(subevent);
	}

	private static final BlockFace[] FLAT_DIRECTIONS = new BlockFace[] {
			BlockFace.NORTH,
			BlockFace.SOUTH,
			BlockFace.EAST,
			BlockFace.WEST,
			BlockFace.NORTH_EAST,
			BlockFace.NORTH_WEST,
			BlockFace.SOUTH_EAST,
			BlockFace.SOUTH_WEST
	};

	private BlockFace calculateTravelingDirection(Block from, Block to) {
		int x = 0;
		if (to.getX() < from.getX()) {
			x = -1;
		} else if (to.getX() > from.getX()) {
			x = 1;
		}

		int z = 0;
		if (to.getZ() < from.getZ()) {
			z = -1;
		} else if (to.getZ() > from.getZ()) {
			z = 1;
		}

		// First attempt to match cardinal and ordinal diretions, ignoring vertical movement
		for (BlockFace face : FLAT_DIRECTIONS) {
			if (face.getModX() == x && face.getModZ() == z) {
				return face;
			}
		}

		// If the cart is only moving vertically then resort to UP and DOWN
		if (to.getY() < from.getY()) {
			return BlockFace.DOWN;
		}

		if (to.getY() > from.getY()) {
			return BlockFace.UP;
		}

		return null;
	}
}
