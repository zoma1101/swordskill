package com.zoma.SwordSkill.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.zoma.SwordSkill.main.SwordSkill;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;

public class Keybindings {
    public static final Keybindings INSTANCE = new Keybindings();

    private static final String CATEGORY = "key.categories." + SwordSkill.MOD_ID;

    private Keybindings() {}
    public final KeyMapping SwordSkill_Selector_Key = new KeyMapping(
            "key."+ SwordSkill.MOD_ID + ".select_ss_key",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_G, -1),
            CATEGORY
    );
    public final KeyMapping SwordSkill_Use_Key = new KeyMapping(
            "key."+ SwordSkill.MOD_ID + ".use_ss_key",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_R, -1),
            CATEGORY
    );
    public final KeyMapping SwordSkill_QuickSelect_Key = new KeyMapping(
            "key."+ SwordSkill.MOD_ID + ".quick_select_ss_key",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_LCONTROL, -1),
            CATEGORY
    );

    public final KeyMapping SwordSkill_Use_Key_0 = new KeyMapping(
            "key."+ SwordSkill.MOD_ID + ".use_ss_key_0",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_X, -1),
            CATEGORY
    );
    public final KeyMapping SwordSkill_Use_Key_1 = new KeyMapping(
            "key."+ SwordSkill.MOD_ID + ".use_ss_key_1",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_C, -1),
            CATEGORY
    );
    public final KeyMapping SwordSkill_Use_Key_2 = new KeyMapping(
            "key."+ SwordSkill.MOD_ID + ".use_ss_key_2",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_V, -1),
            CATEGORY
    );
    public final KeyMapping SwordSkill_Use_Key_3 = new KeyMapping(
            "key."+ SwordSkill.MOD_ID + ".use_ss_key_3",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_B, -1),
            CATEGORY
    );
    public final KeyMapping SwordSkill_Use_Key_4 = new KeyMapping(
            "key."+ SwordSkill.MOD_ID + ".use_ss_key_4",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_N, -1),
            CATEGORY
    );

}
