package cn.erindax.bcya.manage;

import cn.erindax.bcya.entity.net.PatrollerSettingsHandler;
import cn.erindax.bcya.item.ModItems;
import cn.erindax.bcya.mask.MaskSwapper;
import cn.erindax.bcya.mask.net.MaskSkinHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PlayerPickerMenu extends AbstractContainerMenu {

	private static final int ROWS = 6;
	private static final int SIZE = ROWS * 9;
	private static final int SWAP_SLOT = SIZE - 1;
	private static final int MASK_SKIN_SLOT = SIZE - 2;
	private static final int PATROL_SLOT = SIZE - 3;
	private static final int MAX_PLAYERS = SIZE - 9;

	private final MinecraftServer server;
	private final List<UUID> players = new ArrayList<>();

	public static void open(ServerPlayer viewer) {
		viewer.openMenu(new SimpleMenuProvider(
			(id, inventory, p) -> new PlayerPickerMenu(id, inventory, viewer.server),
			Component.translatable("screen.bcya.players.title")));
	}

	private PlayerPickerMenu(int containerId, Inventory viewerInventory, MinecraftServer server) {
		super(MenuType.GENERIC_9x6, containerId);
		this.server = server;

		SimpleContainer container = new SimpleContainer(SIZE);
		List<ServerPlayer> online = new ArrayList<>(server.getPlayerList().getPlayers());
		online.sort((a, b) -> a.getGameProfile().getName().compareToIgnoreCase(b.getGameProfile().getName()));
		for (int i = 0; i < online.size() && i < MAX_PLAYERS; i++) {
			ServerPlayer target = online.get(i);
			players.add(target.getUUID());
			container.setItem(i, ManagerItems.head(target));
		}
		for (int i = MAX_PLAYERS; i < SIZE; i++) {
			container.setItem(i, ManagerItems.filler());
		}
		container.setItem(SWAP_SLOT, ManagerItems.button(ModItems.MAGIC_WAND,
			"screen.bcya.players.swap", "screen.bcya.players.swap_lore"));
		container.setItem(MASK_SKIN_SLOT, ManagerItems.button(ModItems.CAT_MASK,
			"screen.bcya.players.mask_skins", "screen.bcya.players.mask_skins_lore"));
		container.setItem(PATROL_SLOT, ManagerItems.button(Items.ARMOR_STAND,
			"screen.bcya.players.patrollers", "screen.bcya.players.patrollers_lore"));

		for (int i = 0; i < SIZE; i++) {
			addSlot(new LockedSlot(container, i));
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new LockedSlot(viewerInventory, col + row * 9 + 9));
			}
		}
		for (int col = 0; col < 9; col++) {
			addSlot(new LockedSlot(viewerInventory, col));
		}
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		if (!(player instanceof ServerPlayer viewer) || clickType != ClickType.PICKUP || slotId < 0) {
			return;
		}
		if (slotId == SWAP_SLOT) {
			int swapped = MaskSwapper.swapAll(server, viewer.getRandom());
			viewer.displayClientMessage(swapped > 0
				? Component.translatable("item.bcya.magic_wand.swapped", swapped)
				: Component.translatable("item.bcya.magic_wand.nothing"), true);
			return;
		}
		if (slotId == MASK_SKIN_SLOT) {
			viewer.closeContainer();
			MaskSkinHandler.openEditor(viewer);
			return;
		}
		if (slotId == PATROL_SLOT) {
			viewer.closeContainer();
			PatrollerSettingsHandler.openList(viewer);
			return;
		}
		if (slotId < players.size()) {
			ServerPlayer target = server.getPlayerList().getPlayer(players.get(slotId));
			if (target != null) {
				PlayerInventoryMenu.open(viewer, target);
			} else {
				open(viewer);
			}
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return WandWhitelist.isAllowed(player);
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return false;
	}
}
