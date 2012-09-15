package com.gmail.emertens.pdxtrackrouter.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.gmail.emertens.pdxtrackrouter.BlockFaceUtils;
import com.gmail.emertens.pdxtrackrouter.events.VehicleMoveBlockEvent;

public final class ChestTransferListener implements Listener {

	private final Material UNLOAD_TRIGGER_BLOCK;
	private final Material LOAD_TRIGGER_BLOCK;

	public ChestTransferListener(final Material loadTrigger, final Material unloadTrigger) {
		UNLOAD_TRIGGER_BLOCK = unloadTrigger;
		LOAD_TRIGGER_BLOCK = loadTrigger;
	}

	@EventHandler(ignoreCancelled = true)
	public void onVehicleMove(final VehicleMoveBlockEvent event) {
		final Block to = event.getBlock();
		final Vehicle vehicle = event.getVehicle();

		if (vehicle instanceof StorageMinecart) {
			storageCartMove((StorageMinecart) vehicle, to);
		}
	}

	public void storageCartMove(StorageMinecart cart, Block block) {
		final Block under = block.getRelative(BlockFace.DOWN);
		final Material underType = under.getType();
		final boolean loadCart;

		if (underType == UNLOAD_TRIGGER_BLOCK) {
			loadCart = false;
		} else if (underType == LOAD_TRIGGER_BLOCK) {
			loadCart = true;
		} else {
			return;
		}

		final Inventory cartInventory = cart.getInventory();

		for (BlockFace dir : BlockFaceUtils.CARDINAL_DIRECTIONS) {
			final Block neighbor = under.getRelative(dir);
			final BlockState state = neighbor.getState();
			if (state instanceof Chest) {
				final Chest chest = (Chest) state;
				final Inventory chestInventory = chest.getInventory();

				if (loadCart) {
					transferInventory(chestInventory, cartInventory);
				} else {
					transferInventory(cartInventory, chestInventory);
				}

				state.update();
			}
		}
	}

	/**
	 * Transfer as many items from one inventory to another as possible.
	 * @param source Inventory to remove items from
	 * @param target Inventory to add items to
	 */
	private static void transferInventory(final Inventory source,
			final Inventory target) {
		final int slots = source.getSize();
		for (int slot = 0; slot < slots; slot++) {
			final ItemStack x = source.getItem(slot);
			if (x != null) {
				final ItemStack remainder = target.addItem(x).get(0);
				source.setItem(slot, remainder);
			}
		}
	}
}
