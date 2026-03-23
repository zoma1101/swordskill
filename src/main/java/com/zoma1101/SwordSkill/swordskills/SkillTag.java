package com.zoma1101.swordskill.swordskills;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ソードスキルの攻撃特性を定義するタグです。
 * ダメージ倍率、特殊効果（回復、デバフ等）、当たり判定方式などを制御します。
 */
public enum SkillTag {
    HOLY("holy"), // アンデッドに大ダメージ
    DARK("dark"), // 非アンデッドに大ダメージ
    BLOOD("blood"), // 与ダメージの一定割合を回復
    EXECUTION("exec"), // 低体力時にダメージ増加
    MAGIC("magic"), // 魔法ダメージ / 防御無視
    SLOWNESS("slow"), // 移動速度低下を付与
    RAY("ray"), // 放射状・貫通型の当たり判定を使用
    TRAIL("trail"), // 剣の軌跡（トレイル）を発生させる
    SHAPE_ARC("shape_arc"), // 三日月形（円弧）の形状
    SHAPE_V("shape_v"); // V字型の形状

    private final String id;

    SkillTag(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * カンマ区切りの文字列からタグのリストを作成します。
     */
    public static List<SkillTag> fromString(String tags) {
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .map(s -> {
                    for (SkillTag tag : values()) {
                        if (tag.id.equalsIgnoreCase(s) || tag.name().equalsIgnoreCase(s)) {
                            return tag;
                        }
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * タグのリストをカンマ区切りの文字列に変換します。
     */
    public static String toString(List<SkillTag> tags) {
        return tags.stream().map(SkillTag::getId).collect(Collectors.joining(","));
    }
}
