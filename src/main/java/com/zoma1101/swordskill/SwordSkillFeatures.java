package com.zoma1101.swordskill;

/**
 * リリースごとに一時的にオン／オフする機能のフラグ。
 *
 * <p>コードを残したまま機能を隠せるように、登録・導線・画面をここで分岐させる。
 * 再有効化するときは {@code true} に戻すだけでよい。</p>
 */
public final class SwordSkillFeatures {

    /**
     * 鍛冶システム（鍛冶台・溶解炉・鍛造武器・インゴット）。
     *
     * <p>バランス調整と他 Mod との互換性のため、一次リリースでは無効。
     * 無効時はブロック・アイテム・メニューを登録しない。</p>
     */
    public static final boolean SMITHING = false;

    /**
     * ステータス画面（プレイヤーレベル／STR・AGI の割り振り表示）。
     *
     * <p>一次リリースでは不要のため無効。熟練度一覧画面は単独で開ける。</p>
     */
    public static final boolean STATUS_SCREEN = false;

    private SwordSkillFeatures() {
    }
}
