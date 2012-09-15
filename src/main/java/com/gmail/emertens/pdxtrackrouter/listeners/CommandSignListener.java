package com.gmail.emertens.pdxtrackrouter.listeners;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.gmail.emertens.pdxtrackrouter.events.PlayerUseCommandSignEvent;

/**
 * This listener generates {@link PlayerUseCommandSignEvent} events.
 * @author Eric Mertens
 */
public final class CommandSignListener implements Listener {

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		final Block block = event.getClickedBlock();
		final BlockState state = block.getState();
		if (state instanceof Sign) {
			final Sign sign = (Sign)state;

			PlayerUseCommandSignEvent subevent = new PlayerUseCommandSignEvent(event.getPlayer(), block, sign);
			Bukkit.getServer().getPluginManager().callEvent(subevent);
			event.setCancelled(subevent.isCancelled());
		}
	}
}
