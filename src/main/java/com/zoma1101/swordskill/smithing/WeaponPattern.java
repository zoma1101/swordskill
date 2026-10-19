package com.zoma1101.swordskill.smithing;

import com.zoma1101.swordskill.swordskills.SkillData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 武器の形（5×5 の shaped レシピ）。データパックの
 * {@code data/<namespace>/weapon_pattern/<id>.json} から読み込む。
 *
 * <p>形 1 つが武器 1 種に対応し、置いたマスの役割（{@link SmithingRole}）ごとに
 * 使う部品テクスチャが決まる。刀身・鍔・柄は既定値を持ち、
 * 素材側の {@code blades} / {@code guard} / {@code handle} で上書きできる。</p>
 *
 * @param id      パターン ID
 * @param rows    形（スペースは空きマス。5×5 まで）
 * @param key     マスの文字 → 役割と受け付ける素材
 * @param weapon  完成する武器種
 * @param blade   既定の刀身
 * @param guard   既定の鍔
 * @param handle  既定の柄
 */
public record WeaponPattern(
        ResourceLocation id,
        List<String> rows,
        Map<Character, Cell> key,
        SkillData.WeaponType weapon,
        ResourceLocation blade,
        ResourceLocation guard,
        ResourceLocation handle) {

    /** 行の長さを揃えておく（短い行は空きマスで埋める）。 */
    public WeaponPattern {
        int longest = 0;
        for (String row : rows) {
            longest = Math.max(longest, row.length());
        }
        List<String> padded = new ArrayList<>(rows.size());
        for (String row : rows) {
            padded.add(row.length() >= longest ? row : row + " ".repeat(longest - row.length()));
        }
        rows = List.copyOf(padded);
        key = Map.copyOf(key);
    }

    /** 形の横幅。 */
    public int width() {
        int width = 0;
        for (String row : rows) {
            width = Math.max(width, row.length());
        }
        return width;
    }

    /** 形の高さ。 */
    public int height() {
        return rows.size();
    }

    /** 刀身のマスを持つか。 */
    public boolean hasRole(SmithingRole role) {
        return key.values().stream().anyMatch(cell -> cell.role() == role);
    }

    /**
     * 5×5 のグリッドに一致するか調べる。左右反転も許す。
     *
     * @param grid 左上から右下へ並べたグリッド
     * @return 一致したときの役割ごとの素材。一致しなければ {@code null}
     */
    public Match match(List<ItemStack> grid, int columns, int rowsInGrid) {
        Match plain = matchAt(grid, columns, rowsInGrid, false);
        if (plain != null) {
            return plain;
        }
        return matchAt(grid, columns, rowsInGrid, true);
    }

    private Match matchAt(List<ItemStack> grid, int columns, int rowsInGrid, boolean mirrored) {
        int patternWidth = width();
        int patternHeight = height();
        if (patternWidth > columns || patternHeight > rowsInGrid) {
            return null;
        }
        for (int dy = 0; dy + patternHeight <= rowsInGrid; dy++) {
            for (int dx = 0; dx + patternWidth <= columns; dx++) {
                Match match = matchAtOffset(grid, columns, rowsInGrid, dx, dy, mirrored);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private Match matchAtOffset(List<ItemStack> grid, int columns, int rowsInGrid, int dx, int dy,
            boolean mirrored) {
        Map<SmithingRole, List<ItemStack>> found = new EnumMap<>(SmithingRole.class);
        for (int row = 0; row < rowsInGrid; row++) {
            for (int column = 0; column < columns; column++) {
                ItemStack stack = grid.get(row * columns + column);
                boolean inside = row >= dy && row < dy + height() && column >= dx && column < dx + width();
                if (!inside) {
                    // 形の外に物があると不一致（バニラの shaped レシピと同じ）
                    if (!stack.isEmpty()) {
                        return null;
                    }
                    continue;
                }
                int patternRow = row - dy;
                int patternColumn = column - dx;
                if (mirrored) {
                    patternColumn = width() - 1 - patternColumn;
                }
                Cell cell = key.get(rows.get(patternRow).charAt(patternColumn));
                if (cell == null) {
                    if (!stack.isEmpty()) {
                        return null;
                    }
                    continue;
                }
                if (!cell.matches(stack)) {
                    return null;
                }
                found.computeIfAbsent(cell.role(), role -> new ArrayList<>()).add(stack);
            }
        }
        if (!found.containsKey(SmithingRole.BLADE)) {
            return null;
        }
        return new Match(this,
                List.copyOf(found.getOrDefault(SmithingRole.BLADE, List.of())),
                List.copyOf(found.getOrDefault(SmithingRole.GUARD, List.of())),
                List.copyOf(found.getOrDefault(SmithingRole.HANDLE, List.of())));
    }

    /**
     * マスの指定。
     *
     * @param role 役割
     * @param item 受け付けるアイテム ID（指定なしは {@code null}）
     * @param tag  受け付けるアイテムタグ（指定なしは {@code null}）
     */
    public record Cell(SmithingRole role, ResourceLocation item, ResourceLocation tag) {

        /** どちらも指定が無ければ「どんな素材でも可」。 */
        public boolean matches(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return false;
            }
            if (item != null) {
                Item value = BuiltInRegistries.ITEM.get(item);
                return value != null && stack.is(value);
            }
            if (tag != null) {
                return stack.is(TagKey.create(Registries.ITEM, tag));
            }
            return true;
        }
    }

    /** 一致したパターンと、役割ごとに置かれていた素材。 */
    public record Match(WeaponPattern pattern,
            List<ItemStack> blade,
            List<ItemStack> guard,
            List<ItemStack> handle) {

        /** 刀身の主素材（先頭のマス）。 */
        public ItemStack mainStack() {
            return blade.isEmpty() ? ItemStack.EMPTY : blade.get(0);
        }
    }
}
