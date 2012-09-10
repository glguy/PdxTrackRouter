package com.gmail.emertens.pdxtrackrouter;

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
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This class implements the logic for a player to express a desired
 * destination and to update junction track blocks to get the player
 * to that destination following the values specified on signs.
 *
 * @author Eric Mertens
 */
public final class PdxTrackRouter extends JavaPlugin {

	public static final String DEFAULT_DESTINATION = "default";
	public static final String EMPTY_DESTINATION = "empty";
	public static final String CHEST_DESTINATION = "chest";
	public static final String ENGINE_DESTINATION = "engine";
	public static final String DESTINATION_HEADER = "[destination]";
	public static final String JUNCTION_HEADER = "[junction]";

	/**
	 * Mapping from player names to destination preference.
	 */
	private Map<String,String> playerTargets = new HashMap<String, String>();

	@Override
	public void onEnable() {
		final PluginManager pm = getServer().getPluginManager();

		// Listen for mine cart events
		TrackListener trackListener = new TrackListener(this);
		pm.registerEvents(trackListener, this);

		// Listen for player events
		PlayerListener playerListener = new PlayerListener(this);
		pm.registerEvents(playerListener, this);
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command command,
			final String label, final String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED
					+ "This command can only be run by a player");
			return true;
		}

		final Player player = (Player) sender;

		if (command.getName().equalsIgnoreCase("destination")) {
			if (args.length == 0) {
				clearPlayerDestination(player, true);
			} else {
				StringBuilder builder = new StringBuilder();
				for (String arg : args) {
					builder.append(arg);
				}
				setPlayerDestination(player, builder.toString());
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
		} else if (command.getName().equalsIgnoreCase("junctions")) {
			junctionsCommand(player);
		} else {
			return false;
		}
		return true;
	}

	private void junctionsCommand(final Player player) {

		Block block = player.getWorld().getBlockAt(player.getLocation());

		player.sendMessage(ChatColor.GREEN + "Junction search started");
		RailSearch.findRoute(block, player, this);
	}

	private static void signChangeCommand(final Player player, final int lineNo, final String line) {
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

		final Sign sign = (Sign) state;
		sign.setLine(lineNo-1, ChatColor.translateAlternateColorCodes('&', line));
		sign.update();
	}

	/**
	 * Determine the destination for a given entity
	 * @param entity Entity to find the destination for
	 * @return A normalized destination for that entity
	 */
	private String entityToPreference(final Entity entity) {
		if (entity instanceof Player) {
			final Player player = (Player)entity;
			return playerToDestination(player);
		} else if (playerTargets.containsKey(Integer.toString(entity.getEntityId()))) {
			return playerTargets.get(Integer.toString(entity.getEntityId()));
		} else if (entity instanceof StorageMinecart) {
			return CHEST_DESTINATION;
		} else if (entity instanceof PoweredMinecart) {
			return ENGINE_DESTINATION;
		} else if (entity instanceof Minecart) {
			return EMPTY_DESTINATION;
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
	public static BlockFace findDestination(final String destination, final String[] lines, final BlockFace direction) {
		final String prefix = normalizeDestination(destination) + ":";
		final String defaultPrefix = DEFAULT_DESTINATION + ":";

		// Search through the sign lines for a valid, matching route
		for (String line : lines) {
			final String current = normalizeDestination(line);
			final int prefixLength;

			if (current.startsWith(prefix)) {
				prefixLength = prefix.length();
			} else if (current.startsWith(defaultPrefix)) {
				prefixLength = defaultPrefix.length();
			} else {
				continue;
			}

			final String directionPart = current.substring(prefixLength);
			final BlockFace routeDir = BlockFaceUtils.charToDirection(directionPart);

			// Ignore invalid and unusable routes
			if (routeDir == null || routeDir == BlockFaceUtils.opposite(direction)) {
				continue;
			}

			return routeDir;
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
	private static BlockFace computeJunction(final BlockFace traveling, final BlockFace open, final BlockFace target) {

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
	private BlockFace computeFourWayJunction(final BlockFace direction, final BlockFace target) {

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
	private String playerToDestination(final Player player) {
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
	public void clearPlayerDestination(final Player player, final boolean verbose) {
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
	public void setPlayerDestination(final Player player, final String destination) {
		final String uncolored = ChatColor.stripColor(destination);
		final String normalized = normalizeDestination(uncolored);
		playerTargets.put(player.getName(), normalized);
		player.sendMessage(ChatColor.GREEN + "Destination set to "
				+ ChatColor.YELLOW + uncolored);
	}

	/**
	 * Normalize text to remove all whitespace and color codes to make comparisons
	 * between commands and various signs more likely to match.
	 * @param input String to be normalized
	 * @return Normalized version of input
	 */
	public static String normalizeDestination(final String input) {
		return ChatColor.stripColor(input).replaceAll(" ", "").toLowerCase();
	}

	/**
	 * Reconfigure a junction based on an entity's destination preference
	 * and travel direction.
	 * @param preferenceEntity Entity used to compute direction preference
	 * @param junction Junction to be updated
	 * @param traveling Direction the entity will travel into the junction
	 */
	public void updateJunction(final Entity preferenceEntity, final Junction junction, final BlockFace traveling) {
		final String destination = entityToPreference(preferenceEntity);
		final BlockFace target = findDestination(destination, junction.getLines(), traveling);
		final BlockFace open = junction.getOpenSide();

		final BlockFace newDirection;
		if (open == null) {
			newDirection = computeFourWayJunction(traveling, target);
		} else {
			newDirection = computeJunction(traveling, open, target);
		}

		if (newDirection != null) {
			junction.setRailDirection(newDirection);
		}
	}

	/**
	 * Copy a player's destination preference over to a minecart and
	 * report the effect to the player.
	 * @param player Player whose preference should be used
	 * @param entityId Entity to copy the preference to
	 */
	public void transferDestination(final Player player, final int entityId) {
		String destination = playerToDestination(player);
		player.sendMessage(ChatColor.GREEN
				+ "Transfering destination preference " + ChatColor.YELLOW
				+ destination + ChatColor.GREEN + " to minecart");
		setEntityDestination(entityId, destination);
	}

	private void setEntityDestination(final int entityId, final String destination) {
		playerTargets.put(Integer.toString(entityId), destination);
	}

	public void clearEntityDestination(int entityId) {
		playerTargets.remove(Integer.toString(entityId));
	}
}
