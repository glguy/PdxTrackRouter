package com.gmail.emertens.pdxtrackrouter.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import com.gmail.emertens.pdxtrackrouter.PdxTrackRouter;
import com.gmail.emertens.pdxtrackrouter.events.PlayerUseCommandSignEvent;

/**
 * This listener watches for operations on destination signs,
 * prevents blocks from being placed against them, and notifies
 * the plug-in when a player uses such a sign.
 * @author Eric Mertens
 */
public final class PlayerListener implements Listener {

	private final PdxTrackRouter plugin;
	private final Material TRANSFER_TOOL;

	public PlayerListener(final PdxTrackRouter p, final Material transferTool) {
		plugin = p;
		this.TRANSFER_TOOL = transferTool;
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

			if (PdxTrackRouter.isDestinationHeader(sign.getLine(0))) {
				event.setCancelled(true);
			}
		}
	}

	/**
	 * Detect uses of destination signs and forward destination to plug-in.
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerUseCommandSign(final PlayerUseCommandSignEvent event) {
		final Sign sign = event.getSign();
		if (PdxTrackRouter.isDestinationHeader(sign.getLine(0))) {
			final Player player = event.getPlayer();
			if (PdxTrackRouter.playerCanUseDestinations(player)) {
				plugin.setPlayerDestination(player, sign.getLine(1));
			} else {
				player.sendMessage(ChatColor.RED + "Permission denied");
			}
		}
	}

	/**
	 * Handle the event when a player right clicks on a minecart with the
	 * transfer tool. Transfer the player's destination preference to the
	 * minecart.
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntityInteract(final PlayerInteractEntityEvent event) {

		if (event.getPlayer().getInventory().getItemInMainHand().getType() != TRANSFER_TOOL) {
			return;
		}

		final Entity entity = event.getRightClicked();
		if (!(entity instanceof Minecart)) {
			return;
		}

		final Player player = event.getPlayer();
		if (PdxTrackRouter.playerCanUseTransferTool(player)) {
			final Minecart minecart = (Minecart) entity;
			minecart.setSlowWhenEmpty(false);
			boolean hasDestination = plugin.transferDestination(player, entity);
			minecart.setSlowWhenEmpty(!hasDestination);
			event.setCancelled(true);
		}
	}

	/**
	 * Change command sign headers to blue to give users a sense of feedback
	 */
	@EventHandler(ignoreCancelled = true)
	public void onSignChange(final SignChangeEvent event) {
		final String header = event.getLine(0);
		final Player player = event.getPlayer();
		boolean eraseHeader = false;
		boolean colorHeader = false;

		if (PdxTrackRouter.isDestinationHeader(header)) {
			if (PdxTrackRouter.playerCanCreateDestinations(player)) {
				colorHeader = true;
			} else {
				eraseHeader = true;
			}
		} else if (PdxTrackRouter.isJunctionHeader(header)) {
			if (PdxTrackRouter.playerCanCreateJunctions(player)) {
				colorHeader = true;
			} else {
				eraseHeader = true;
			}
		}

		if (colorHeader) {
			event.setLine(0, ChatColor.DARK_BLUE + ChatColor.stripColor(header));
		} else if (eraseHeader) {
			event.setLine(0, "");
		}
	}
}
