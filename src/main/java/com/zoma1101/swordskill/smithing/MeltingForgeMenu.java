package com.zoma1101.swordskill.smithing;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 溶解炉の GUI。鍛造武器を入れるとインゴットになる。
 *
 * <p>スロット構成: 0 = 入れる武器 / 1 = できたインゴット。</p>
 */
public class MeltingForgeMenu extends AbstractContainerMenu {

    /** 入れる武器のスロット。 */
    public static final int SLOT_INPUT = 0;
    /** できたインゴットのスロット。 */
    public static final int SLOT_RESULT = 1;
    /** プレイヤーインベントリの先頭スロット番号。 */
    public static final int FIRST_PLAYER_SLOT = 2;

    private static final int INPUT_X = 56;
    private static final int INPUT_Y = 35;
    private static final int RESULT_X = 116;
    private static final int RESULT_Y = 35;

    private final Player owner;
    private final SimpleContainer input;
    private final SimpleContainer result = new SimpleContainer(1);
    private boolean refreshing;

    public MeltingForgeMenu(int containerId, Inventory inventory) {
        super(SmithingRegistry.meltingMenu(), containerId);
        this.owner = inventory.player;
        this.input = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                refresh();
            }
        };

        addSlot(new Slot(input, SLOT_INPUT, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isForgedWeapon(stack);
            }
        });
        addSlot(new ResultSlot(result, 0, RESULT_X, RESULT_Y));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }

        refresh();
    }

    private static ItemStack melt(ItemStack stack) {
        return isForgedWeapon(stack) ? SmithingForge.melt(stack) : ItemStack.EMPTY;
    }

    private static boolean isForgedWeapon(ItemStack stack) {
        return !stack.isEmpty()
                && stack.is(SmithingRegistry.FORGED_WEAPON.get())
                && ForgedWeaponData.get(stack) != null;
    }

    private void refresh() {
        if (!(owner instanceof ServerPlayer)) {
            return;
        }
        boolean previous = refreshing;
        refreshing = true;
        try {
            result.setItem(0, melt(input.getItem(SLOT_INPUT)));
        } finally {
            refreshing = previous;
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && player.isAlive();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            clearContainer(player, input);
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
            if (!this.moveItemStackTo(stack, SLOT_INPUT, SLOT_RESULT, false)) {
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

    /** 取り出し専用の出力スロット。取り出すと入れた武器を消費する。 */
    private class ResultSlot extends Slot {

        ResultSlot(Container container, int slot, int x, int y) {
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
                    input.removeItem(SLOT_INPUT, 1);
                } finally {
                    refreshing = previous;
                }
                refresh();
            }
            super.onTake(player, stack);
        }
    }
}
