package com.gmail.emertens.pdxtrackrouter.listeners;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleExitEvent;

import com.gmail.emertens.pdxtrackrouter.Junction;
import com.gmail.emertens.pdxtrackrouter.PdxTrackRouter;
import com.gmail.emertens.pdxtrackrouter.RailSearch;
import com.gmail.emertens.pdxtrackrouter.events.VehicleMoveBlockEvent;

/**
 * This listener catches vehicle move events which correspond arriving at a
 * junction and notifies the plug-in when one is found.
 *
 * @author Eric Mertens
 *
 */
public final class TrackListener implements Listener {

	/**
	 * Plug-in to notify when events happen.
	 */
	private final PdxTrackRouter plugin;

	/**
	 * Construct a new TrackListener
	 *
	 * @param p The plug-in to notify when a junction is approached
	 * @param header
	 */
	public TrackListener(PdxTrackRouter p) {
		plugin = p;
	}

	/**
	 * Notify the plug-in when a player is approaching a properly configured
	 * junction.
	 *
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onVehicleMove(VehicleMoveBlockEvent event) {
		final Block to = event.getBlock();
		final BlockFace currentDirection = event.getDirection();
		final Vehicle vehicle = event.getVehicle();

		if (!(vehicle instanceof Minecart)) {
			return;
		}

		// Figure out where the minecart is likely to go next
		final BlockFace nextDirection = RailSearch.computeNextRail(to, currentDirection);
		if (nextDirection == null) {
			return;
		}

		final Block block = to.getRelative(nextDirection);
		if (block == null) {
			return;
		}

		Junction junction = Junction.makeJunction(block);
		if (junction == null) {
			return;
		}

		// If a junction sign has been found, treat this as a plug-in controlled
		// junction and report to the plug-in
		plugin.updateJunction((Minecart) vehicle, junction, nextDirection);
	}

	/**
	 * Reset players' destinations upon departing a mine cart.
	 *
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onVehicleExit(VehicleExitEvent event) {
		// Only handle minecart exits
		if (event.getVehicle().getType() != EntityType.MINECART) {
			return;
		}

		// Only handle player exits
		LivingEntity entity = event.getExited();
		if (entity instanceof Player) {
			plugin.clearPlayerDestination((Player) entity, false);
		}
	}
}
