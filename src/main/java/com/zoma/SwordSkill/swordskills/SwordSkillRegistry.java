package com.zoma.SwordSkill.swordskills;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zoma.SwordSkill.swordskills.skill.HowToUse;
import com.zoma.SwordSkill.swordskills.skill.axe.*;
import com.zoma.SwordSkill.swordskills.skill.dual_sword.AscadeRefraction;
import com.zoma.SwordSkill.swordskills.skill.dual_sword.DoubleCircular;
import com.zoma.SwordSkill.swordskills.skill.dual_sword.EndRevolver;
import com.zoma.SwordSkill.swordskills.skill.dual_sword.StarBurstStream;
import com.zoma.SwordSkill.swordskills.skill.spear.*;
import com.zoma.SwordSkill.swordskills.skill.sword.*;

import static com.zoma.SwordSkill.swordskills.SkillData.WeaponType.*;

public class SwordSkillRegistry {
    public static final Map<Integer, SkillData> SKILLS = new HashMap<>();
    private static int nextSkillId = 0; // 自動ID生成用

    static {
        // スキル登録
        registerSkill(new SkillData(getNextSkillId(), "how_to_use",100, SkillData.SkillType.SIMPLE, HowToUse.class, List.of(ONE_HANDED_SWORD, KATANA,AXE,DUALSWORD,SPEAR), false, 0));
        registerSkill(new SkillData(getNextSkillId(), "slant",50, SkillData.SkillType.SIMPLE, Slant.class, List.of(ONE_HANDED_SWORD, KATANA,SPEAR,DUALSWORD), false, 0));
        registerSkill(new SkillData(getNextSkillId(), "horizontal",60, SkillData.SkillType.TRANSFORM, Horizontal.class, List.of(ONE_HANDED_SWORD, KATANA,DUALSWORD), false, 0));
        registerSkill(new SkillData(getNextSkillId(), "horizontal_arc",120, SkillData.SkillType.TRANSFORM, Horizontal_arc.class, List.of(ONE_HANDED_SWORD, KATANA,DUALSWORD), true, 0));
        registerSkill(new SkillData(getNextSkillId(), "horizontal_square", 240, SkillData.SkillType.TRANSFORM_FINISH, Horizontal_square.class, List.of(ONE_HANDED_SWORD, KATANA,DUALSWORD), true, 7));
        registerSkill(new SkillData(getNextSkillId(), "vertical", 60, SkillData.SkillType.TRANSFORM, Vertical.class, List.of(ONE_HANDED_SWORD, KATANA,DUALSWORD), false, 0));
        registerSkill(new SkillData(getNextSkillId(), "vertical_arc",120, SkillData.SkillType.TRANSFORM, Vertical_arc.class, List.of(ONE_HANDED_SWORD, KATANA,DUALSWORD), true, 0));
        registerSkill(new SkillData(getNextSkillId(), "vertical_square",240, SkillData.SkillType.TRANSFORM_FINISH, Vertical_square.class, List.of(ONE_HANDED_SWORD, KATANA,DUALSWORD), true, 7));
        registerSkill(new SkillData(getNextSkillId(), "x_break", 80, SkillData.SkillType.SIMPLE, XBreak.class, List.of(ONE_HANDED_SWORD, KATANA,DUALSWORD), false, 6));
        registerSkill(new SkillData(getNextSkillId(), "sharp_neil",160, SkillData.SkillType.SIMPLE, SharpNeil.class, List.of(ONE_HANDED_SWORD,DUALSWORD), false, 10));
        registerSkill(new SkillData(getNextSkillId(), "holy_cross_brade",100, SkillData.SkillType.SIMPLE, HolyCrossBrade.class, List.of(ONE_HANDED_SWORD,DUALSWORD), false, 7));
        registerSkill(new SkillData(getNextSkillId(), "rage_spike",120, SkillData.SkillType.RUSH, RageSpike.class, List.of(ONE_HANDED_SWORD,DUALSWORD), false, 9));
        registerSkill(new SkillData(getNextSkillId(), "sonic_reap",240, SkillData.SkillType.RUSH, SonicReap.class, List.of(ONE_HANDED_SWORD,DUALSWORD), false, 35));
        registerSkill(new SkillData(getNextSkillId(), "savage_fulcrum",200, SkillData.SkillType.SIMPLE, SavageFulcrum.class, List.of(ONE_HANDED_SWORD,DUALSWORD), false, 10));
        registerSkill(new SkillData(getNextSkillId(), "howling_octave",300, SkillData.SkillType.SIMPLE, HowlingOctave.class, List.of(ONE_HANDED_SWORD,DUALSWORD), false, 26));
        registerSkill(new SkillData(getNextSkillId(), "vorpal_strike",300, SkillData.SkillType.RUSH, VorpalStrike.class, List.of(ONE_HANDED_SWORD,DUALSWORD), false, 16));
        registerSkill(new SkillData(getNextSkillId(), "star_sword",240, SkillData.SkillType.SIMPLE, StarSword.class, List.of(ONE_HANDED_SWORD,DUALSWORD), false, 32));
        registerSkill(new SkillData(getNextSkillId(), "world_nova",400, SkillData.SkillType.SIMPLE, WorldNova.class, List.of(ONE_HANDED_SWORD,DUALSWORD), false, 31));

        registerSkill(new SkillData(getNextSkillId(), "wall_wind", 120, SkillData.SkillType.SIMPLE, WallWind.class, List.of(AXE), false, 7));
        registerSkill(new SkillData(getNextSkillId(), "blood_finisher", 300, SkillData.SkillType.SIMPLE, BloodFinisher.class, List.of(AXE), false, 16));
        registerSkill(new SkillData(getNextSkillId(), "excite", 80, SkillData.SkillType.TRANSFORM, Excite.class, List.of(AXE), false, 0));
        registerSkill(new SkillData(getNextSkillId(), "excite_line",120, SkillData.SkillType.TRANSFORM, Excite_line.class, List.of(AXE), true, 0));
        registerSkill(new SkillData(getNextSkillId(), "excite_triangle",300, SkillData.SkillType.TRANSFORM_FINISH, Excite_triangle.class, List.of(AXE), true, 7));
        registerSkill(new SkillData(getNextSkillId(), "king_axe", 800, SkillData.SkillType.SIMPLE, KingAxe.class, List.of(AXE), false, 0));
        registerSkill(new SkillData(getNextSkillId(), "lumber_jack", 250, SkillData.SkillType.SIMPLE, LumberJack.class, List.of(AXE), false, 21));
        registerSkill(new SkillData(getNextSkillId(), "grand_upper", 120, SkillData.SkillType.RUSH, GrandUpper.class, List.of(AXE), false, 16));
        registerSkill(new SkillData(getNextSkillId(), "ryuuko_souhaku", 480, SkillData.SkillType.SIMPLE, RyuukoSouhaku.class, List.of(AXE), false, 21));

        registerSkill(new SkillData(getNextSkillId(), "linear", 80, SkillData.SkillType.SIMPLE, Linear.class, List.of(ONE_HANDED_SWORD,RAPIER,SPEAR), false, 0));
        registerSkill(new SkillData(getNextSkillId(), "spinning_spear", 180, SkillData.SkillType.SIMPLE, SpinningSpear.class, List.of(SPEAR), false, 20));
        registerSkill(new SkillData(getNextSkillId(), "spark_thrust", 200, SkillData.SkillType.SIMPLE, SparkThrust.class, List.of(RAPIER,SPEAR), false, 14));
        registerSkill(new SkillData(getNextSkillId(), "sorvelte_charge", 200, SkillData.SkillType.RUSH, SorvelteCharge.class, List.of(SPEAR), false, 14));
        registerSkill(new SkillData(getNextSkillId(), "longlonglong", 200, SkillData.SkillType.SIMPLE, LongLongLong.class, List.of(SPEAR), false, 14));
        registerSkill(new SkillData(getNextSkillId(), "falling_star", 100, SkillData.SkillType.SIMPLE, FallingStar.class, List.of(SPEAR), false, 30));

        registerSkill(new SkillData(getNextSkillId(), "end_revolver", 100, SkillData.SkillType.SIMPLE, EndRevolver.class, List.of(DUALSWORD), false, 5));
        registerSkill(new SkillData(getNextSkillId(), "ascade_refraction", 160, SkillData.SkillType.SIMPLE, AscadeRefraction.class, List.of(DUALSWORD), false, 10));
        registerSkill(new SkillData(getNextSkillId(), "double_circular", 200, SkillData.SkillType.RUSH, DoubleCircular.class, List.of(DUALSWORD), false, 40));
        registerSkill(new SkillData(getNextSkillId(), "star_burst_stream", 600, SkillData.SkillType.RUSH, StarBurstStream.class, List.of(DUALSWORD), false, 60));

    }

    private static int getNextSkillId() {
        return nextSkillId++;
    }

    private static void registerSkill(SkillData skillData) {
        SKILLS.put(skillData.getId(), skillData);
    }
}