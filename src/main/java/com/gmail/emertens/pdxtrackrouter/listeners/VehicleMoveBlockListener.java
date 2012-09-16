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
		final BlockFace direction = from.getFace(to);

		if (direction == null) {
			return;
		}

		final Vehicle vehicle = event.getVehicle();
		final Event subevent = new VehicleMoveBlockEvent(to, direction, vehicle);
		pluginManager.callEvent(subevent);
	}
}
