package com.gmail.emertens.PdxTrackRouter;

import org.bukkit.block.BlockFace;

/**
 * This class provides utility methods for operating on BlockFace values
 * as compass directions.
 * @author Eric Mertens
 */
public class BlockFaceUtils {

	/**
	 * The cardinal directions are north, east, south, and west.
	 */
	public static final BlockFace[] cardinalDirections
	  = new BlockFace[] {BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH};

	/**
	 * The ordinal directions are north-east, south-east, south-west, and north-west.
	 */
	public static final BlockFace[] ordinalDirections
	  = new BlockFace[] {BlockFace.NORTH_EAST, BlockFace.SOUTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_WEST};

	/**
	 * Translate a direction string into a block face
	 * @param c String describing a direction
	 * @return block face corresponding to game direction
	 */
	public static BlockFace charToDirection(String c) {
		if (c.length() == 0) {
			return null;
		}
		switch (Character.toUpperCase(c.charAt(0))) {
		case 'N': return BlockFace.EAST;
		case 'E': return BlockFace.SOUTH;
		case 'S': return BlockFace.WEST;
		case 'W': return BlockFace.NORTH;
		default: return null;
		}
	}

	/**
	 * Return the opposite cardinal direction
	 * @param a A cardinal direction
	 * @return The opposite direction
	 */
	public static BlockFace opposite(BlockFace a) {
		if (a == null) {
			return null;
		}
		switch (a) {
		case NORTH: return BlockFace.SOUTH;
		case SOUTH: return BlockFace.NORTH;
		case EAST: return BlockFace.WEST;
		case WEST: return BlockFace.EAST;
		default: return null;
		}
	}

	/**
	 * Compute the ordinal direction of two cardinal directions, if possible
	 * @param a A cardinal direction
	 * @param b A cardinal direction
	 * @return An ordinal directions or null if there is none
	 */
	public static BlockFace addFaces(BlockFace a, BlockFace b) {
		if (a == null || b == null) {
			return null;
		}
		switch (a) {
		case NORTH:
			switch (b) {
			case EAST: return BlockFace.NORTH_EAST;
			case WEST: return BlockFace.NORTH_WEST;
			default: return null;
			}
		case EAST:
			switch (b) {
			case NORTH: return BlockFace.NORTH_EAST;
			case SOUTH: return BlockFace.SOUTH_EAST;
			default: return null;
			}
		case SOUTH:
			switch (b) {
			case EAST: return BlockFace.SOUTH_EAST;
			case WEST: return BlockFace.SOUTH_WEST;
			default: return null;
			}
		case WEST:
			switch (b) {
			case NORTH: return BlockFace.NORTH_WEST;
			case SOUTH: return BlockFace.SOUTH_WEST;
			default: return null;
			}
		default: return null;
		}
	}
	
	public static BlockFace turnFortyFiveDegreesCW(BlockFace b) {
		switch (b) {
			case NORTH: return BlockFace.NORTH_EAST;
			case SOUTH: return BlockFace.SOUTH_WEST;
			case EAST: return BlockFace.SOUTH_EAST;
			case WEST: return BlockFace.NORTH_WEST;
			case NORTH_EAST: return BlockFace.EAST;
			case NORTH_WEST: return BlockFace.NORTH;
			case SOUTH_EAST: return BlockFace.SOUTH;
			case SOUTH_WEST: return BlockFace.WEST;
		}
		return null;
	}

	public static BlockFace turnFortyFiveDegreesCCW(BlockFace b) {
		switch (b) {
			case NORTH: return BlockFace.NORTH_WEST;
			case SOUTH: return BlockFace.SOUTH_EAST;
			case EAST: return BlockFace.NORTH_EAST;
			case WEST: return BlockFace.SOUTH_WEST;
			case NORTH_EAST: return BlockFace.NORTH;
			case NORTH_WEST: return BlockFace.WEST;
			case SOUTH_EAST: return BlockFace.EAST;
			case SOUTH_WEST: return BlockFace.SOUTH;
		}
		return null;
	}

}
