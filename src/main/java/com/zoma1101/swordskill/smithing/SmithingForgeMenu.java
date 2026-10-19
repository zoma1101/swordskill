package com.zoma1101.swordskill.smithing;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 鍛冶台（作業台ベース）の GUI。
 *
 * <p>スロット構成:</p>
 * <ul>
 *   <li>0..24 = 5×5 の作業グリッド。置いた形（{@link WeaponPattern}）で作る武器が決まる</li>
 *   <li>25..27 = 補強スロット（溶解インゴット / 特殊素材 / 底上げ素材）</li>
 *   <li>28 = 完成スロット</li>
 * </ul>
 *
 * <p>結果の計算は品質（熟練度）に依存するためサーバー専用で行い、
 * クライアントには通常のスロット同期で配られる。</p>
 */
public class SmithingForgeMenu extends AbstractContainerMenu {

    /** グリッドの一辺。 */
    public static final int GRID_SIZE = 5;
    /** グリッドのスロット数。 */
    public static final int GRID_SLOTS = GRID_SIZE * GRID_SIZE;
    /** 補強スロットの数。 */
    public static final int REINFORCE_COUNT = 3;
    /** グリッドの先頭スロット番号。 */
    public static final int SLOT_GRID = 0;
    /** 補強スロットの先頭スロット番号。 */
    public static final int SLOT_REINFORCE = SLOT_GRID + GRID_SLOTS;
    /** 完成スロットの番号。 */
    public static final int SLOT_RESULT = SLOT_REINFORCE + REINFORCE_COUNT;
    /** プレイヤーインベントリの先頭スロット番号。 */
    public static final int FIRST_PLAYER_SLOT = SLOT_RESULT + 1;

    /** スロット座標。テクスチャの穴はこの座標から 1px 外側。 */
    private static final int GRID_X = 8;
    private static final int GRID_Y = 18;
    private static final int REINFORCE_Y = 124;
    private static final int[] REINFORCE_X = { 8, 26, 44 };
    private static final int RESULT_X = 140;
    private static final int RESULT_Y = 58;

    private final Player owner;
    private final SimpleContainer grid;
    private final SimpleContainer reinforce;
    private final SimpleContainer result = new SimpleContainer(1);
    private boolean refreshing;

    public SmithingForgeMenu(int containerId, Inventory inventory) {
        super(SmithingRegistry.forgeMenu(), containerId);
        this.owner = inventory.player;
        this.grid = new SimpleContainer(GRID_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                onInputsChanged();
            }
        };
        this.reinforce = new SimpleContainer(REINFORCE_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                onInputsChanged();
            }
        };

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int column = 0; column < GRID_SIZE; column++) {
                addSlot(new GridSlot(grid, row * GRID_SIZE + column,
                        GRID_X + column * 18, GRID_Y + row * 18));
            }
        }
        for (int i = 0; i < REINFORCE_COUNT; i++) {
            addSlot(new ReinforceSlot(reinforce, i, REINFORCE_X[i], REINFORCE_Y));
        }
        addSlot(new SmithingResultSlot(result, 0, RESULT_X, RESULT_Y));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 150 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 208));
        }

        refresh();
    }

    // ------------------------------------------------------------------
    // 形と結果
    // ------------------------------------------------------------------

    /** 今のグリッドに一致する形。無ければ null。 */
    public WeaponPattern.Match match() {
        List<ItemStack> stacks = new ArrayList<>(GRID_SLOTS);
        for (int i = 0; i < GRID_SLOTS; i++) {
            stacks.add(grid.getItem(i));
        }
        return SmithingDefinitions.findPattern(stacks, GRID_SIZE, GRID_SIZE);
    }

    private List<ItemStack> reinforcementStacks() {
        List<ItemStack> stacks = new ArrayList<>(REINFORCE_COUNT);
        for (int i = 0; i < REINFORCE_COUNT; i++) {
            stacks.add(reinforce.getItem(i));
        }
        return stacks;
    }

    private void onInputsChanged() {
        if (refreshing || !(owner instanceof ServerPlayer)) {
            return;
        }
        refresh();
    }

    private void refresh() {
        if (!(owner instanceof ServerPlayer)) {
            return;
        }
        boolean previous = refreshing;
        refreshing = true;
        try {
            result.setItem(0, computeResult());
        } finally {
            refreshing = previous;
        }
    }

    private ItemStack computeResult() {
        if (!(owner instanceof ServerPlayer player)) {
            return ItemStack.EMPTY;
        }
        WeaponPattern.Match match = match();
        if (match == null) {
            return ItemStack.EMPTY;
        }
        return SmithingForge.craft(player, match, reinforcementStacks());
    }

    /** 結果を取り出したときに消費する材料（置いたマスと補強スロットから 1 個ずつ）。 */
    private void consumeInputs() {
        for (int i = 0; i < GRID_SLOTS; i++) {
            grid.removeItem(i, 1);
        }
        for (int i = 0; i < REINFORCE_COUNT; i++) {
            reinforce.removeItem(i, 1);
        }
    }

    private static boolean isForgedIngot(ItemStack stack) {
        return !stack.isEmpty() && stack.is(SmithingRegistry.FORGED_INGOT.get());
    }

    // ------------------------------------------------------------------
    // AbstractContainerMenu
    // ------------------------------------------------------------------

    @Override
    public boolean stillValid(Player player) {
        return player == owner && player.isAlive();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            clearContainer(player, grid);
            clearContainer(player, reinforce);
            clearContainer(player, result);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index == SLOT_RESULT) {
            if (!this.moveItemStackTo(stack, FIRST_PLAYER_SLOT, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, copy);
        } else if (index >= FIRST_PLAYER_SLOT) {
            if (!this.moveItemStackTo(stack, SLOT_GRID, SLOT_RESULT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, FIRST_PLAYER_SLOT, this.slots.size(), false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }

    /** 5×5 のマス。素材以外は置けない。 */
    private static class GridSlot extends Slot {

        GridSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return SmithingDefinitions.materialFor(stack) != null;
        }
    }

    /** 補強スロット。溶解インゴットか素材を置ける。 */
    private static class ReinforceSlot extends Slot {

        ReinforceSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isForgedIngot(stack) || SmithingDefinitions.materialFor(stack) != null;
        }
    }

    /** 取り出し専用の出力スロット。取り出すと材料を消費して結果を計算し直す。 */
    private class SmithingResultSlot extends Slot {

        SmithingResultSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            stack.onCraftedBy(player.level(), player, stack.getCount());
            if (player instanceof ServerPlayer) {
                boolean previous = refreshing;
                refreshing = true;
                try {
                    consumeInputs();
                } finally {
                    refreshing = previous;
                }
                refresh();
            }
            super.onTake(player, stack);
        }
    }
}
