package com.gmail.emertens.PdxTrackRouter;

import java.util.ArrayList;

import org.apache.commons.lang.ArrayUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * This listener catches vehicle move events which correspond arriving at a junction
 * and notifies the plug-in when one is found.
 * @author Eric Mertens
 *
 */
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
	 * @param p The plug-in to notify when a junction is approached
	 * @param header
	 */
	public TrackListener(PdxTrackRouter p, String header) {
		plugin = p;
		junctionHeader = header;
	}

	/**
	 * Notify the plug-in when a player is approaching a properly
	 * configured junction.
	 * @param event
	 */
	@EventHandler(ignoreCancelled=true)
	public void onEvent(VehicleMoveEvent event) {

		Block from = event.getFrom().getBlock();
		Block to   = event.getTo().getBlock();
		BlockFace direction = from.getFace(to);
		// Skip move events inside the same block
		if (direction == BlockFace.SELF) return;

		Block block = to.getRelative(direction);

		// Only check when arriving at a rails block
		if (block.getType() != Material.RAILS) return;

		// Check that this is a 3-way intersection with a junction sign
		BlockFace openEnd = null;
		String[] lines = null;

		// Verify that this block's neighbors are all rails
		// or a junction sign stack, but nothing else.
		for (BlockFace d : BlockFaceUtils.cardinalDirections) {
			Block neighbor = block.getRelative(d);
			Material m = neighbor.getType();
			if (m == Material.RAILS || m == Material.POWERED_RAIL) {
				continue;
			} else if (openEnd == null) {
				openEnd = d;
				lines = collectJunctionSignLines(neighbor);
				if (lines.length == 0 || !lines[0].equalsIgnoreCase(junctionHeader)) return;
			} else {
				// Abort as soon as we don't find rails or a sign
				return;
			}
		}

		Entity passenger = event.getVehicle().getPassenger();
		final Player player;
		if (passenger instanceof Player) {
			player = (Player)passenger;
		} else {
			player = null;
		}

		// Notify the plug-in
		if (openEnd == null) {
			lines = findCornerSigns(block);
			if (lines != null) {
				plugin.updateFourWay(player, block, direction, lines);
			}
		} else {
			plugin.updateJunction(player, block, direction, openEnd, lines);
		}
	}

	private static String[] findCornerSigns(Block block) {
		String[] result = null;
		for (BlockFace d : BlockFaceUtils.combinedDirections) {
			Block b = block.getRelative(d);
			String[] lines = collectJunctionSignLines(b);
			if (lines.length > 0) {
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
	 * @param block The base of the potential stack of signs
	 * @return An array of the lines of the sign stack
	 */
	private static String[] collectJunctionSignLines(Block block) {
		final ArrayList<String> stack = new ArrayList<String>();

		// Search upwards collecting all the sign lines
		BlockState state;
		while((state = block.getState()) instanceof Sign) {
			Sign sign = (Sign)state;
			String[] lines = sign.getLines();
			for (int i = lines.length - 1; i >= 0; i--) {
				stack.add(ChatColor.stripColor(lines[i]));
			}
			block = block.getRelative(signStackDirection);
		}

		final String[] result = stack.toArray(new String[]{});
		ArrayUtils.reverse(result);
		return result;
	}

	/**
	 * Reset players' destinations upon departing a mine cart.
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onVehicleExit(VehicleExitEvent event) {
		if (event.getVehicle().getType() != EntityType.MINECART) return;
		LivingEntity entity = event.getExited();
		if (entity instanceof Player) {
			plugin.clearPlayerDestination((Player)entity);
		}
	}
}
