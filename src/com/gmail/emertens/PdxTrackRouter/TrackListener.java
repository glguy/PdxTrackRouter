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
 * and notifies the plugin when one is found.
 * @author Eric Mertens
 *
 */
public class TrackListener implements Listener {
	
	private PdxTrackRouter plugin;
	private static BlockFace[] cardinalDirections
	  = new BlockFace[] {BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH}; 
	
	public TrackListener(PdxTrackRouter p) {
		plugin = p;
	}
		
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
		
		for (BlockFace d : cardinalDirections) {
			Block neighbor = block.getRelative(d);
			Material m = neighbor.getType();
			if (m == Material.RAILS || m == Material.POWERED_RAIL) {
				continue;
			} else if (openEnd == null) {
				openEnd = d;
				lines = collectJunctionSignLines(neighbor);
				if (lines.length == 0 || !lines[0].equalsIgnoreCase("[junction]")) return;
			} else {
				// Abort as soon as we don't find rails or a sign
				return;
			}
		}
		
		// Notify the plug-in
		plugin.updateJunction(player, block, direction, openEnd, lines);
	}

	private static String[] collectJunctionSignLines(Block block) {
		ArrayList<String> stack = new ArrayList<String>();
		
		// Search upwards collecting all the sign lines
		while(true) {
			BlockState state = block.getState();
			
			if (state instanceof Sign) {
				Sign sign = (Sign)state;
				String[] lines = sign.getLines();
				for (int i = 3; i >= 0; i--) {
					stack.add(lines[i]);
				}
				block = block.getRelative(BlockFace.UP);
			} else {
				break;
			}
		}
		String[] result = stack.toArray(new String[]{});
		ArrayUtils.reverse(result);
		return result;
	}
}
