package com.gmail.emertens.pdxtrackrouter;

import org.bukkit.block.BlockFace;

/**
 * This class provides utility methods for operating on BlockFace values
 * as compass directions.
 * @author Eric Mertens
 */
public final class BlockFaceUtils {
	
	/**
	 * The cardinal directions are north, east, south, and west.
	 */
	public static final BlockFace[] CARDINAL_DIRECTIONS
	  = new BlockFace[] {BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH};

	/**
	 * The ordinal directions are north-east, south-east, south-west, and north-west.
	 */
	public static final BlockFace[] ORDINAL_DIRECTIONS
	  = new BlockFace[] {BlockFace.NORTH_EAST, BlockFace.SOUTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_WEST};

	/**
	 * Translate a direction string into a block face
	 * @param c String describing a direction
	 * @return block face corresponding to game direction
	 */
	public static BlockFace charToDirection(final String c) {
		if (c.length() == 0) {
			return null;
		}
		switch (Character.toUpperCase(c.charAt(0))) {
		case 'S':
		case '0': return BlockFace.SOUTH;
		case 'W':
		case '1': return BlockFace.WEST;
		case 'N':
		case '2': return BlockFace.NORTH;
		case 'E':
		case '3': return BlockFace.EAST;
		default: return null;
		}
	}

	/**
	 * Return the opposite cardinal direction
	 * @param a A cardinal direction
	 * @return The opposite direction
	 */
	public static BlockFace opposite(final BlockFace a) {
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
	public static BlockFace addFaces(final BlockFace a, final BlockFace b) {
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

	public static BlockFace turnFortyFiveDegreesCW(final BlockFace b) {
		switch (b) {
		case NORTH: return BlockFace.NORTH_EAST;
		case SOUTH: return BlockFace.SOUTH_WEST;
		case EAST: return BlockFace.SOUTH_EAST;
		case WEST: return BlockFace.NORTH_WEST;
		case NORTH_EAST: return BlockFace.EAST;
		case NORTH_WEST: return BlockFace.NORTH;
		case SOUTH_EAST: return BlockFace.SOUTH;
		case SOUTH_WEST: return BlockFace.WEST;
		default: return null;
		}
	}

	public static BlockFace turnFortyFiveDegreesCCW(final BlockFace b) {
		switch (b) {
		case NORTH: return BlockFace.NORTH_WEST;
		case SOUTH: return BlockFace.SOUTH_EAST;
		case EAST: return BlockFace.NORTH_EAST;
		case WEST: return BlockFace.SOUTH_WEST;
		case NORTH_EAST: return BlockFace.NORTH;
		case NORTH_WEST: return BlockFace.WEST;
		case SOUTH_EAST: return BlockFace.EAST;
		case SOUTH_WEST: return BlockFace.SOUTH;
		default: return null;
		}
	}

	public static String toCorrectString(BlockFace d) {
		if (d == null) { return "None"; }
		switch (d) {
		case DOWN:
			return "Down";
		case EAST:
			return "North";
		case EAST_NORTH_EAST:
			break;
		case EAST_SOUTH_EAST:
			break;
		case NORTH:
			return "West";
		case NORTH_EAST:
			break;
		case NORTH_NORTH_EAST:
			break;
		case NORTH_NORTH_WEST:
			break;
		case NORTH_WEST:
			break;
		case SELF:
			break;
		case SOUTH:
			return "East";
		case SOUTH_EAST:
			break;
		case SOUTH_SOUTH_EAST:
			break;
		case SOUTH_SOUTH_WEST:
			break;
		case SOUTH_WEST:
			break;
		case UP:
			return "Up";
		case WEST:
			return "South";
		case WEST_NORTH_WEST:
			break;
		case WEST_SOUTH_WEST:
			break;
		default:
			break;

		}
		return "Not-implemented";
	}
}
