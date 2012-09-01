package com.gmail.emertens.pdxtrackrouter;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * This listener watches for operations on destination signs,
 * prevents blocks from being placed against them, and notifies
 * the plug-in when a player uses such a sign.
 * @author Eric Mertens
 */
public final class PlayerListener implements Listener {

	private final PdxTrackRouter plugin;

	public PlayerListener(final PdxTrackRouter p) {
		plugin = p;
	}

	/**
	 * Intercept block placements against destination signs.
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(final BlockPlaceEvent event) {
		final Block block = event.getBlockAgainst();
		final BlockState state = block.getState();
		if (state instanceof Sign) {
			final Sign sign = (Sign)state;

			if (PdxTrackRouter.DESTINATION_HEADER.equalsIgnoreCase(ChatColor.stripColor(sign.getLine(0)))) {
				event.setCancelled(true);
			}
		}
	}

	/**
	 * Detect uses of destination signs and forward destination to plug-in.
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteract(final PlayerInteractEvent event) {

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		final Block block = event.getClickedBlock();
		final BlockState state = block.getState();
		if (state instanceof Sign) {
			final Sign sign = (Sign)state;

			if (PdxTrackRouter.DESTINATION_HEADER.equalsIgnoreCase(ChatColor.stripColor(sign.getLine(0)))) {
				plugin.setPlayerDestination(event.getPlayer(), sign.getLine(1));
			}
		}
	}

	/**
	 * Change command sign headers to blue to give users a sense of feedback
	 */
	@EventHandler(ignoreCancelled = true)
	public void onSignChange(final SignChangeEvent event) {
		final String header = event.getLine(0);
		if (PdxTrackRouter.JUNCTION_HEADER.equalsIgnoreCase(header)
				|| PdxTrackRouter.DESTINATION_HEADER.equalsIgnoreCase(header)) {
			event.setLine(0, ChatColor.DARK_BLUE + header);
		}
	}
}
