package com.gmail.emertens.PdxTrackRouter;

import java.util.ArrayList;

import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Location;
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
		
		Location from = event.getFrom();
		Location to   = event.getTo();
		
		// Skip move events inside the same block
		if (from.getBlockX() == to.getBlockX()
				&& from.getBlockY() == to.getBlockY()
				&& from.getBlockZ() == to.getBlockZ())
			return;
	
		// Skip move events where a player is not riding
		Entity passenger = event.getVehicle().getPassenger();
		if (! (passenger instanceof Player)) return;
		Player player = (Player)passenger;
		
		// Compute the next block the car will arrive at
		int bx = to.getBlockX() - from.getBlockX() + to.getBlockX();
		int by = to.getBlockY() - from.getBlockY() + to.getBlockY();
		int bz = to.getBlockZ() - from.getBlockZ() + to.getBlockZ();
		
		Block block = to.getWorld().getBlockAt(bx,by,bz);
		BlockFace direction = to.getBlock().getFace(block);
		
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
			} else if (openEnd == null && neighbor.getState() instanceof Sign) {
				openEnd = d;
				lines = collectJunctionSignLines(neighbor);
				if (lines == null) return;
				if (!lines[0].equalsIgnoreCase("[junction]")) return;
			} else {
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
