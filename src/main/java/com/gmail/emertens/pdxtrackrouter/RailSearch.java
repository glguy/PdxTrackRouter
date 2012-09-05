package com.gmail.emertens.pdxtrackrouter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Rails;
import org.bukkit.plugin.Plugin;

final class RailVector {
	private final Block block;
	private final BlockFace travelDirection;
	private final transient Rails rails;

	/**
	 * Construct a new RailVector
	 * @param block Rail block
	 * @param rails Rail block's data
	 * @param travelDirection Player's direction of travel
	 */
	private RailVector(Block block, final Rails rails, BlockFace travelDirection) {
		this.block = block;
		this.travelDirection = travelDirection;
		this.rails = rails;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof RailVector)) {
			return false;
		}
		RailVector o = (RailVector) other;
		return block.equals(o.block) && travelDirection.equals(o.travelDirection);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return block.hashCode() + travelDirection.hashCode() * 13;
	}
	
	/**
	 * Attempt to construct a RailVector for the given Block. Return null if the
	 * given block is not a rail, powered rail, or detector rail. This
	 * method will search downward until a non-air block is found.
	 * @param block Block to start search for rail block from
	 * @param travelDirection Direction player is traveling
	 * @return a RailVector representing the found rail block, if possible
	 */
	public static RailVector makeRailVector(
			Block block,
			final BlockFace travelDirection) {
		
		// Search downward to find the nearest block
		while (block.getType() == Material.AIR) {
			block = block.getRelative(BlockFace.DOWN);
		}
		
		final MaterialData d = block.getState().getData();
		if (d instanceof Rails) {
			return new RailVector(block, (Rails) d, travelDirection);
		} else {
			return null;
		}
	}
	
	public Block getBlock() {
		return block;
	}
	
	public BlockFace getTravelDirection() {
		return travelDirection;
	}
	
	public BlockFace getRailDirection() {
		return rails.getDirection();
	}
	
	public boolean isOnSlope() {
		return rails.isOnSlope();
	}

	public BlockFace getExitDirection() {
		return RailSearch.checkTurn(travelDirection, rails.getDirection());
	}
}

/**
 * This class encapsulates a rail network traversal collecting the
 * destination which would have an effect on a player if he were
 * to depart a given block in any of 4 cardinal directions.
 * 
 * This class is designed to break the search up into several runs
 * to compensate for the non-thread-safe nature of Minecraft/Bukkit.
 * 
 * @author Eric Mertens
 *
 */
public final class RailSearch {

	private static final int BLOCKS_PER_ITERATION = 100;
	private static final int ITERATION_TICK_DELAY = 10;

	/**
	 * Block in which the search started
	 */
	private final Block firstBlock;
	
	private final Player player;
	private final Set<String> result = new HashSet<String>();
	private final Set<RailVector> visited = new HashSet<RailVector>();
	private final Queue<BlockFace> faces = new LinkedList<BlockFace>();
	private final Plugin plugin;

	/**
	 * Direction that the current search left the firstBlock in
	 */
	private BlockFace firstDirection;
	
	/**
	 * Current position and traveling direction of the search
	 */
	private RailVector cursor = null;

	/**
	 * Construct a new RailSearch
	 * @param block Starting block for the search
	 * @param player Player to notify with search results
	 * @param plugin Plugin used to schedule delayed computations with.
	 */
	private RailSearch(Block block, Player player, Plugin plugin) {
		this.firstBlock = block;
		this.player = player;
		this.plugin = plugin;
		faces.addAll(Arrays.asList(BlockFaceUtils.CARDINAL_DIRECTIONS));
	}

	/**
	 * Advance the search.
	 * 
	 * This method will perform a chunk of the rail network traversal
	 * and schedule subsequent searches and notify the player when
	 * appropriate. This method will perform one search for every
	 * element of the faces field, reporting to the player as each completes.
	 */
	private void step() {

		if (cursor == null) {
			firstDirection = faces.poll();
			result.clear();
			visited.clear();
			cursor = RailVector.makeRailVector(
					firstBlock.getRelative(firstDirection), firstDirection);
		}

		int cutoff = 0;

		while (cursor != null) {
			if (visited.contains(cursor)) {
				cursor = null;
				break;
			}
			 
			visited.add(cursor);

			//Compute the direction that we will depart from this block
			
			final Junction junction = Junction.makeJunction(cursor.getBlock());
			final BlockFace newDirection;
			
			if (junction == null) {
				newDirection = cursor.getExitDirection();
			} else {
				recordDestinations(junction);
				newDirection = PdxTrackRouter.findDestination(
						PdxTrackRouter.DEFAULT_DESTINATION,
						junction.getLines(), cursor.getTravelDirection());
			}

			// Compute the next block we will arrive at
			Block nextBlock = cursor.getBlock().getRelative(newDirection);

			// Correct for slopes
			if (cursor.isOnSlope()) {
				if (cursor.getRailDirection() == newDirection) {
					nextBlock = nextBlock.getRelative(BlockFace.UP);
				}
			}
			
			cursor = RailVector.makeRailVector(nextBlock, newDirection);

			// Ensure we don't hold the main thread for too long
			cutoff++;
			if (cutoff > BLOCKS_PER_ITERATION) {
				yield();
				return;
			}
		}

		if (!result.isEmpty()) {
			reportToPlayer();
		}

		if (!faces.isEmpty()) {
			yield();
		} else {
			player.sendMessage(ChatColor.GREEN + "Search complete");
		}
	}

	/**
	 * Record the interesting (non-backward, non-default) destinations which
	 * would cause an effect at this sign in the result set.
	 * @param junction Junction reached by the cursor
	 */
	private void recordDestinations(final Junction junction) {
		final BlockFace backward = BlockFaceUtils.opposite(cursor.getTravelDirection());
		for (final String s : junction.getLines()) {
			final String[] parts = PdxTrackRouter.normalizeDestination(s).split(":");
			if (parts.length == 2) {
				if (BlockFaceUtils.charToDirection(parts[1]) != backward
						&& !parts[0].equals(PdxTrackRouter.DEFAULT_DESTINATION)) {
					result.add(parts[0]);
				}
			}
		}
	}

	/**
	 * Report to player all of the unique destinations reached by departing
	 * firstBlock in firstDirection.
	 */
	private void reportToPlayer() {
		final StringBuilder builder = new StringBuilder();

		builder.append(BlockFaceUtils.toCorrectString(firstDirection) + ": ");
		for (String s : result) {
			builder.append(ChatColor.YELLOW + s);
			builder.append(ChatColor.GRAY + "; ");
		}
		player.sendMessage(builder.toString());
	}

	/**
	 * Schedule a run of the step method to be run after a short delay
	 */
	private void yield() {
		Runnable task = new Runnable() {
			@Override
			public void run() { step(); }
		};
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, task, ITERATION_TICK_DELAY);
	}

	/**
	 * Start a new destination search in all cardinal directions departing from
	 * the starting block. The search will report all destinations which a
	 * player could specify which would have a routing effect. The report will
	 * be sent to the given player.
	 * 
	 * @param block Staring block
	 * @param player Player to report to
	 * @param plugin Plugin to schedule delayed computation with
	 */
	public static void findRoute(Block block, Player player, Plugin plugin) {

		final RailSearch task = new RailSearch(block, player, plugin);
		task.step();
	}

	/**
	 * Compute the direction a player will leave the to block given that he
	 * arrived from the from block traveling in the traveling direction.
	 * @param from Block player left
	 * @param to Block player arrived
	 * @param traveling Direction player traveled to to block
	 * @return the direction player will leave to block, if possible, null otherwise
	 */
	public static BlockFace computeNextRail(Block from, Block to,
			BlockFace traveling) {

		final BlockFace toDir;

		switch (traveling) {
		case UP:
			return Junction.railDirection(from);
		case DOWN:
			toDir = Junction.railDirection(to);
			// If we are falling out of the sky guess we will continue to
			if (toDir == null) {
				return traveling;
			}
			return BlockFaceUtils.opposite(toDir);
		case NORTH:
		case SOUTH:
		case EAST:
		case WEST:
			toDir = Junction.railDirection(to);

			// If we are not on a rail guess we will not turn
			if (toDir == null) {
				return traveling;
			}

			return checkTurn(traveling, toDir);
		default:
			return null;
		}
	}

	/**
	 * Compute the next block a player is likely to encounter when traveling on
	 * a flat piece of track.
	 *
	 * @param traveling
	 *            The direction the player is traveling
	 * @param track
	 *            The direction the track is facing
	 * @return The direction the player will leave the track block
	 */
	 static BlockFace checkTurn(BlockFace traveling, BlockFace track) {

		switch (track) {
		case NORTH_EAST:
			switch (traveling) {
			case NORTH:
				return BlockFace.WEST;
			case EAST:
				return BlockFace.SOUTH;
			default:
				return traveling;
			}
		case NORTH_WEST:
			switch (traveling) {
			case NORTH:
				return BlockFace.EAST;
			case WEST:
				return BlockFace.SOUTH;
			default:
				return traveling;
			}
		case SOUTH_EAST:
			switch (traveling) {
			case SOUTH:
				return BlockFace.WEST;
			case EAST:
				return BlockFace.NORTH;
			default:
				return traveling;
			}
		case SOUTH_WEST:
			switch (traveling) {
			case SOUTH:
				return BlockFace.EAST;
			case WEST:
				return BlockFace.NORTH;
			default:
				return traveling;
			}
		default:
			return traveling;
		}
	}

}
