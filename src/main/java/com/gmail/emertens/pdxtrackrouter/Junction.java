package com.gmail.emertens.pdxtrackrouter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Rails;

/**
 * This class contains all the information to make decisions
 * at a plug-in controlled junction and to update the junction.
 * @author Eric Mertens
 */
public final class Junction {
	private final Collection<String> lines;
	private final Block block;
	private final BlockFace openSide;

	/**
	 * Class constructor specifying junction block, routing lines, and open face.
	 * @param block the rails block of the junction
	 * @param lines the lines of the routing signs
	 * @param openSide The face corresponding to the open end of the
	 *                 junction, if one exists; null otherwise.
	 */
	public Junction(Block block, Collection<String> lines, BlockFace openSide) {
		this.lines = lines;
		this.block = block;
		this.openSide = openSide;
	}

	/**
	 * Returns the lines of the routing signs for this junction.
	 * @returns the lines of the routing signs for this junction
	 */
	public Collection<String> getLines() {
		return lines;
	}

	/**
	 * Returns the open face of the junction for 3-day junctions,
	 * otherwise returns null for 4-way junctions.
	 * @returns the open face of the junction if one exists
	 */
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
		Block openBlock = null;

		// Verify that this block's neighbors are all rails
		// or a junction sign stack, but nothing else.
		for (BlockFace d : BlockFaceUtils.CARDINAL_DIRECTIONS) {
			Block neighbor = block.getRelative(d);
			if (isConnectedRail(neighbor,d)
			 || neighbor.getType() == Material.AIR
			    && isConnectedSlopeRail(neighbor.getRelative(BlockFace.DOWN), d)) {
				// Do nothing
			} else if (openEnd == null) {
				openEnd = d;
				openBlock = neighbor;
			} else {
				// Abort after two non-rails
				return null;
			}
		}

		Collection<String> routingLines = findJunctionSigns(block, openBlock);
		if (routingLines == null) {
			return null;
		}

		return new Junction(block, routingLines, openEnd);
	}

	private static Collection<String> findJunctionSigns(Block block,
			Block openBlock) {
		Collection<String> routingLines = null;

		if (routingLines == null) {
			final Block under = block.getRelative(BlockFace.DOWN, 2);
			routingLines = collectJunctionSignLinesDown(under);
		}

		if (routingLines == null) {
			final Block above = block.getRelative(BlockFace.UP);
			routingLines = collectJunctionSignLinesUp(above);
		}

		if (routingLines == null && openBlock != null) {
			routingLines = collectJunctionSignLinesUp(openBlock);
		}

		if (routingLines == null) {
			routingLines = findCornerSigns(block);
		}

		return routingLines;
	}

	/**
	 * Search the sign locations for a unique junction sign
	 * @param block Center block to search from
	 * @return Unique lines found or an empty array otherwise
	 */
	private static Collection<String> findCornerSigns(Block block) {
		Collection<String> result = null;

		for (BlockFace d : BlockFaceUtils.ORDINAL_DIRECTIONS) {
			final Block b = block.getRelative(d);
			final Collection<String> lines = collectJunctionSignLinesUp(b);

			if (lines != null) {
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
	 * @param searchDirection When true start the search at the bottom sign, otherwise search down from the top
	 * @return An array of the lines of the sign stack
	 */
	private static Collection<String> collectJunctionSignLinesUp(Block block) {
		final List<String> stack = new ArrayList<String>();

		// Search upwards collecting all the sign lines,
		// stop when the junction header is found
		for (;;) {
			final BlockState state = block.getState();
		    if (!(state instanceof Sign)) {
			return null;
		    }

			final Sign sign = (Sign) state;
			final String[] lines = sign.getLines();

			stack.add(lines[3]);
			stack.add(lines[2]);
			stack.add(lines[1]);

			if (PdxTrackRouter.isJunctionHeader(lines[0])) {
				Collections.reverse(stack);
				return stack;
			} else {
				stack.add(lines[0]);
			}

			block = block.getRelative(BlockFace.UP);
		}
	}

	/**
	 * Collect the concatenated lines from a stack of signs
	 *
	 * @param block The topmost of the potential stack of signs
	 * @param searchDirection When true start the search at the bottom sign, otherwise search down from the top
	 * @return An array of the lines of the sign stack
	 */
	private static Collection<String> collectJunctionSignLinesDown(Block block) {
		final Collection<String> stack = new ArrayList<String>();

		// Search downward collecting all the sign lines
		boolean firstSign = true;

		for (;;) {
			final BlockState state = block.getState();

			// Not a junction sign stack if you find a non-sign before
			// a junction header
			if (!(state instanceof Sign)) {
				return firstSign ? null : stack;
			}

			final Sign sign = (Sign) state;
			final String[] lines = sign.getLines();

			if (firstSign) {
				firstSign = false;
				if (!PdxTrackRouter.isJunctionHeader(lines[0])) {
					return null;
				}
			} else {
				stack.add(lines[0]);
			}

			stack.add(lines[1]);
			stack.add(lines[2]);
			stack.add(lines[3]);

			block = block.getRelative(BlockFace.DOWN);
		}
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
		return blockDir != null // shortcut for non-rails
				&& (blockDir == dir
				 || blockDir == BlockFaceUtils.opposite(dir)
				 || blockDir == BlockFaceUtils.turnFortyFiveDegreesCCW(dir)
				 || blockDir == BlockFaceUtils.turnFortyFiveDegreesCW(dir));
	}
}
