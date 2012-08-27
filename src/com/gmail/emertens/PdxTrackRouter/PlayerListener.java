package com.gmail.emertens.PdxTrackRouter;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class PlayerListener implements Listener {

	private PdxTrackRouter plugin;
	private String header;

	public PlayerListener(PdxTrackRouter p, String header) {
		plugin = p;
		this.header = header;
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlockAgainst();
		BlockState state = block.getState();
		if (state instanceof Sign) {
			Sign sign = (Sign)state;

			if (header.equalsIgnoreCase(ChatColor.stripColor(sign.getLine(0)))) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

		Block block = event.getClickedBlock();
		BlockState state = block.getState();
		if (state instanceof Sign) {
			Sign sign = (Sign)state;

			if (header.equalsIgnoreCase(ChatColor.stripColor(sign.getLine(0)))) {
				plugin.setPlayerDestination(event.getPlayer(), ChatColor.stripColor(sign.getLine(1)));
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onVehicleExit(VehicleExitEvent event) {
		if (event.getVehicle().getType() != EntityType.MINECART) return;
		LivingEntity entity = event.getExited();
		if (entity instanceof Player) {
			plugin.clearPlayerDestination((Player)entity);
		}
	}
}
