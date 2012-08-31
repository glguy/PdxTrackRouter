package com.gmail.emertens.PdxTrackRouter;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
	 * @param p
	 *            The plug-in to notify when a junction is approached
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
		final BlockFace nextDirection = computeNextRail(from, to, currentDirection);
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


	private static BlockFace computeNextRail(Block from, Block to, BlockFace traveling) {

		final BlockFace toDir;

		switch (traveling) {
		case UP:
			return Junction.railDirection(from);
		case DOWN:
			toDir = Junction.railDirection(to);
			// If we are falling out of the sky guess we will continue to
			if (toDir == null) {
				return traveling;
			}
			return BlockFaceUtils.opposite(toDir);
		case NORTH:
		case SOUTH:
		case EAST:
		case WEST:
			toDir = Junction.railDirection(to);

			// If we are not on a rail guess we will not turn
			if (toDir == null) {
				return traveling;
			}

			return checkTurn(traveling, toDir);
		default:
			return null;
		}
	}

	/**
	 * Compute the next block a player is likely to encounter when traveling
	 * on a flat piece of track.
	 * @param traveling The direction the player is traveling
	 * @param track     The direction the track is facing
	 * @return The direction the player will leave the track block
	 */
	private static BlockFace checkTurn(BlockFace traveling, BlockFace track) {

		switch (track) {
		case NORTH_EAST:
			switch (traveling) {
			case NORTH: return BlockFace.WEST;
			case EAST:  return BlockFace.SOUTH;
			default:    return traveling;
			}
		case NORTH_WEST:
			switch (traveling) {
			case NORTH: return BlockFace.EAST;
			case WEST:  return BlockFace.SOUTH;
			default:    return traveling;
			}
		case SOUTH_EAST:
			switch (traveling) {
			case SOUTH: return BlockFace.WEST;
			case EAST:  return BlockFace.NORTH;
			default:    return traveling;
			}
		case SOUTH_WEST:
			switch (traveling) {
			case SOUTH: return BlockFace.EAST;
			case WEST:  return BlockFace.NORTH;
			default:    return traveling;
			}
		default:        return traveling;
		}
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
