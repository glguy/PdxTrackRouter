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
	
	private TrackListener trackListener;
	private Map<String,String> playerTargets = new HashMap<String, String>();
	
	@Override
	public void onEnable() {
		trackListener = new TrackListener(this);
		getServer().getPluginManager().registerEvents(trackListener, this);
	}
	
	@Override
	public void onDisable() {
		playerTargets.clear();
		trackListener = null;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		
		final boolean success;
		
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (args.length == 1) {
				playerTargets.put(player.getName(), args[0]);
				sender.sendMessage(ChatColor.GREEN + "Junction set to "
						+ ChatColor.YELLOW + args[0]);
				success = true;
			} else if (args.length == 0) {
				playerTargets.remove(player.getName());
				sender.sendMessage(ChatColor.GREEN + "Junction cleared");
				success = true;
			} else {
				success = false;
			}
		} else {
			success = true;
			sender.sendMessage(ChatColor.RED + "Console can't use junction");
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
		
		String destination = playerTargets.get(player.getName());
		if (destination == null) destination = "default";
		
		BlockFace target = findDestination(destination, lines);
		if (target == null) return;
		
		BlockState state = block.getState();
		Rails rails = (Rails)state.getData();
		BlockFace newDirection = computeJunction(traveling, open, target);
		
		if (newDirection != null) {
			rails.setDirection(newDirection, false);
			state.setData(rails);
			state.update();
		}
	} 

	/**
	 * Find a target direction given a junction sign line array and a destination name.
	 * @param destination Destination label to search for
	 * @param lines Lines of sign to search in
	 * @return first matching direction or first default direction
	 */
	private static BlockFace findDestination(String destination, String[] lines) {
		final String prefix = destination.toLowerCase() + ":";
		final String defaultPrefix = "default:";
		
		for (int i = 1; i < lines.length; i++) {
			String current = lines[i].toLowerCase();
			if (current.startsWith(prefix)) {
				return charToDirection(lines[i].substring(prefix.length()).trim());
			} else if (current.startsWith(defaultPrefix)) {
				return charToDirection(lines[i].substring(defaultPrefix.length()).trim());
			}
		}
		return null;
	}
	
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
	 * Compute the new direction a track should face
	 * @param traveling The direction the player is going
	 * @param open The direction that has no track
	 * @param target The direction the player wants to go
	 * @return The direction the track should be changed to
	 */
	public static BlockFace computeJunction(BlockFace traveling, BlockFace open, BlockFace target) {

		// You can't turn around
		if (traveling == opposite(target)) return null;
		
		// You can't go off the tracks
		if (target == open) return null;
		
		// Heading into a T junction
		if (traveling == open) return addFaces(opposite(target), open); 
			
		// Continuing straight through a junction
		if (traveling == target) return addFaces(opposite(target), open);
		
		// Turning into a junction
		return addFaces(traveling, open);
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
}
