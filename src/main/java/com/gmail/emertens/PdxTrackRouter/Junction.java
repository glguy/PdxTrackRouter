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
public final class Junction {
	/**
	 * Direction to search for stacked signs
	 */
	private static final BlockFace SIGN_STACK_DIRECTION = BlockFace.UP;

	/**
	 * Locations to check for junction signs relative to the junction track
	 */
	private static final BlockFace[] signLocations =
		(BlockFace[]) ArrayUtils.add(BlockFaceUtils.ORDINAL_DIRECTIONS, BlockFace.UP);

	private final String[] lines;
	private final Block block;
	private final BlockFace openSide;

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

	/**
	 * Construct a junction starting at a given block if possible.
	 * @param block The candidate junction block
	 * @return Junction object if block is a routed junction
	 */
	public static Junction makeJunction(Block block) {
		// Only check when arriving at a rails block
		if (block.getType() != Material.RAILS) {
			return null;
		}

		// Check that this is a 3-way intersection with a junction sign
		BlockFace openEnd = null;

		// Lines of the routing table
		String[] lines = new String[] {};

		// Verify that this block's neighbors are all rails
		// or a junction sign stack, but nothing else.
		for (BlockFace d : BlockFaceUtils.CARDINAL_DIRECTIONS) {
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

		// If there are no routing lines to be found, this is not a junction
		if (lines.length == 0) {
			return null;
		}

		return new Junction(block, lines, openEnd);
	}

	/**
	 * Search the sign locations for a unique junction sign
	 * @param block Center block to search from
	 * @return Unique lines found or an empty array otherwise
	 */
	private static String[] findCornerSigns(Block block) {
		String[] result = new String[] {};

		for (BlockFace d : signLocations) {
			final Block b = block.getRelative(d);
			final String[] lines = collectJunctionSignLines(b);

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
			final Sign sign = (Sign) state;
			final String[] lines = sign.getLines();
			for (int i = lines.length - 1; i >= 0; i--) {
				stack.add(lines[i]);
			}
			block = block.getRelative(SIGN_STACK_DIRECTION);
		}

		if (stack.isEmpty()) {
			return new String[] {};
		}

		// The top line is the last in the list due to searching from the bottom
		final String topLine = ChatColor.stripColor(stack.get(stack.size()-1));

		if (!PdxTrackRouter.JUNCTION_HEADER.equalsIgnoreCase(topLine)) {
			return new String[] {};
		}

		// Removing the header will always leave at least 3 lines
		stack.remove(stack.size()-1);

		final String[] result = stack.toArray(new String[stack.size()]);
		ArrayUtils.reverse(result);

		return result;
	}

	/**
	 * Treat a block like a rail and set its direction. Ignore nulls.
	 * @param block A block which is a rail
	 * @param newDirection new direction that rail should be set to
	 */
	public void setRailDirection(final BlockFace newDirection) {
		final BlockState state = block.getState();
		final Rails rails = (Rails) state.getData();
		rails.setDirection(newDirection, false);
		state.setData(rails);
		state.update();
	}

	/**
	 * Return the orientation of a rail block, or null if it is not a rail
	 * @param b Block to check orientation of
	 * @return Orientation of rail block or null
	 */
	public static BlockFace railDirection(final Block b) {
		final MaterialData d = b.getState().getData();
		if (d instanceof Rails) {
			final Rails r = (Rails) d;
			return r.getDirection();
		}
		return null;
	}

	private static boolean isConnectedSlopeRail(final Block b, final BlockFace dir) {
		final MaterialData d = b.getState().getData();
		if (d instanceof Rails) {
			final Rails r = (Rails) d;
			return r.isOnSlope() && r.getDirection() == BlockFaceUtils.opposite(dir);
		}
		return false;
	}

	private static boolean isConnectedRail(final Block b, final BlockFace dir) {
		final BlockFace blockDir = railDirection(b);
		return blockDir == dir
				|| blockDir == BlockFaceUtils.opposite(dir)
				|| blockDir == BlockFaceUtils.turnFortyFiveDegreesCCW(dir)
				|| blockDir == BlockFaceUtils.turnFortyFiveDegreesCW(dir);
	}
}
