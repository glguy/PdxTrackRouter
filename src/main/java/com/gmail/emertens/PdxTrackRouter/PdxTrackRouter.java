package com.gmail.emertens.PdxTrackRouter;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
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
	public static String DESTINATION_HEADER = "[destination]";
	public static String JUNCTION_HEADER = "[junction]";

	/**
	 * Store player destination preferences based on player name.
	 */
	private Map<String,String> playerTargets = new HashMap<String, String>();

	@Override
	public void onEnable() {
		PluginManager pm = getServer().getPluginManager();

		// Listen for mine cart events
		TrackListener trackListener = new TrackListener(this);
		pm.registerEvents(trackListener, this);

		// Listen for player events
		PlayerListener playerListener = new PlayerListener(this);
		pm.registerEvents(playerListener, this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED
					+ "This command can only be run by a player");
			return true;
		}

		final Player player = (Player) sender;

		if (command.getName().equalsIgnoreCase("destination")) {
			if (args.length == 0) {
				clearPlayerDestination(player, true);
				return true;
			} else {
				StringBuilder builder = new StringBuilder();
				for (String arg : args) {
					builder.append(arg);
				}
				setPlayerDestination(player, builder.toString());
				return true;
			}

		} else if (command.getName().equalsIgnoreCase("changesign")
				&& args.length >= 1) {

			try {
				StringBuilder builder = new StringBuilder();
				for (int i = 1; i < args.length; i++) {
					if (i > 1) {
						builder.append(' ');
					}
					builder.append(args[i]);
				}

				signChangeCommand(player, Integer.parseInt(args[0]), builder.toString());
			} catch (NumberFormatException e) {
				return false;
			}
			return true;
		}
		return false;
	}

	private static void signChangeCommand(Player player, int lineNo, String line) {
		Block block = player.getTargetBlock(null, 10);

		if (lineNo < 1 || lineNo > 4) {
			player.sendMessage(ChatColor.RED + "Line number out of range");
			return;
		}

		if (block == null) {
			player.sendMessage(ChatColor.RED + "No sign in range");
			return;
		}

		BlockState state = block.getState();
		if (!(state instanceof Sign)) {
			player.sendMessage(ChatColor.RED + "Selected block not a sign");
			return;
		}

		Sign sign = (Sign) state;
		sign.setLine(lineNo-1, ChatColor.translateAlternateColorCodes('&', line));
		sign.update();
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
		final String prefix = normalizeDestination(destination) + ":";
		final String defaultPrefix = DEFAULT_DESTINATION + ":";

		// Search through the sign lines for a valid, matching route
		for (int i = 1; i < lines.length; i++) {
			final String current = normalizeDestination(lines[i]);
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
			if (dir == null || dir == BlockFaceUtils.opposite(direction)) {
				continue;
			}

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
		if (traveling == BlockFaceUtils.opposite(target)) {
			return null;
		}

		// You can't go off the tracks
		if (target == open) {
			return null;
		}

		// Heading into a T junction
		if (traveling == open) {
			return BlockFaceUtils.addFaces(BlockFaceUtils.opposite(target), open);
		}

		// Continuing straight through a junction
		if (traveling == target) {
			return BlockFaceUtils.addFaces(BlockFaceUtils.opposite(target), open);
		}

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
		if (direction == target) {
			return direction;
		}

		// Impossible to reverse direction
		if (direction == BlockFaceUtils.opposite(target)) {
			return null;
		}

		// Compute a turn
		return BlockFaceUtils.addFaces(direction, BlockFaceUtils.opposite(target));
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
	 * Clear the target destination for a given player.
	 * @param player The player whose destination preference should be cleared
	 * @param verbose Send player a message even if no destination was set
	 */
	public void clearPlayerDestination(Player player, boolean verbose) {
		if (playerTargets.containsKey(player.getName())) {
			playerTargets.remove(player.getName());
			player.sendMessage(ChatColor.GREEN + "Destination cleared");
		} else if (verbose) {
			player.sendMessage(ChatColor.RED + "No destination set");
		}
	}

	/**
	 * Set the target destination for a player.
	 * @param player The player whose destination should be set
	 * @param destination The destination to set for the player
	 */
	public void setPlayerDestination(Player player, String destination) {
		final String uncolored = ChatColor.stripColor(destination);
		final String normalized = normalizeDestination(uncolored);
		playerTargets.put(player.getName(), normalized);
		player.sendMessage(ChatColor.GREEN + "Destination set to "
				+ ChatColor.YELLOW + uncolored);
	}

	private static String normalizeDestination(String input) {
		return ChatColor.stripColor(input).replaceAll(" ", "").toLowerCase();
	}

	void updateJunction(Entity preferenceEntity, Junction junction, BlockFace traveling) {
		String destination = entityToPreference(preferenceEntity);
		BlockFace target = findDestination(destination, junction.getLines(), traveling);

		final BlockFace open = junction.getOpenSide();
		final BlockFace newDirection;
		if (open == null) {
			newDirection = computeFourWayJunction(traveling, target);
		} else {
			newDirection = computeJunction(traveling, open, target);
		}

		if (newDirection == null) {
			return;
		}

		junction.setRailDirection(newDirection);
	}
}
