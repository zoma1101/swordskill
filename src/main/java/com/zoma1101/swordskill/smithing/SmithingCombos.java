package com.zoma1101.swordskill.smithing;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 刀身・鍔・柄の組み合わせに、見た目（自動生成モデル）の番号を割り当てる。
 *
 * <p>番号は「刀身（ID 順）× 鍔（無し + ID 順）× 柄（ID 順）」の辞書順で決まるため、
 * 同じ定義を持っていればサーバーとクライアントで必ず同じ値になる。
 * 生成スクリプト（tools/smithing）も同じ規則で計算する。</p>
 *
 * <p>番号は {@code minecraft:custom_model_data} に焼き込み、アイテムモデルの
 * {@code overrides} から合成モデルを選ぶのに使う。</p>
 */
public final class SmithingCombos {

    private SmithingCombos() {
    }

    /** 組み合わせに対応する 1 以上の番号。該当が無ければ 0（= 既定モデル）。 */
    public static int modelIndex(ResourceLocation blade, ResourceLocation guard, ResourceLocation handle) {
        if (blade == null || handle == null) {
            return 0;
        }
        List<ResourceLocation> blades = ids(SmithingPartKind.BLADE);
        List<ResourceLocation> guards = ids(SmithingPartKind.GUARD);
        List<ResourceLocation> handles = ids(SmithingPartKind.HANDLE);
        int bladeIndex = blades.indexOf(blade);
        int guardIndex = guard == null ? 0 : guards.indexOf(guard) + 1;
        int handleIndex = handles.indexOf(handle);
        if (bladeIndex < 0 || guardIndex < 0 || handleIndex < 0 || handles.isEmpty()) {
            return 0;
        }
        int guardSpan = guards.size() + 1;
        return ((bladeIndex * guardSpan) + guardIndex) * handles.size() + handleIndex + 1;
    }

    /** 組み合わせの総数（番号の最大値）。 */
    public static int modelCount() {
        int guards = ids(SmithingPartKind.GUARD).size() + 1;
        return ids(SmithingPartKind.BLADE).size() * guards * ids(SmithingPartKind.HANDLE).size();
    }

    private static List<ResourceLocation> ids(SmithingPartKind kind) {
        List<ResourceLocation> list = new ArrayList<>();
        for (SmithingPart part : SmithingDefinitions.parts()) {
            if (part.kind() == kind) {
                list.add(part.id());
            }
        }
        list.sort(Comparator.comparing(ResourceLocation::toString));
        return list;
    }
}
