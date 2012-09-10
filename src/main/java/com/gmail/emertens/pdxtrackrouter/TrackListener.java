package com.gmail.emertens.pdxtrackrouter;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

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
	public void onVehicleMove(VehicleMoveEvent event) {

		final Block from = event.getFrom().getBlock();
		final Block to = event.getTo().getBlock();
		final BlockFace currentDirection = from.getFace(to);

		// Handle the common case early
		if (currentDirection == BlockFace.SELF || currentDirection == null) {
			return;
		}

		// Figure out where the minecart is likely to go next
		final BlockFace nextDirection = RailSearch.computeNextRail(from, to, currentDirection);
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

		final Vehicle vehicle = event.getVehicle();
		final Entity passenger = vehicle.getPassenger();

		// Base routing decisions on the identity of this entity
		final Entity preferenceEntity = passenger == null ? vehicle : passenger;

		// If a junction sign has been found, treat this as a plug-in controlled
		// junction and report to the plug-in

		plugin.updateJunction(preferenceEntity, junction, nextDirection);
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

	/**
	 * When a minecart is destroyed we clean its preference from the system
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onMinecartDestroyed(VehicleDestroyEvent event) {
		Entity entity = event.getVehicle();
		if (entity instanceof Minecart) {
			plugin.clearEntityDestination(entity.getEntityId());
		}
	}

	/**
	 * Ensure that a minecart starts with a fresh preference in case entity ids
	 * get reused
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onMinecartCreated(VehicleCreateEvent event) {
		Entity entity = event.getVehicle();
		if (entity instanceof Minecart) {
			plugin.clearEntityDestination(entity.getEntityId());
		}
	}
}
