package com.gmail.emertens.PdxTrackRouter;

import org.bukkit.block.BlockFace;

public class BlockFaceUtils {

	public static BlockFace[] cardinalDirections
	  = new BlockFace[] {BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH};
	public static BlockFace[] combinedDirections
	  = new BlockFace[] {BlockFace.NORTH_EAST, BlockFace.SOUTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_WEST};

	/**
	 * Translate a direction string into a block face
	 * @param c String describing a direction
	 * @return block face corresponding to game direction
	 */
	public static BlockFace charToDirection(String c) {
		if (c.length() == 0) return null;
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
		switch (a) {
		case NORTH: return BlockFace.SOUTH;
		case SOUTH: return BlockFace.NORTH;
		case EAST: return BlockFace.WEST;
		case WEST: return BlockFace.EAST;
		default: return null;
		}
	}

	/**
	 * Compute the combination of two cardinal directions
	 * @param a A cardinal direction
	 * @param b A cardinal direction
	 * @return A combination of the cardinal directions or null if there is none
	 */
	public static BlockFace addFaces(BlockFace a, BlockFace b) {
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
}
