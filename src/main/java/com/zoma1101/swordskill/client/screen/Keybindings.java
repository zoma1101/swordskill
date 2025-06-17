package com.zoma1101.swordskill.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.client.KeyMapping;


import static net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME;

public class Keybindings {
    public static final Keybindings INSTANCE = new Keybindings();

    private static final String CATEGORY = "key.categories." + SwordSkill.MOD_ID;

    private static final String QUICK_CAST_CATEGORY = "key.categories." + SwordSkill.MOD_ID+".quick_cast";

    private Keybindings() {}
    public final KeyMapping SwordSkill_Selector_Key = new KeyMapping(
            "key."+ SwordSkill.MOD_ID + ".select_ss_key",
            IN_GAME,
            InputConstants.getKey(InputConstants.KEY_G, -1),
            CATEGORY
    );
    public final KeyMapping SwordSkill_Use_Key = new KeyMapping(
            "key."+ SwordSkill.MOD_ID + ".use_ss_key",
            IN_GAME,
            InputConstants.getKey(InputConstants.KEY_R, -1),
            CATEGORY
    );
    public final KeyMapping SwordSkill_QuickSelect_Key = new KeyMapping(
            "key."+ SwordSkill.MOD_ID + ".quick_select_ss_key",
            IN_GAME,
            InputConstants.getKey(InputConstants.KEY_LCONTROL, -1),
            CATEGORY
    );
    public final KeyMapping SwordSkill_HUD_Setting = new KeyMapping(
            "key."+ SwordSkill.MOD_ID + ".hud_setting_key",
            IN_GAME,
            InputConstants.getKey(InputConstants.KEY_MINUS, -1),
            CATEGORY
    );

    public final KeyMapping SwordSkill_Use_Key_0 = new KeyMapping(
            "key."+ SwordSkill.MOD_ID + ".use_ss_key_0",
            IN_GAME,
            InputConstants.getKey(InputConstants.KEY_X, -1),
            QUICK_CAST_CATEGORY
    );
    public final KeyMapping SwordSkill_Use_Key_1 = new KeyMapping(
            "key."+ SwordSkill.MOD_ID + ".use_ss_key_1",
            IN_GAME,
            InputConstants.getKey(InputConstants.KEY_C, -1),
            QUICK_CAST_CATEGORY
    );
    public final KeyMapping SwordSkill_Use_Key_2 = new KeyMapping(
            "key."+ SwordSkill.MOD_ID + ".use_ss_key_2",
            IN_GAME,
            InputConstants.getKey(InputConstants.KEY_V, -1),
            QUICK_CAST_CATEGORY
    );
    public final KeyMapping SwordSkill_Use_Key_3 = new KeyMapping(
            "key."+ SwordSkill.MOD_ID + ".use_ss_key_3",
            IN_GAME,
            InputConstants.getKey(InputConstants.KEY_B, -1),
            QUICK_CAST_CATEGORY
    );
    public final KeyMapping SwordSkill_Use_Key_4 = new KeyMapping(
            "key."+ SwordSkill.MOD_ID + ".use_ss_key_4",
            IN_GAME,
            InputConstants.getKey(InputConstants.KEY_N, -1),
            QUICK_CAST_CATEGORY
    );

}
