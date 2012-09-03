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
	private final Block a;
	private final BlockFace b;

	public RailVector(Block a, BlockFace b) {
		this.a = a;
		this.b = b;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof RailVector)) {
			return false;
		}
		RailVector o = (RailVector) other;
		return a.equals(o.a) && b.equals(o.b);
	}

	@Override
	public int hashCode() {
		return a.hashCode() + b.hashCode() * 13;
	}
}

public class RailSearch {

	private static final int BLOCKS_PER_ITERATION = 100;
	private static final int ITERATION_TICK_DELAY = 10;

	private final Block firstBlock;
	private final Player player;
	private final Set<String> result = new HashSet<String>();
	private final Set<RailVector> visited = new HashSet<RailVector>();
	private final Queue<BlockFace> faces = new LinkedList<BlockFace>();
	private final Plugin plugin;

	/**
	 * Cursor for the current search in progress
	 */
	private Block block;

	/**
	 * Direction of the cursor for the current search
	 */
	private BlockFace direction;

	/**
	 * Direction that the current search started in
	 */
	private BlockFace firstDirection;

	private RailSearch(Block block, Player player, Plugin plugin) {
		this.firstBlock = block;
		this.player = player;
		this.plugin = plugin;
		faces.addAll(Arrays.asList(BlockFaceUtils.CARDINAL_DIRECTIONS));
	}

	public void step() {

		if (block == null) {
			firstDirection = direction = faces.poll();
			result.clear();
			visited.clear();
			block = firstBlock.getRelative(direction);
		}

		int cutoff = 0;

		while (true) {
			// Search downward to find the nearest block
			while (block.getType() == Material.AIR) {
				block = block.getRelative(BlockFace.DOWN);
			}

			final RailVector p = new RailVector(block, direction);
			if (visited.contains(p)) { break; }
			visited.add(new RailVector(block, direction));

			final MaterialData d = block.getState().getData();
			if (!(d instanceof Rails)) {
				break;
			}
			final Rails rails = (Rails) d;
			final BlockFace railDirection = rails.getDirection();

			//Compute the direction that we will depart from this block
			final Junction junction = Junction.makeJunction(block);
			if (junction != null) {

				for (final String s : junction.getLines()) {
					final String[] parts = PdxTrackRouter.normalizeDestination(s).split(":");
					if (parts.length == 2) {
						if (BlockFaceUtils.charToDirection(parts[1]) != BlockFaceUtils.opposite(direction)
								&& !parts[0].equals(PdxTrackRouter.DEFAULT_DESTINATION)) {
							result.add(parts[0]);
						}
					}
				}

				direction = PdxTrackRouter.findDestination(
						PdxTrackRouter.DEFAULT_DESTINATION,
						junction.getLines(), direction);
			} else {
				direction = RailSearch.checkTurn(direction, railDirection);
			}

			// Compute the next block we will arrive at
			block = block.getRelative(direction);

			// Correct for slopes
			if (rails.isOnSlope()) {
				if (railDirection == direction) {
					block = block.getRelative(BlockFace.UP);
				}
			}

			// Ensure we don't hold the main thread for too long
			cutoff++;
			if (cutoff > BLOCKS_PER_ITERATION) {
				yield();
				return;
			}
		}

		// Clear block so that next run will start a new direction
		block = null;

		if (!result.isEmpty()) {
			reportToPlayer();
		}

		if (!faces.isEmpty()) {
			yield();
		} else {
			player.sendMessage(ChatColor.GREEN + "Search complete");
		}
	}

	private void reportToPlayer() {
		final StringBuilder builder = new StringBuilder();

		builder.append(BlockFaceUtils.toCorrectString(firstDirection) + ": ");
		for (String s : result) {
			builder.append(ChatColor.YELLOW + s);
			builder.append(ChatColor.GRAY + "; ");
		}
		player.sendMessage(builder.toString());
	}

	private void yield() {
		Runnable task = new Runnable() {
			@Override
			public void run() { step(); }
		};
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, task, ITERATION_TICK_DELAY);
	}

	public static void findRoute(Block block, Player player, Plugin plugin) {

		final RailSearch task = new RailSearch(block, player, plugin);
		task.step();
	}

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
