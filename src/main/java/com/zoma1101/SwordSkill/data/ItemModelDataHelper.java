package com.zoma1101.swordskill.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * アイテムモデルデータ（model.jsonおよびそれが親として参照するモデル）を読み込み、
 * 親モデルの名前やアイテムの持ち方に関する情報を提供する。
 */
public class ItemModelDataHelper {
    private static final Gson GSON = new Gson();
    private static final Map<String, ItemModelData> MODEL_CACHE = new HashMap<>();
    
    /**
     * アイテムスタックから親モデルの情報を取得する。
     */
    @Nullable
    public static ItemModelData getItemModelData(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            return null;
        }
        
        String modelPath = "models/item/" + itemId.getPath() + ".json";
        return getOrLoadModel(modelPath);
    }
    
    /**
     * キャッシュから、または読み込んでモデルデータを取得する。
     */
    @Nullable
    private static ItemModelData getOrLoadModel(String modelPath) {
        if (MODEL_CACHE.containsKey(modelPath)) {
            return MODEL_CACHE.get(modelPath);
        }
        
        ItemModelData data = loadModel(modelPath);
        MODEL_CACHE.put(modelPath, data);
        return data;
    }
    
    /**
     * モデルJSONファイルを読み込む。
     */
    @Nullable
    private static ItemModelData loadModel(String modelPath) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getResourceManager() == null) {
                return null;
            }
            
            ResourceLocation modelLocation = ResourceLocation.fromNamespaceAndPath("minecraft", modelPath);
            Optional<Resource> resource = mc.getResourceManager().getResource(modelLocation);
            
            if (resource.isEmpty()) {
                return null;
            }
            
            try (InputStream is = resource.get().open();
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                return parseModelData(json);
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * JSONオブジェクトからモデルデータを解析する。
     */
    @Nullable
    private static ItemModelData parseModelData(JsonObject json) {
        if (json == null) {
            return null;
        }
        
        String parent = null;
        if (json.has("parent")) {
            parent = json.get("parent").getAsString();
        }
        
        JsonObject display = null;
        if (json.has("display")) {
            display = json.getAsJsonObject("display");
        }
        
        return new ItemModelData(parent, display);
    }
    
    /**
     * キャッシュをクリアする。リソースパック再読み込み時などに呼ぶ。
     */
    public static void clearCache() {
        MODEL_CACHE.clear();
    }
    
    /**
     * アイテムモデルデータを保持するクラス。
     */
    public static class ItemModelData {
        private final String parent;
        private final JsonObject displayConfig;
        
        public ItemModelData(@Nullable String parent, @Nullable JsonObject displayConfig) {
            this.parent = parent;
            this.displayConfig = displayConfig;
        }
        
        /**
         * 親モデルの名前を取得（e.g., "minecraft:item/handheld"）。
         */
        @Nullable
        public String getParent() {
            return parent;
        }
        
        /**
         * 親モデルが指定された名前かチェック。
         */
        public boolean isParent(String parentName) {
            if (parent == null) {
                return false;
            }
            return parent.equals(parentName) || 
                   parent.endsWith(":" + parentName) ||
                   parent.endsWith(parentName);
        }
        
        /**
         * displayオブジェクトを取得。
         */
        @Nullable
        public JsonObject getDisplayConfig() {
            return displayConfig;
        }
        
        /**
         * 親モデルが既知の持ち方のいずれかか判定。
         */
        public HoldingStyle getHoldingStyle() {
            if (parent == null) {
                return HoldingStyle.UNKNOWN;
            }
            
            if (isParent("item/handheld")) {
                return HoldingStyle.HANDHELD;
            } else if (isParent("item/handheld_rod")) {
                return HoldingStyle.HANDHELD_ROD;
            } else if (isParent("item/bow")) {
                return HoldingStyle.BOW;
            } else if (isParent("item/crossbow")) {
                return HoldingStyle.CROSSBOW;
            } else if (isParent("item/trident")) {
                return HoldingStyle.TRIDENT;
            } else if (isParent("item/shield")) {
                return HoldingStyle.SHIELD;
            }
            
            return HoldingStyle.UNKNOWN;
        }
    }
    
    /**
     * アイテムの持ち方のタイプ。
     */
    public enum HoldingStyle {
        HANDHELD,
        HANDHELD_ROD,
        BOW,
        CROSSBOW,
        TRIDENT,
        SHIELD,
        UNKNOWN
    }
}
