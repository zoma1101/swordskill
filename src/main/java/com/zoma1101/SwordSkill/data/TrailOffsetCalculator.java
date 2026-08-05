package com.zoma1101.swordskill.data;

import com.zoma1101.swordskill.client.renderer.layer.SwordTrailLayer;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

/**
 * 武器の持ち方（handheld など）に基づいてトレイルのオフセット位置を計算する。
 */
public class TrailOffsetCalculator {
    
    /**
     * アイテムの親モデル情報から、トレイル発光位置のオフセットを計算する。
     * オフセットはアイテムの参照点からの相対位置。
     */
    public static Vector3f calculateTrailOffset(ItemStack itemStack, ItemModelDataHelper.ItemModelData modelData, boolean isLeftHand) {
        if (modelData == null) {
            return getDefaultOffset(isLeftHand);
        }
        
        ItemModelDataHelper.HoldingStyle style = modelData.getHoldingStyle();
        
        return switch (style) {
            case HANDHELD -> calculateHandheldOffset(isLeftHand);
            case HANDHELD_ROD -> calculateHandheldRodOffset(isLeftHand);
            case BOW -> calculateBowOffset(isLeftHand);
            case CROSSBOW -> calculateCrossbowOffset(isLeftHand);
            case TRIDENT -> calculateTridentOffset(isLeftHand);
            case SHIELD -> calculateShieldOffset(isLeftHand);
            case UNKNOWN -> getDefaultOffset(isLeftHand);
        };
    }
    
    /**
     * デフォルトオフセット（通常の一手武器）。
     */
    private static Vector3f getDefaultOffset(boolean isLeftHand) {
        // デフォルトのトレイル発光位置：アイテムの先端
        // 右手/左手で X 座標を反転
        float sign = isLeftHand ? -1.0f : 1.0f;
        return new Vector3f(sign * 0.5f, -0.5f, 0.5f);
    }
    
    /**
     * handheld 型（剣など一般的な一手武器）のオフセット。
     */
    private static Vector3f calculateHandheldOffset(boolean isLeftHand) {
        float sign = isLeftHand ? -1.0f : 1.0f;
        // 標準的な一手武器の持ち方：掌の高さから先端へ
        return new Vector3f(sign * 0.6f, -0.4f, 0.6f);
    }
    
    /**
     * handheld_rod 型（釣竿など棒状の武器）のオフセット。
     * より長く、角度がついている。
     */
    private static Vector3f calculateHandheldRodOffset(boolean isLeftHand) {
        float sign = isLeftHand ? -1.0f : 1.0f;
        // 棒状武器：より前に、より下へ
        return new Vector3f(sign * 0.4f, -0.8f, 1.0f);
    }
    
    /**
     * bow 型（弓）のオフセット。
     * 両手で持つため、通常はトレイル処理に含まれないが、カスタム実装の場合用。
     */
    private static Vector3f calculateBowOffset(boolean isLeftHand) {
        float sign = isLeftHand ? -1.0f : 1.0f;
        // 弓：握りの位置
        return new Vector3f(sign * 0.3f, -0.2f, 0.3f);
    }
    
    /**
     * crossbow 型（クロスボウ）のオフセット。
     */
    private static Vector3f calculateCrossbowOffset(boolean isLeftHand) {
        float sign = isLeftHand ? -1.0f : 1.0f;
        // クロスボウ：先端（弦の位置）
        return new Vector3f(sign * 0.5f, -0.3f, 0.8f);
    }
    
    /**
     * trident 型（トライデント）のオフセット。
     * 槍のような先端のある武器。
     */
    private static Vector3f calculateTridentOffset(boolean isLeftHand) {
        float sign = isLeftHand ? -1.0f : 1.0f;
        // トライデント：先端への距離
        return new Vector3f(sign * 0.4f, -0.9f, 1.1f);
    }
    
    /**
     * shield 型（盾）のオフセット。
     * 盾はトレイル対象でないことが多いが、念のため定義。
     */
    private static Vector3f calculateShieldOffset(boolean isLeftHand) {
        float sign = isLeftHand ? -1.0f : 1.0f;
        // 盾：中心
        return new Vector3f(sign * 0.3f, -0.1f, 0.2f);
    }
    
    /**
     * 親モデル名に基づいて直接的にトレイルオフセットを計算する。
     * モデルデータが無い場合の代替用。
     */
    public static Vector3f calculateTrailOffsetFromParentName(String parentModelName, boolean isLeftHand) {
        if (parentModelName == null) {
            return getDefaultOffset(isLeftHand);
        }
        
        float sign = isLeftHand ? -1.0f : 1.0f;
        
        return switch (parentModelName) {
            case "minecraft:item/handheld", "item/handheld" -> new Vector3f(sign * 0.6f, -0.4f, 0.6f);
            case "minecraft:item/handheld_rod", "item/handheld_rod" -> new Vector3f(sign * 0.4f, -0.8f, 1.0f);
            case "minecraft:item/bow", "item/bow" -> new Vector3f(sign * 0.3f, -0.2f, 0.3f);
            case "minecraft:item/crossbow", "item/crossbow" -> new Vector3f(sign * 0.5f, -0.3f, 0.8f);
            case "minecraft:item/trident", "item/trident" -> new Vector3f(sign * 0.4f, -0.9f, 1.1f);
            case "minecraft:item/shield", "item/shield" -> new Vector3f(sign * 0.3f, -0.1f, 0.2f);
            default -> getDefaultOffset(isLeftHand);
        };
    }
}
