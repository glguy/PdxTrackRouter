package com.gmail.emertens.PdxTrackRouter;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.PoweredMinecart;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.material.Rails;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This class implements the logic for a player to express a desired
 * destination and to update junction track blocks to get the player
 * to that destination following the values specified on signs.
 *
 * @author Eric Mertens
 */
public class PdxTrackRouter extends JavaPlugin {

	private static final String DEFAULT_DESTINATION = "default";
	private static String DESTINATION_HEADER = "[destination]";
	private static String JUNCTION_HEADER = "[junction]";

	/**
	 * Store player destination preferences based on player name.
	 */
	private Map<String,String> playerTargets = new HashMap<String, String>();

	@Override
	public void onEnable() {
		PluginManager pm = getServer().getPluginManager();

		// Listen for mine cart events
		TrackListener trackListener = new TrackListener(this, JUNCTION_HEADER);
		pm.registerEvents(trackListener, this);

		// Listen for player events
		PlayerListener playerListener = new PlayerListener(this, DESTINATION_HEADER, JUNCTION_HEADER);
		pm.registerEvents(playerListener, this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {

		final boolean success;

		if (sender instanceof Player) {
			Player player = (Player) sender;
			switch (args.length) {
			case 0:
				clearPlayerDestination(player);
				success = true;
				break;
			case 1:
				setPlayerDestination(player, args[0]);
				success = true;
				break;
			default:
				success = false;
			}
		} else {
			success = true;
			sender.sendMessage(ChatColor.RED + "This command can only be run by a player");
		}
		return success;
	}

	/**
	 * Update a junction a player is about to cross.
	 * @param preferenceEntity The player in the mine cart
	 * @param block The middle track piece to be updated
	 * @param traveling The direction the player is traveling
	 * @param open The direction which has no track
	 * @param lines The concatenated lines of the junction sign stack
	 */
	public void updateThreeWayJunction(Entity preferenceEntity, Block block, BlockFace traveling, BlockFace open, String[] lines) {
		String destination = entityToPreference(preferenceEntity);
		BlockFace target = findDestination(destination, lines, traveling);
		BlockFace newDirection = computeJunction(traveling, open, target);
		if (newDirection == null) return;
		setRailDirection(block, newDirection);
	}

	private String entityToPreference(Entity entity) {
		if (entity instanceof Player) {
			Player player = (Player)entity;
			return playerToDestination(player);
		} else if (entity instanceof StorageMinecart) {
			return "chest";
		} else if (entity instanceof PoweredMinecart) {
			return "engine";
		} else if (entity instanceof Minecart) {
			return "empty";
		} else {
			return DEFAULT_DESTINATION;
		}
	}

	/**
	 * Find a target direction given a junction sign line array and a destination name. If no rules
	 * match continue forward.
	 * @param destination Destination label to search for
	 * @param lines Lines of sign to search in
	 * @param direction
	 * @return first matching direction or first default direction
	 */
	private static BlockFace findDestination(String destination, String[] lines, BlockFace direction) {
		final String prefix = destination.toLowerCase() + ":";
		final String defaultPrefix = DEFAULT_DESTINATION + ":";

		// Search through the sign lines for a valid, matching route
		for (int i = 1; i < lines.length; i++) {
			final String current = ChatColor.stripColor(lines[i]).toLowerCase().replaceAll(" ", "");
			final int prefixLength;

			if (current.startsWith(prefix)) {
				prefixLength = prefix.length();
			} else if (current.startsWith(defaultPrefix)) {
				prefixLength = defaultPrefix.length();
			} else {
				continue;
			}

			final String directionPart = current.substring(prefixLength).trim();
			final BlockFace dir = BlockFaceUtils.charToDirection(directionPart);

			// Ignore invalid and unusable routes
			if (dir == null || dir == BlockFaceUtils.opposite(direction)) continue;

			return dir;
		}
		// If no rules match default to continuing forward.
		return direction;
	}

	/**
	 * Compute the new direction a track should face
	 * @param traveling The direction the player is going
	 * @param open The direction that has no track
	 * @param target The direction the player wants to go
	 * @return The direction the track should be changed to
	 */
	private static BlockFace computeJunction(BlockFace traveling, BlockFace open, BlockFace target) {

		// You can't turn around
		if (traveling == BlockFaceUtils.opposite(target)) return null;

		// You can't go off the tracks
		if (target == open) return null;

		// Heading into a T junction
		if (traveling == open) return BlockFaceUtils.addFaces(BlockFaceUtils.opposite(target), open);

		// Continuing straight through a junction
		if (traveling == target) return BlockFaceUtils.addFaces(BlockFaceUtils.opposite(target), open);

		// Turning into a junction
		return BlockFaceUtils.addFaces(traveling, open);
	}

	/**
	 * Compute the track direction to take a player moving in one direction
	 * and move him in the target direction
	 * @param direction Direction player is moving
	 * @param target Direction player wants to be moving
	 * @return Direction the junction track should be positioned in.
	 */
	private BlockFace computeFourWayJunction(BlockFace direction, BlockFace target) {

		// Continuing straight through
		if (direction == target) return direction;

		// Impossible to reverse direction
		if (direction == BlockFaceUtils.opposite(target)) return null;

		// Compute a turn
		return BlockFaceUtils.addFaces(direction, BlockFaceUtils.opposite(target));
	}

	/**
	 * This call back is called when a player reaches a 4-way intersection
	 * @param preferenceEntity Player who reached the intersection
	 * @param block  Center block of the intersection
	 * @param direction Direction player is traveling
	 */
	public void updateFourWayJunction(Entity preferenceEntity, Block block, BlockFace direction, String[] lines) {
		final String destination = entityToPreference(preferenceEntity);
		BlockFace target = findDestination(destination, lines, direction);
		final BlockFace newDirection = computeFourWayJunction(direction, target);
		if (newDirection == null) return;
		setRailDirection(block, newDirection);
	}

	/**
	 * Determine the target destination for a player. Assume that null means
	 * that there is no player and the cart is empty.
	 * @param player Player in the cart or null for empty carts
	 * @return The label of the preferred destination if found, default otherwise
	 */
	private String playerToDestination(Player player) {
		String destination = playerTargets.get(player.getName());

		if (destination == null) {
			destination = DEFAULT_DESTINATION;
		}

		return destination;
	}

	/**
	 * Treat a block like a rail and set its direction. Ignore nulls.
	 * @param block A block which is a rail
	 * @param newDirection new direction that rail should be set to
	 */
	private static void setRailDirection(Block block, BlockFace newDirection) {
		BlockState state = block.getState();
		Rails rails = (Rails) state.getData();
		rails.setDirection(newDirection, false);
		state.setData(rails);
		state.update();
	}

	/**
	 * Clear the target destination for a given player.
	 * @param player The player whose destination preference should be cleared
	 */
	public void clearPlayerDestination(Player player) {
		if (playerTargets.containsKey(player.getName())) {
			playerTargets.remove(player.getName());
			player.sendMessage(ChatColor.GREEN + "Destination cleared");
		}
	}

	/**
	 * Set the target destination for a player.
	 * @param player The player whose destination should be set
	 * @param destination The destination to set for the player
	 */
	public void setPlayerDestination(Player player, String destination) {
		playerTargets.put(player.getName(), destination);
		player.sendMessage(ChatColor.GREEN + "Destination set to "
				+ ChatColor.YELLOW + destination);

	}
}
