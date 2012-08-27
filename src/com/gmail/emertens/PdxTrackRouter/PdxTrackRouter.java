package com.gmail.emertens.PdxTrackRouter;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.material.Rails;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This class implements the logic for a player to express a desired
 * destination and to update junction track blocks to get the player
 * to that destination following the values specified on signs.
 *
 * @author Eric Mertens
 */
public class PdxTrackRouter extends JavaPlugin {

	private static final String EMPTY_DESTINATION = "empty";
	private static final String DEFAULT_DESTINATION = "default";
	private static String DESTINATION_HEADER = "[destination]";
	private static String JUNCTION_HEADER = "[junction]";

	private Map<String,String> playerTargets = new HashMap<String, String>();

	@Override
	public void onEnable() {
		TrackListener trackListener = new TrackListener(this, JUNCTION_HEADER);
		getServer().getPluginManager().registerEvents(trackListener, this);

		PlayerListener playerListener = new PlayerListener(this, DESTINATION_HEADER);
		getServer().getPluginManager().registerEvents(playerListener, this);
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
	 * @param player The player in the mine cart
	 * @param block The middle track piece to be updated
	 * @param traveling The direction the player is traveling
	 * @param open The direction which has no track
	 * @param lines The concatenated lines of the junction sign stack
	 */
	public void updateJunction(Player player, Block block, BlockFace traveling, BlockFace open, String[] lines) {
		String destination = playerToDestination(player);
		BlockFace target = findDestination(destination, lines);
		if (target == null) return;

		BlockFace newDirection = computeJunction(traveling, open, target);
		setRailDirection(block, newDirection);
	}

	/**
	 * Find a target direction given a junction sign line array and a destination name.
	 * @param destination Destination label to search for
	 * @param lines Lines of sign to search in
	 * @return first matching direction or first default direction
	 */
	private static BlockFace findDestination(String destination, String[] lines) {
		final String prefix = destination.toLowerCase() + ":";
		final String defaultPrefix = DEFAULT_DESTINATION + ":";

		for (int i = 1; i < lines.length; i++) {
			final String current = lines[i].toLowerCase();
			final String str;

			if (current.startsWith(prefix)) {
				str = current.substring(prefix.length());
			} else if (current.startsWith(defaultPrefix)) {
				str = current.substring(defaultPrefix.length());
			} else {
				continue;
			}
			return BlockFaceUtils.charToDirection(str.trim());
		}
		return null;
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
		if (direction == target) return direction;
		if (direction == BlockFaceUtils.opposite(target)) return null;
		return BlockFaceUtils.addFaces(direction, BlockFaceUtils.opposite(target));
	}

	/**
	 * This call back is called when a player reaches a 4-way intersection
	 * @param player Player who reached the intersection
	 * @param block  Center block of the intersection
	 * @param direction Direction player is traveling
	 */
	public void updateFourWay(Player player, Block block, BlockFace direction, String[] lines) {
		String destination = playerToDestination(player);
		BlockFace target = findDestination(destination, lines);
		if (target == null) return;
		BlockFace newDirection = computeFourWayJunction(direction, target);
		setRailDirection(block, newDirection);
	}

	private String playerToDestination(Player player) {
		String destination;
		if (player != null) {
			destination = playerTargets.get(player.getName());
			if (destination == null) {
				destination = DEFAULT_DESTINATION;
			}
		} else {
			destination = EMPTY_DESTINATION;
		}
		return destination;
	}

	/**
	 * Treat a block like a rail and set its direction
	 * @param block A block which is a rail
	 * @param newDirection new direction that rail should be set to
	 */
	private static void setRailDirection(Block block, BlockFace newDirection) {
		BlockState state = block.getState();
		Rails rails = (Rails)state.getData();

		if (newDirection != null) {
			rails.setDirection(newDirection, false);
			state.setData(rails);
			state.update();
		}
	}

	public void clearPlayerDestination(Player player) {
		playerTargets.remove(player.getName());
		player.sendMessage(ChatColor.GREEN + "Destination cleared");
	}

	public void setPlayerDestination(Player player, String destination) {
		playerTargets.put(player.getName(), destination);
		player.sendMessage(ChatColor.GREEN + "Destination set to "
				+ ChatColor.YELLOW + destination);

	}
}
