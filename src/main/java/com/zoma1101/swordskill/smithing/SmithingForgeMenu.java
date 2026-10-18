package com.zoma1101.swordskill.smithing;

import com.zoma1101.swordskill.config.ServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 鍛冶台の GUI。作成・強化・溶解・再鍛造をモードで切り替える。
 *
 * <p>スロット構成（入力 5 + 出力 1）:</p>
 * <ul>
 *   <li>作成: 0 = メイン素材 / 1..4 = 対応素材 / 5 = 完成</li>
 *   <li>強化: 0 = 対象の武器 / 1 = 強化素材 / 5 = 完成</li>
 *   <li>溶解: 0 = 対象の武器 / 5 = 完成</li>
 *   <li>再鍛造: 0 = インゴット / 1 = メイン素材 / 5 = 完成</li>
 * </ul>
 *
 * <p>メイン素材を入れると、その素材で作れるレシピ（= 武器）が一覧になる。
 * レシピを選ぶと対応素材がゴースト表示され、揃うと完成スロットに品が出る。</p>
 *
 * <p>結果の計算は品質（熟練度）に依存するためサーバー専用で行い、
 * クライアントには通常のスロット同期で配られる。選択中のレシピ番号だけは
 * {@link ContainerData} で常にサーバーの値へ合わせる。</p>
 */
public class SmithingForgeMenu extends AbstractContainerMenu {

    /** メイン素材（作成）、対象（強化・溶解）、インゴット（再鍛造）。 */
    public static final int SLOT_MAIN = 0;
    /** 対応素材の先頭スロット番号。 */
    public static final int SLOT_EXTRA = 1;
    /** 対応素材スロット数。 */
    public static final int EXTRA_COUNT = SmithingRecipe.MAX_INGREDIENTS;
    /** 入力スロット数（メイン + 対応素材）。 */
    public static final int INPUT_COUNT = 1 + EXTRA_COUNT;
    public static final int SLOT_RESULT = INPUT_COUNT;
    /** プレイヤーインベントリの先頭スロット番号。 */
    public static final int FIRST_PLAYER_SLOT = INPUT_COUNT + 1;

    /** スロット座標（{@code Slot.x/y} は final なので、全モードで同じ位置を使い意味だけ変える）。 */
    private static final int[][] INPUT_POSITIONS = {
            {8, 34}, {100, 56}, {118, 56}, {100, 74}, {118, 74}
    };
    private static final int RESULT_X = 146;
    private static final int RESULT_Y = 56;

    private final Player owner;
    private final SimpleContainer inputs;
    private final SimpleContainer result = new SimpleContainer(1);
    private SmithingMode mode = SmithingMode.CREATE;
    private int selected;
    private boolean refreshing;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? selected : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                selected = value;
            }
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public SmithingForgeMenu(int containerId, Inventory inventory) {
        super(SmithingRegistry.forgeMenu(), containerId);
        this.owner = inventory.player;
        this.inputs = new SimpleContainer(INPUT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                onInputsChanged();
            }
        };

        for (int i = 0; i < INPUT_COUNT; i++) {
            int[] position = INPUT_POSITIONS[Math.min(i, INPUT_POSITIONS.length - 1)];
            addSlot(new InputSlot(inputs, i, position[0], position[1]));
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

        addDataSlots(this.dataAccess);
        refresh();
    }

    // ------------------------------------------------------------------
    // モードとスロットの有効範囲
    // ------------------------------------------------------------------

    public SmithingMode getMode() {
        return mode;
    }

    /** モードを切り替える。使わないスロットの中身はプレイヤーに返す。 */
    public void setMode(SmithingMode newMode) {
        SmithingMode next = newMode == null ? SmithingMode.CREATE : newMode;
        if (next == mode) {
            return;
        }
        ejectInactiveInputs();
        this.mode = next;
        refresh();
    }

    /** そのモードで使う入力スロットか。 */
    private boolean isInputActive(int index) {
        return switch (mode) {
            case CREATE -> true;
            case ENHANCE, REFORGE -> index <= SLOT_EXTRA;
            case MELT -> index == SLOT_MAIN;
        };
    }

    private void ejectInactiveInputs() {
        if (!(owner instanceof ServerPlayer player)) {
            return;
        }
        for (int i = 0; i < INPUT_COUNT; i++) {
            if (isInputActive(i)) {
                continue;
            }
            ItemStack stack = inputs.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            inputs.setItem(i, ItemStack.EMPTY);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    // ------------------------------------------------------------------
    // レシピ
    // ------------------------------------------------------------------

    /**
     * メイン素材の入ったスロット。
     *
     * <p>作成では先頭がメイン素材、強化では強化素材、再鍛造ではインゴットの隣がメイン素材。</p>
     */
    private int materialSlot() {
        return switch (mode) {
            case ENHANCE, REFORGE -> SLOT_EXTRA;
            case CREATE, MELT -> SLOT_MAIN;
        };
    }

    private ItemStack materialStack() {
        return inputs.getItem(materialSlot());
    }

    /** メイン素材として判定された素材（無ければ null）。 */
    public SmithingMaterial material() {
        return SmithingDefinitions.materialFor(materialStack());
    }

    /**
     * 選べるレシピ（サーバー・クライアントで同じ並び）。
     *
     * <p>レシピ一覧を使うのは作成と再鍛造だけ。強化・溶解では一覧を出さない。</p>
     */
    public List<SmithingRecipe> recipes() {
        if (mode != SmithingMode.CREATE && mode != SmithingMode.REFORGE) {
            return List.of();
        }
        return SmithingDefinitions.recipesFor(material());
    }

    /** 選択中のレシピ。無ければ null。 */
    public SmithingRecipe selectedRecipe() {
        List<SmithingRecipe> list = recipes();
        return selected >= 0 && selected < list.size() ? list.get(selected) : null;
    }

    public int getSelectedIndex() {
        return selected;
    }

    /** レシピ一覧の選択（クライアントからのリクエスト）。 */
    public void selectRecipe(int index) {
        List<SmithingRecipe> list = recipes();
        if (list.isEmpty()) {
            return;
        }
        int clamped = Mth.clamp(index, 0, list.size() - 1);
        if (clamped == selected) {
            return;
        }
        selected = clamped;
        refresh();
        if (owner instanceof ServerPlayer) {
            broadcastChanges();
        }
    }

    // ------------------------------------------------------------------
    // 結果の計算
    // ------------------------------------------------------------------

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
            List<SmithingRecipe> list = recipes();
            if (!list.isEmpty()) {
                selected = Mth.clamp(selected, 0, list.size() - 1);
            }
            result.setItem(0, computeResult());
        } finally {
            refreshing = previous;
        }
    }

    private ItemStack computeResult() {
        if (!(owner instanceof ServerPlayer player)) {
            return ItemStack.EMPTY;
        }
        SmithingMaterial material = material();

        return switch (mode) {
            case CREATE -> {
                SmithingRecipe recipe = selectedRecipe();
                if (material == null || recipe == null || !extrasSatisfied(recipe)
                        || inputs.getItem(SLOT_MAIN).getCount() < recipe.count()) {
                    yield ItemStack.EMPTY;
                }
                yield SmithingForge.create(player, material, recipe, null);
            }
            case ENHANCE -> {
                ItemStack target = inputs.getItem(SLOT_MAIN);
                ForgedWeaponData data = isForgedWeapon(target) ? ForgedWeaponData.get(target) : null;
                if (data == null || material == null || !material.id().equals(data.material())
                        || inputs.getItem(SLOT_EXTRA).getCount() < SmithingForge.enhancementCost()) {
                    yield ItemStack.EMPTY;
                }
                yield SmithingForge.previewEnhance(target);
            }
            case MELT -> {
                ItemStack target = inputs.getItem(SLOT_MAIN);
                yield isForgedWeapon(target) ? SmithingForge.melt(target) : ItemStack.EMPTY;
            }
            case REFORGE -> {
                ItemStack target = inputs.getItem(SLOT_MAIN);
                SmithingRecipe recipe = selectedRecipe();
                if (material == null || recipe == null
                        || !target.is(SmithingRegistry.FORGED_INGOT.get())
                        || ForgedWeaponData.get(target) == null
                        || materialStack().getCount() < recipe.count()) {
                    yield ItemStack.EMPTY;
                }
                yield SmithingForge.reforge(player, target, material, recipe);
            }
        };
    }

    /** 対応素材が揃っているか。 */
    private boolean extrasSatisfied(SmithingRecipe recipe) {
        List<SmithingIngredient> needed = recipe.materials();
        if (needed.size() > EXTRA_COUNT) {
            return false;
        }
        for (int i = 0; i < needed.size(); i++) {
            SmithingIngredient ingredient = needed.get(i);
            ItemStack stack = inputs.getItem(SLOT_EXTRA + i);
            if (!ingredient.matches(stack) || stack.getCount() < ingredient.count()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isForgedWeapon(ItemStack stack) {
        return !stack.isEmpty()
                && stack.is(SmithingRegistry.FORGED_WEAPON.get())
                && ForgedWeaponData.get(stack) != null;
    }

    /** 結果を取り出したときに消費する材料。 */
    private void consumeInputs() {
        switch (mode) {
            case CREATE -> {
                SmithingRecipe recipe = selectedRecipe();
                if (recipe == null) {
                    return;
                }
                inputs.removeItem(SLOT_MAIN, recipe.count());
                List<SmithingIngredient> needed = recipe.materials();
                for (int i = 0; i < needed.size() && i < EXTRA_COUNT; i++) {
                    inputs.removeItem(SLOT_EXTRA + i, needed.get(i).count());
                }
            }
            case ENHANCE -> {
                inputs.removeItem(SLOT_EXTRA, SmithingForge.enhancementCost());
                inputs.removeItem(SLOT_MAIN, 1);
            }
            case MELT -> inputs.removeItem(SLOT_MAIN, 1);
            case REFORGE -> {
                SmithingRecipe recipe = selectedRecipe();
                if (recipe != null) {
                    inputs.removeItem(SLOT_EXTRA, recipe.count());
                }
                inputs.removeItem(SLOT_MAIN, 1);
            }
        }
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
            clearContainer(player, inputs);
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
            if (!this.moveItemStackTo(stack, SLOT_MAIN, SLOT_RESULT, false)) {
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

    /** 入力スロット。使わないモードでは非表示で、物を置けない。 */
    private class InputSlot extends Slot {

        private final int index;

        InputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
            this.index = index;
        }

        @Override
        public boolean isActive() {
            return isInputActive(index);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!isInputActive(index)) {
                return false;
            }
            return switch (mode) {
                case CREATE -> switch (index) {
                    case SLOT_MAIN -> SmithingDefinitions.materialFor(stack) != null;
                    default -> true;
                };
                case ENHANCE -> index == SLOT_MAIN
                        ? isForgedWeapon(stack)
                        : SmithingDefinitions.materialFor(stack) != null;
                case MELT -> index == SLOT_MAIN && isForgedWeapon(stack);
                case REFORGE -> index == SLOT_MAIN
                        ? stack.is(SmithingRegistry.FORGED_INGOT.get())
                        : SmithingDefinitions.materialFor(stack) != null;
            };
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
