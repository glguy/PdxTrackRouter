package com.gmail.emertens.pdxtrackrouter.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.gmail.emertens.pdxtrackrouter.BlockFaceUtils;
import com.gmail.emertens.pdxtrackrouter.events.VehicleMoveBlockEvent;

/**
 * This class implements storage cart transfers to chests when
 * a cart passes over specially designated blocks.
 * @author Eric Mertens
 *
 */
public final class ChestTransferListener implements Listener {

	private final Material UNLOAD_TRIGGER_BLOCK;
	private final Material LOAD_TRIGGER_BLOCK;

	/**
	 * Construct a new ChestTransferListener using the given tigger materials.
	 * @param loadTrigger Material which triggers chest load behavior
	 * @param unloadTrigger Material which triggers chest unload behavior
	 */
	public ChestTransferListener(final Material loadTrigger, final Material unloadTrigger) {
		UNLOAD_TRIGGER_BLOCK = unloadTrigger;
		LOAD_TRIGGER_BLOCK = loadTrigger;
	}

	@EventHandler(ignoreCancelled = true)
	public void onVehicleMove(final VehicleMoveBlockEvent event) {

		final Vehicle vehicle = event.getVehicle();
		if (!(vehicle instanceof StorageMinecart)) {
			return;
		}
		final StorageMinecart cart = (StorageMinecart) vehicle;

		final Block block = event.getBlock();
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
	private static void transferInventory(final Inventory source, final Inventory target) {
		final int slots = source.getSize();
		for (int slot = 0; slot < slots; slot++) {
			final ItemStack x = source.getItem(slot);
			if (x != null) {
				source.setItem(slot, target.addItem(x).get(0));
			}
		}
	}
}
