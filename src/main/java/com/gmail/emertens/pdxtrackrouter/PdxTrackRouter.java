package com.gmail.emertens.pdxtrackrouter;

import java.util.Collection;

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
import org.bukkit.event.Listener;
import org.bukkit.metadata.FixedMetadataValue;
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

	private static final String TRACKROUTER_DESTINATION = "trackrouter.destination";
	public static final String DEFAULT_DESTINATION = "default";
	public static final String EMPTY_DESTINATION = "empty";
	public static final String CHEST_DESTINATION = "chest";
	public static final String ENGINE_DESTINATION = "engine";
	private static final String DESTINATION_HEADER = "[destination]";
	private static final String JUNCTION_HEADER = "[junction]";

	/**
	 * This method is called when the plug-in is enabled. It registers
	 * event listeners for the plug-in.
	 */
	@Override
	public void onEnable() {
		final PluginManager pm = getServer().getPluginManager();

		// Listen for mine cart events
		Listener trackListener = new TrackListener(this);
		pm.registerEvents(trackListener, this);

		// Listen for player events
		Listener playerListener = new PlayerListener(this);
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

		player.sendMessage(ChatColor.GREEN + "Junction search started");

		final Block block = player.getWorld().getBlockAt(player.getLocation());
		RailSearch.findRoute(block, player, this);
	}

	private static void signChangeCommand(final Player player, final int lineNo, final String line) {

		if (lineNo < 1 || lineNo > 4) {
			player.sendMessage(ChatColor.RED + "Line number out of range");
			return;
		}

		final Block block = player.getTargetBlock(null, 10);
		if (block == null) {
			player.sendMessage(ChatColor.RED + "No sign in range");
			return;
		}

		final BlockState state = block.getState();
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
	private String minecartToPreference(final Minecart minecart) {
		final Entity passenger = minecart.getPassenger();

		if (passenger != null) {
			if (entityHasDestination(passenger)) {
				return entityToDestination(passenger);
			} else {
				return DEFAULT_DESTINATION;
			}
		} else if (entityHasDestination(minecart)) {
			return entityToDestination(minecart);
		} else if (minecart instanceof StorageMinecart) {
			return CHEST_DESTINATION;
		} else if (minecart instanceof PoweredMinecart) {
			return ENGINE_DESTINATION;
		} else if (minecart instanceof Minecart) {
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
	public static BlockFace findDestination(final String destination, final Collection<String> lines, final BlockFace direction) {
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
	private static BlockFace computeThreeWayJunction(final BlockFace traveling, final BlockFace open, final BlockFace target) {

		// You can't go off the tracks
		if (target == open) {
			return null;
		}

		return computeFourWayJunction(traveling, target);
	}

	/**
	 * Compute the track direction to take a player moving in one direction
	 * and move him in the target direction
	 * @param direction Direction player is moving
	 * @param target Direction player wants to be moving
	 * @return Direction the junction track should be positioned in.
	 */
	private static BlockFace computeFourWayJunction(final BlockFace direction, final BlockFace target) {

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
	private String entityToDestination(final Entity entity) {
		return entity.getMetadata(TRACKROUTER_DESTINATION).get(0).asString();
	}

	/**
	 * Clear the target destination for a given player.
	 * @param player The player whose destination preference should be cleared
	 * @param verbose Send player a message even if no destination was set
	 */
	public void clearPlayerDestination(final Player player, final boolean verbose) {
		if (entityHasDestination(player)) {
			clearEntityDestination(player);
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
		setEntityDestination(player, normalized);
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
	public void updateJunction(final Minecart minecart, final Junction junction, final BlockFace traveling) {
		final String destination = minecartToPreference(minecart);
		final BlockFace target = findDestination(destination, junction.getLines(), traveling);
		final BlockFace open = junction.getOpenSide();

		final BlockFace newDirection;
		if (open == null) {
			newDirection = computeFourWayJunction(traveling, target);
		} else {
			newDirection = computeThreeWayJunction(traveling, open, target);
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
	public void transferDestination(final Player player, final Entity entity) {

		if (entityHasDestination(player)) {
			final String destination = entityToDestination(player);
			player.sendMessage(ChatColor.GREEN
					+ "Transfering destination preference " + ChatColor.YELLOW
					+ destination + ChatColor.GREEN + " to minecart");
			setEntityDestination(entity, destination);
		} else {
			player.sendMessage(ChatColor.GREEN
					+ "Clearing destination preference on minecart");
			clearEntityDestination(entity);
		}
	}

	/**
	 * Set the destination preference for an cart without a player
	 * @param entityId Identity of the cart
	 * @param destination Destination preference
	 */
	private void setEntityDestination(final Entity entity, final String destination) {
		entity.setMetadata(TRACKROUTER_DESTINATION, new FixedMetadataValue(this, destination));
	}

	/**
	 * Clear the destination preference for a cart without a player
	 * @param entityId Identity of the cart
	 */
	public void clearEntityDestination(final Entity entity) {
		entity.removeMetadata(TRACKROUTER_DESTINATION, this);
	}


	public boolean entityHasDestination(final Entity entity) {
		return entity.hasMetadata(TRACKROUTER_DESTINATION);
	}

	public static boolean isJunctionHeader(final String line) {
		return JUNCTION_HEADER.equalsIgnoreCase(ChatColor.stripColor(line));
	}

	public static boolean isDestinationHeader(final String line) {
		return DESTINATION_HEADER.equalsIgnoreCase(ChatColor.stripColor(line));
	}

	public static boolean playerCanCreateDestinations(Player player) {
		return player.hasPermission("trackrouter.sign.destination.create");
	}

	public static boolean playerCanCreateJunctions(Player player) {
		return player.hasPermission("trackrouter.sign.junction.create");
	}

	public static boolean playerCanUseDestinations(Player player) {
		return player.hasPermission("trackrouter.sign.destination.use");
	}

	public static boolean playerCanUseTransferTool(Player player) {
		return player.hasPermission("trackrouter.transfertool");
	}
}
