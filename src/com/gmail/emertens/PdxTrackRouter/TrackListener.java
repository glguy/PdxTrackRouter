package com.gmail.emertens.PdxTrackRouter;

import java.util.ArrayList;

import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
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

	private static final String JUNCTION_HEADER = "[junction]";
	private static BlockFace[] cardinalDirections
	  = new BlockFace[] {BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH};
	private static BlockFace signStackDirection = BlockFace.UP;

	private PdxTrackRouter plugin;

	/**
	 * Construct a new TrackListener
	 * @param p The plug-in to notify when a junction is approached
	 */
	public TrackListener(PdxTrackRouter p) {
		plugin = p;
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

		// Skip move events inside the same block
		if (from == to) return;

		// Skip move events where a player is not riding
		Entity passenger = event.getVehicle().getPassenger();
		if (! (passenger instanceof Player)) return;
		Player player = (Player)passenger;

		BlockFace direction = from.getFace(to);
		Block block = to.getRelative(direction);

		// Only check when arriving at a rails block
		if (block.getType() != Material.RAILS) return;

		// Check that this is a 3-way intersection with a junction sign
		BlockFace openEnd = null;
		String[] lines = null;

		// Verify that this block's neighbors are all rails
		// or a junction sign stack, but nothing else.
		for (BlockFace d : cardinalDirections) {
			Block neighbor = block.getRelative(d);
			Material m = neighbor.getType();
			if (m == Material.RAILS || m == Material.POWERED_RAIL) {
				continue;
			} else if (openEnd == null) {
				openEnd = d;
				lines = collectJunctionSignLines(neighbor);
				if (lines.length == 0 || !lines[0].equalsIgnoreCase(JUNCTION_HEADER)) return;
			} else {
				// Abort as soon as we don't find rails or a sign
				return;
			}
		}

		// Notify the plug-in
		if (openEnd == null) {
			plugin.updateFourWay(player, block, direction);
		} else {
			plugin.updateJunction(player, block, direction, openEnd, lines);
		}
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
				stack.add(lines[i]);
			}
			block = block.getRelative(signStackDirection);
		}

		final String[] result = stack.toArray(new String[]{});
		ArrayUtils.reverse(result);
		return result;
	}
}
