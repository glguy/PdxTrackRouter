package com.gmail.emertens.PdxTrackRouter;

import java.util.ArrayList;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Rails;

/**
 * This listener catches vehicle move events which correspond arriving at a
 * junction and notifies the plug-in when one is found.
 *
 * @author Eric Mertens
 *
 */
public class TrackListener implements Listener {

	/**
	 * Direction to search for stacked signs
	 */
	private static final BlockFace signStackDirection = BlockFace.UP;

	/**
	 * Locations to check for junction signs relative to the junction track
	 */
	private static final BlockFace[] signLocations = (BlockFace[]) ArrayUtils.add(
			BlockFaceUtils.ordinalDirections, BlockFace.UP);
	
	/**
	 * Plug-in to notify when events happen.
	 */
	private final PdxTrackRouter plugin;

	/**
	 * String to match against when checking for junction signs.
	 */
	private final String junctionHeader;

	/**
	 * Construct a new TrackListener
	 *
	 * @param p
	 *            The plug-in to notify when a junction is approached
	 * @param header
	 */
	public TrackListener(PdxTrackRouter p, String header) {
		plugin = p;
		junctionHeader = header;
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

		// Only check when arriving at a rails block
		if (block.getType() != Material.RAILS) {
			return;
		}

		// Check that this is a 3-way intersection with a junction sign
		BlockFace openEnd = null;
		String[] lines = new String[] {};

		// Verify that this block's neighbors are all rails
		// or a junction sign stack, but nothing else.
		for (BlockFace d : BlockFaceUtils.cardinalDirections) {
			Block neighbor = block.getRelative(d);
			Material m = neighbor.getType();
			if (isRail(m)) {
				continue;
			} else if (m == Material.AIR && isRail(neighbor.getRelative(BlockFace.DOWN).getType())) {
				// Look down hills
				continue;
			} else if (openEnd == null) {
				openEnd = d;
				// Give the open-end of a 3-way junction a chance to hold the sign
				lines = collectJunctionSignLines(neighbor);
			} else {
				// Abort as soon as we don't find rails or a sign
				return;
			}
		}

		final Vehicle vehicle = event.getVehicle();
		final Entity passenger = vehicle.getPassenger();

		// Base routing decisions on the identity of this entity
		final Entity preferenceEntity = passenger == null ? vehicle : passenger;

		// Search the corners if this wasn't a 3-way junction with a sign at the
		// open end of the junction.
		if (lines.length == 0) {
			lines = findCornerSigns(block);
		}

		// If a junction sign has been found, treat this as a plug-in controlled
		// junction and report to the plug-in
		if (lines.length != 0) {
			if (openEnd == null) {
				plugin.updateFourWayJunction(preferenceEntity, block, currentDirection, lines);
			} else {
				plugin.updateThreeWayJunction(preferenceEntity, block, currentDirection,
						openEnd, lines);
			}
		}
	}

	/**
	 * Check if block is a legal rail neighbor to a junction. Detector rails
	 * are ignored because they would attempt to turn the track upon being
	 * powered
	 * @param m The material of the block to check
	 * @return True if and only if the material is rails or powered rails
	 */
	private static boolean isRail(Material m) {
		return m == Material.RAILS || m == Material.POWERED_RAIL;
	}

	/**
	 * Return the orientation of a rail block, or null if it is not a rail
	 * @param b Block to check orientation of
	 * @return Orientation of rail block or null
	 */
	private static BlockFace railDirection(Block b) {
		MaterialData d = b.getState().getData();
		if (d instanceof Rails) {
			Rails r = (Rails) d;
			return r.getDirection();
		}
		return null;
	}

	private static BlockFace computeNextRail(Block from, Block to, BlockFace traveling) {

		final BlockFace toDir;
		
		switch (traveling) {
		case UP:
			return railDirection(from);
		case DOWN:
			toDir = railDirection(to);
			// If we are falling out of the sky guess we will continue to
			if (toDir == null) {
				return traveling;
			}
			return BlockFaceUtils.opposite(toDir);
		case NORTH:
		case SOUTH:
		case EAST:
		case WEST:
			toDir = railDirection(to);
			
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
	 * Search the sign locations for a unique junction sign
	 * @param block Center block to search from
	 * @return Unique lines found or an empty array otherwise
	 */
	private String[] findCornerSigns(Block block) {
		String[] result = new String[] {};
		for (BlockFace d : signLocations) {
			Block b = block.getRelative(d);
			String[] lines = collectJunctionSignLines(b);
			if (lines.length != 0) {
				if (result.length != 0) {
					result = lines;
				} else {
					return new String[] {};
				}
			}
		}

		return result;
	}

	/**
	 * Collect the concatenated lines from a stack of signs
	 *
	 * @param block
	 *            The base of the potential stack of signs
	 * @return An array of the lines of the sign stack
	 */
	private String[] collectJunctionSignLines(Block block) {
		final ArrayList<String> stack = new ArrayList<String>();

		// Search upwards collecting all the sign lines
		BlockState state;
		while ((state = block.getState()) instanceof Sign) {
			Sign sign = (Sign) state;
			String[] lines = sign.getLines();
			for (int i = lines.length - 1; i >= 0; i--) {
				stack.add(lines[i]);
			}
			block = block.getRelative(signStackDirection);
		}

		final String[] result = stack.toArray(new String[stack.size()]);
		ArrayUtils.reverse(result);

		if (result.length != 0 &&
		    ChatColor.stripColor(result[0]).equalsIgnoreCase(junctionHeader)) {
			return result;
		} else {
			return new String[] {};
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
