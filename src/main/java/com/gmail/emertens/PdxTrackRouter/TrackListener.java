package com.gmail.emertens.PdxTrackRouter;

import java.util.ArrayList;

import org.apache.commons.lang.ArrayUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Rails;

/**
 * This listener catches vehicle move events which correspond arriving at a
 * junction and notifies the plug-in when one is found.
 *
 * @author Eric Mertens
 *
 */
@SuppressWarnings("unused")
public class TrackListener implements Listener {

	private static BlockFace signStackDirection = BlockFace.UP;

	/**
	 * Plug-in to notify when events happen.
	 */
	private PdxTrackRouter plugin;

	/**
	 * String to match against when checking for junction signs.
	 */
	private String junctionHeader;

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
		BlockFace direction = from.getFace(to);

		// Handle the common case early
		if (direction == BlockFace.SELF || direction == null) return;
		direction = computeNextRail(from, to, direction);

		final Block block = to.getRelative(direction);
		if (block == null)
			return;

		// Only check when arriving at a rails block
		if (block.getType() != Material.RAILS)
			return;

		// Check that this is a 3-way intersection with a junction sign
		BlockFace openEnd = null;
		String[] lines = null;

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

		Entity preferenceEntity;

		Vehicle vehicle = event.getVehicle();
		Entity passenger = vehicle.getPassenger();

		preferenceEntity = passenger == null ? vehicle : passenger;

		if (lines == null) lines = findCornerSigns(block);

		if (lines != null) {
			if (openEnd == null) {
				plugin.updateFourWay(preferenceEntity, block, direction, lines);
			} else {
				plugin.updateJunction(preferenceEntity, block, direction,
						openEnd, lines);
			}
		}
	}

	private static boolean isRail(Material m) {
		return m == Material.RAILS || m == Material.POWERED_RAIL;
	}

	private static BlockFace railDirection(Block b) {
		MaterialData d = b.getState().getData();
		if (d instanceof Rails) {
			Rails r = (Rails) d;
			return r.getDirection();
		}
		return null;
	}

	private BlockFace computeNextRail(Block from, Block to, BlockFace traveling) {
		switch (traveling) {
		case UP:
			return railDirection(from);
		case DOWN:
			return BlockFaceUtils.opposite(railDirection(to));
		case NORTH:
		case SOUTH:
		case EAST:
		case WEST:
			final BlockFace d = railDirection(to);
			final BlockFace next = checkTurn(traveling, d);
			if (next == null)
				return traveling;
			return next;
		default:
			return null;
		}
	}

	private static BlockFace checkTurn(BlockFace traveling, BlockFace track) {
		if (track == null) return null;
		switch (track) {
		case NORTH_EAST:
			switch (traveling) {
			case NORTH:
				return BlockFace.WEST;
			case EAST:
				return BlockFace.SOUTH;
			default:
				return null;
			}
		case NORTH_WEST:
			switch (traveling) {
			case NORTH:
				return BlockFace.EAST;
			case WEST:
				return BlockFace.SOUTH;
			default:
				return null;
			}
		case SOUTH_EAST:
			switch (traveling) {
			case SOUTH:
				return BlockFace.WEST;
			case EAST:
				return BlockFace.NORTH;
			default:
				return null;
			}
		case SOUTH_WEST:
			switch (traveling) {
			case SOUTH:
				return BlockFace.EAST;
			case WEST:
				return BlockFace.NORTH;
			default:
				return null;
			}
		default:
			return null;
		}
	}

	private static BlockFace[] signLocations = (BlockFace[]) ArrayUtils.add(
			BlockFaceUtils.ordinalDirections, BlockFace.UP);

	private String[] findCornerSigns(Block block) {
		String[] result = null;
		for (BlockFace d : signLocations) {
			Block b = block.getRelative(d);
			String[] lines = collectJunctionSignLines(b);
			if (lines != null && lines.length > 0) {
				if (result == null) {
					result = lines;
				} else {
					return null;
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

		final String[] result = stack.toArray(new String[] {});
		ArrayUtils.reverse(result);

		if (result.length > 0 &&
		    ChatColor.stripColor(result[0]).equalsIgnoreCase(junctionHeader)) {
			return result;
		} else {
			return null;
		}
	}

	/**
	 * Reset players' destinations upon departing a mine cart.
	 *
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onVehicleExit(VehicleExitEvent event) {
		if (event.getVehicle().getType() != EntityType.MINECART)
			return;
		LivingEntity entity = event.getExited();
		if (entity instanceof Player) {
			plugin.clearPlayerDestination((Player) entity);
		}
	}
}
