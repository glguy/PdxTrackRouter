package com.gmail.emertens.pdxtrackrouter.listeners;

import org.bukkit.Bukkit;
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
		final Block from = event.getFrom().getBlock();
		final Block to = event.getTo().getBlock();
		final BlockFace direction = from.getFace(to);

		// Handle the common case early
		if (direction == BlockFace.SELF || direction == null) {
			return;
		}

		final Vehicle vehicle = event.getVehicle();
		final Event subevent = new VehicleMoveBlockEvent(to, direction, vehicle);
		pluginManager.callEvent(subevent);
	}
}
