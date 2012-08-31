/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.gmail.emertens.PdxTrackRouter;

import java.util.ArrayList;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Rails;

/**
 *
 * @author emertens
 */
public class Junction {
	/**
	 * Direction to search for stacked signs
	 */
	private static final BlockFace signStackDirection = BlockFace.UP;

	/**
	 * Locations to check for junction signs relative to the junction track
	 */
	private static final BlockFace[] signLocations = (BlockFace[]) ArrayUtils.add(
			BlockFaceUtils.ordinalDirections, BlockFace.UP);

	private final String[] lines;
	private final Block block;
	private final BlockFace openSide;

	public Junction(Block block, String[] lines) {
		this.lines = lines;
		this.block = block;
		this.openSide = null;
	}

	public Junction(Block block, String[] lines, BlockFace openSide) {
		this.lines = lines;
		this.block = block;
		this.openSide = openSide;
	}

	public Block getBlock() {
		return block;
	}

	public String[] getLines() {
		return lines;
	}

	public BlockFace getOpenSide() {
		return openSide;
	}

	public static Junction makeJunction(Block block) {
		// Only check when arriving at a rails block
		if (block.getType() != Material.RAILS) {
			return null;
		}

		// Check that this is a 3-way intersection with a junction sign
		BlockFace openEnd = null;
		String[] lines = new String[] {};

		// Verify that this block's neighbors are all rails
		// or a junction sign stack, but nothing else.
		for (BlockFace d : BlockFaceUtils.cardinalDirections) {
			Block neighbor = block.getRelative(d);
			if (isConnectedRail(neighbor,d)) {
				continue;
			} else if (neighbor.getType() == Material.AIR
					&& isConnectedSlopeRail(neighbor.getRelative(BlockFace.DOWN), d)) {
				// Look down hills
				continue;
			} else if (openEnd == null) {
				openEnd = d;
				// Give the open-end of a 3-way junction a chance to hold the sign
				lines = collectJunctionSignLines(neighbor);
			} else {
				// Abort as soon as we don't find rails or a sign
				return null;
			}
		}

		// Search the corners if this wasn't a 3-way junction with a sign at the
		// open end of the junction.
		if (lines.length == 0) {
			lines = findCornerSigns(block);
		}

		if (lines.length == 0) {
			return null;
		}

		return new Junction(block, lines, openEnd);
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
	 * Search the sign locations for a unique junction sign
	 * @param block Center block to search from
	 * @return Unique lines found or an empty array otherwise
	 */
	private static String[] findCornerSigns(Block block) {
		String[] result = new String[] {};
		for (BlockFace d : signLocations) {
			Block b = block.getRelative(d);
			String[] lines = collectJunctionSignLines(b);
			if (lines.length != 0) {
				if (result.length == 0) {
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
	private static String[] collectJunctionSignLines(Block block) {
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
		    ChatColor.stripColor(result[0]).equalsIgnoreCase(PdxTrackRouter.JUNCTION_HEADER)) {
			return result;
		} else {
			return new String[] {};
		}
	}

	/**
	 * Treat a block like a rail and set its direction. Ignore nulls.
	 * @param block A block which is a rail
	 * @param newDirection new direction that rail should be set to
	 */
	public void setRailDirection(BlockFace newDirection) {
		BlockState state = block.getState();
		Rails rails = (Rails) state.getData();
		rails.setDirection(newDirection, false);
		state.setData(rails);
		state.update();
	}



	/**
	 * Return the orientation of a rail block, or null if it is not a rail
	 * @param b Block to check orientation of
	 * @return Orientation of rail block or null
	 */
	public static BlockFace railDirection(Block b) {
		MaterialData d = b.getState().getData();
		if (d instanceof Rails) {
			Rails r = (Rails) d;
			return r.getDirection();
		}
		return null;
	}

	private static boolean isConnectedSlopeRail(Block b, BlockFace dir) {
		MaterialData d = b.getState().getData();
		if (d instanceof Rails) {
			Rails r = (Rails) d;
			return r.isOnSlope() && r.getDirection() == BlockFaceUtils.opposite(dir);
		}
		return false;
	}
	
	private static boolean isConnectedRail(Block b, BlockFace dir) {
		BlockFace blockDir = railDirection(b);
		
		return blockDir == dir
				|| blockDir == BlockFaceUtils.opposite(dir)
				|| blockDir == BlockFaceUtils.turnFortyFiveDegreesCCW(dir)
				|| blockDir == BlockFaceUtils.turnFortyFiveDegreesCW(dir);
	}
}
