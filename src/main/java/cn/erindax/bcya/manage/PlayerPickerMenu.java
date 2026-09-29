package cn.erindax.bcya.manage;

import cn.erindax.bcya.entity.net.PatrollerSettingsHandler;
import cn.erindax.bcya.item.ModItems;
import cn.erindax.bcya.mask.MaskSwapper;
import cn.erindax.bcya.mask.net.MaskRulesHandler;
import cn.erindax.bcya.mask.net.MaskSkinHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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

import org.jetbrains.annotations.Nullable;

public class PlayerPickerMenu extends AbstractContainerMenu {

	private static final int ROWS = 6;
	private static final int SIZE = ROWS * 9;
	private static final int PAGE_SIZE = SIZE - 9;
	private static final int GROUP_SLOT = SIZE - 9;
	private static final int PREV_SLOT = SIZE - 8;
	private static final int NEXT_SLOT = SIZE - 7;
	private static final int MASK_RULES_SLOT = SIZE - 4;
	private static final int PATROL_SLOT = SIZE - 3;
	private static final int MASK_SKIN_SLOT = SIZE - 2;
	private static final int SWAP_SLOT = SIZE - 1;
	private static final String UNGROUPED = "";
	private static final Map<UUID, View> VIEWS = new HashMap<>();

	private record View(@Nullable String group, int page) {
	}

	private final MinecraftServer server;
	private final UUID viewerId;
	private final SimpleContainer container = new SimpleContainer(SIZE);
	private final List<UUID> players = new ArrayList<>();
	@Nullable
	private String group;
	private int page;

	public static void open(ServerPlayer viewer) {
		viewer.openMenu(new SimpleMenuProvider(
			(id, inventory, p) -> new PlayerPickerMenu(id, inventory, viewer),
			Component.translatable("screen.bcya.players.title")));
	}

	private PlayerPickerMenu(int containerId, Inventory viewerInventory, ServerPlayer viewer) {
		super(MenuType.GENERIC_9x6, containerId);
		this.server = viewer.server;
		this.viewerId = viewer.getUUID();
		View view = VIEWS.get(viewerId);
		if (view != null) {
			group = view.group();
			page = view.page();
		}

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
		refresh();
	}

	private void refresh() {
		WandRosterState roster = WandRosterState.get(server);
		List<ServerPlayer> visible = visible(roster);
		List<String> groups = groups(roster, visible);
		if (!groups.contains(group)) {
			group = null;
			page = 0;
		}
		List<ServerPlayer> shown = new ArrayList<>();
		for (ServerPlayer target : visible) {
			if (group == null || roster.groupOf(target.getUUID()).equals(group)) {
				shown.add(target);
			}
		}
		int pages = Math.max(1, (shown.size() + PAGE_SIZE - 1) / PAGE_SIZE);
		page = Mth.clamp(page, 0, pages - 1);
		VIEWS.put(viewerId, new View(group, page));

		players.clear();
		for (int i = 0; i < PAGE_SIZE; i++) {
			int index = page * PAGE_SIZE + i;
			if (index < shown.size()) {
				ServerPlayer target = shown.get(index);
				players.add(target.getUUID());
				container.setItem(i, ManagerItems.head(target, roster.groupOf(target.getUUID())));
			} else {
				container.setItem(i, ItemStack.EMPTY);
			}
		}
		for (int i = PAGE_SIZE; i < SIZE; i++) {
			container.setItem(i, ManagerItems.filler());
		}
		container.setItem(GROUP_SLOT, ManagerItems.button(Items.NAME_TAG,
			Component.translatable("screen.bcya.players.group", groupName(group)),
			Component.translatable("screen.bcya.players.group_lore")));
		Component pageLabel = Component.translatable("screen.bcya.players.page", page + 1, pages);
		if (page > 0) {
			container.setItem(PREV_SLOT, ManagerItems.button(Items.ARROW,
				Component.translatable("screen.bcya.players.prev"), pageLabel));
		}
		if (page < pages - 1) {
			container.setItem(NEXT_SLOT, ManagerItems.button(Items.SPECTRAL_ARROW,
				Component.translatable("screen.bcya.players.next"), pageLabel));
		}
		container.setItem(MASK_RULES_SLOT, ManagerItems.button(Items.WRITABLE_BOOK,
			"screen.bcya.players.mask_rules", "screen.bcya.players.mask_rules_lore"));
		container.setItem(PATROL_SLOT, ManagerItems.button(Items.ARMOR_STAND,
			"screen.bcya.players.patrollers", "screen.bcya.players.patrollers_lore"));
		container.setItem(MASK_SKIN_SLOT, ManagerItems.button(ModItems.CAT_MASK,
			"screen.bcya.players.mask_skins", "screen.bcya.players.mask_skins_lore"));
		container.setItem(SWAP_SLOT, ManagerItems.button(ModItems.MAGIC_WAND,
			"screen.bcya.players.swap", "screen.bcya.players.swap_lore"));
	}

	private List<ServerPlayer> visible(WandRosterState roster) {
		List<ServerPlayer> visible = new ArrayList<>();
		for (ServerPlayer target : server.getPlayerList().getPlayers()) {
			if (!roster.isHidden(target.getUUID())) {
				visible.add(target);
			}
		}
		visible.sort((a, b) -> a.getGameProfile().getName().compareToIgnoreCase(b.getGameProfile().getName()));
		return visible;
	}

	private static List<String> groups(WandRosterState roster, List<ServerPlayer> visible) {
		TreeSet<String> named = new TreeSet<>();
		boolean ungrouped = false;
		for (ServerPlayer target : visible) {
			String name = roster.groupOf(target.getUUID());
			if (name.isEmpty()) {
				ungrouped = true;
			} else {
				named.add(name);
			}
		}
		List<String> groups = new ArrayList<>();
		groups.add(null);
		groups.addAll(named);
		if (ungrouped && !named.isEmpty()) {
			groups.add(UNGROUPED);
		}
		return groups;
	}

	private static Component groupName(@Nullable String group) {
		if (group == null) {
			return Component.translatable("screen.bcya.players.group_all");
		}
		return group.isEmpty() ? Component.translatable("screen.bcya.players.group_none") : Component.literal(group);
	}

	private void cycle(int step) {
		WandRosterState roster = WandRosterState.get(server);
		List<String> groups = groups(roster, visible(roster));
		int index = Math.max(0, groups.indexOf(group));
		group = groups.get(Math.floorMod(index + step, groups.size()));
		page = 0;
		refresh();
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		if (!(player instanceof ServerPlayer viewer) || clickType != ClickType.PICKUP || slotId < 0) {
			return;
		}
		if (slotId == GROUP_SLOT) {
			cycle(button == 1 ? -1 : 1);
			return;
		}
		if (slotId == PREV_SLOT || slotId == NEXT_SLOT) {
			page += slotId == NEXT_SLOT ? 1 : -1;
			refresh();
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
		if (slotId == MASK_RULES_SLOT) {
			viewer.closeContainer();
			MaskRulesHandler.openEditor(viewer);
			return;
		}
		if (slotId < players.size()) {
			ServerPlayer target = server.getPlayerList().getPlayer(players.get(slotId));
			if (target != null && !WandRosterState.get(server).isHidden(target.getUUID())) {
				PlayerInventoryMenu.open(viewer, target);
			} else {
				refresh();
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
