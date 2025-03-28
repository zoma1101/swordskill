package com.zoma1101.swordskill.swordskills;

import com.zoma1101.swordskill.swordskills.skill.HowToUse;
import com.zoma1101.swordskill.swordskills.skill.axe.*;
import com.zoma1101.swordskill.swordskills.skill.claw.*;
import com.zoma1101.swordskill.swordskills.skill.dagger.*;
import com.zoma1101.swordskill.swordskills.skill.dual_sword.*;
import com.zoma1101.swordskill.swordskills.skill.katana.*;
import com.zoma1101.swordskill.swordskills.skill.mace.*;
import com.zoma1101.swordskill.swordskills.skill.rapier.*;
import com.zoma1101.swordskill.swordskills.skill.scythe.*;
import com.zoma1101.swordskill.swordskills.skill.spear.*;
import com.zoma1101.swordskill.swordskills.skill.sword.*;
import com.zoma1101.swordskill.swordskills.skill.two_handed_sword.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.zoma1101.swordskill.swordskills.SkillData.WeaponType.*;

public class SwordSkillRegistry {
    public static final Map<Integer, SkillData> SKILLS = new HashMap<>();
    private static int nextSkillId = 0; // 自動ID生成用
    private static final int DefaultTransformTick = 12;
    
    private static final List<SkillData.WeaponType> AllWeapons = List.of(ONE_HANDED_SWORD, TWO_HANDED_SWORD, KATANA, AXE, RAPIER, CLAW, SPEAR, SCYTHE, DAGGER, DUALSWORD,MACE);

    static {
        // スキル登録
        //片手剣
        registerSkill(new SkillData(getNextSkillId(), "how_to_use",100, SkillData.SkillType.SIMPLE, HowToUse.class, AllWeapons, false, 0,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "slant",50, SkillData.SkillType.SIMPLE, Slant.class, List.of(ONE_HANDED_SWORD, TWO_HANDED_SWORD, KATANA, RAPIER, DUALSWORD), false, 0,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "horizontal",60, SkillData.SkillType.TRANSFORM, Horizontal.class, List.of(ONE_HANDED_SWORD, KATANA,DUALSWORD), false, 0,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "horizontal_arc",120, SkillData.SkillType.TRANSFORM, Horizontal_arc.class, List.of(ONE_HANDED_SWORD, KATANA,DUALSWORD), true, 0,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "horizontal_square", 240, SkillData.SkillType.TRANSFORM_FINISH, Horizontal_square.class, List.of(ONE_HANDED_SWORD, KATANA,DUALSWORD), true, 7,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "vertical", 60, SkillData.SkillType.TRANSFORM, Vertical.class, List.of(ONE_HANDED_SWORD, KATANA,DUALSWORD), false, 0,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "vertical_arc",120, SkillData.SkillType.TRANSFORM, Vertical_arc.class, List.of(ONE_HANDED_SWORD, KATANA,DUALSWORD), true, 0,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "vertical_square",240, SkillData.SkillType.TRANSFORM_FINISH, Vertical_square.class, List.of(ONE_HANDED_SWORD, KATANA,DUALSWORD), true, 7,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "x_break", 80, SkillData.SkillType.SIMPLE, XBreak.class, List.of(ONE_HANDED_SWORD, KATANA,DUALSWORD), false, 6,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "sharp_neil",160, SkillData.SkillType.SIMPLE, SharpNeil.class, List.of(ONE_HANDED_SWORD,DUALSWORD), false, 15,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "holy_cross_brade",100, SkillData.SkillType.SIMPLE, HolyCrossBrade.class, List.of(ONE_HANDED_SWORD,DUALSWORD), false, 7,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "rage_spike",120, SkillData.SkillType.RUSH, RageSpike.class, List.of(ONE_HANDED_SWORD,DUALSWORD), false, 9,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "sonic_reap",240, SkillData.SkillType.RUSH, SonicReap.class, List.of(ONE_HANDED_SWORD,DUALSWORD,DAGGER), false, 35,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "savage_fulcrum",200, SkillData.SkillType.SIMPLE, SavageFulcrum.class, List.of(ONE_HANDED_SWORD,DUALSWORD), false, 20,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "howling_octave",300, SkillData.SkillType.SIMPLE, HowlingOctave.class, List.of(ONE_HANDED_SWORD,DUALSWORD), false, 32,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "vorpal_strike",300, SkillData.SkillType.SIMPLE, VorpalStrike.class, List.of(ONE_HANDED_SWORD,DUALSWORD), false, 16,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "star_sword",240, SkillData.SkillType.SIMPLE, StarSword.class, List.of(ONE_HANDED_SWORD,DUALSWORD), false, 32,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "world_nova",400, SkillData.SkillType.SIMPLE, WorldNova.class, List.of(ONE_HANDED_SWORD,DUALSWORD), false, 25,DefaultTransformTick)); //低クオ
        //斧
        registerSkill(new SkillData(getNextSkillId(), "wall_wind", 120, SkillData.SkillType.SIMPLE, WallWind.class, List.of(AXE,TWO_HANDED_SWORD,MACE), false, 7,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "blood_finisher", 300, SkillData.SkillType.SIMPLE, BloodFinisher.class, List.of(AXE), false, 16,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "excite", 80, SkillData.SkillType.TRANSFORM, Excite.class, List.of(AXE,TWO_HANDED_SWORD,MACE), false, 0,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "excite_line",120, SkillData.SkillType.TRANSFORM, Excite_line.class, List.of(AXE,TWO_HANDED_SWORD,MACE), true, 0,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "excite_triangle",300, SkillData.SkillType.TRANSFORM_FINISH, Excite_triangle.class, List.of(AXE,TWO_HANDED_SWORD,MACE), true, 7,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "king_axe", 800, SkillData.SkillType.SIMPLE, KingAxe.class, List.of(AXE), false, 0,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "lumber_jack", 250, SkillData.SkillType.SIMPLE, LumberJack.class, List.of(AXE,MACE), false, 30,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "grand_upper", 120, SkillData.SkillType.RUSH, GrandUpper.class, List.of(AXE,TWO_HANDED_SWORD,MACE), false, 16,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "ryuuko_souhaku", 480, SkillData.SkillType.SIMPLE, RyuukoSouhaku.class, List.of(AXE), false, 31,DefaultTransformTick));
        //槍
        registerSkill(new SkillData(getNextSkillId(), "thrusts", 100, SkillData.SkillType.SIMPLE, Thrusts.class, List.of(SPEAR), false, 0,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "spinning_spear", 180, SkillData.SkillType.SIMPLE, SpinningSpear.class, List.of(SPEAR), false, 33,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "spark_thrust", 200, SkillData.SkillType.SIMPLE, SparkThrust.class, List.of(RAPIER,SPEAR), false, 22,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "sorvelte_charge", 200, SkillData.SkillType.RUSH, SorvelteCharge.class, List.of(SPEAR), false, 14,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "longlonglong", 200, SkillData.SkillType.SIMPLE, LongLongLong.class, List.of(SPEAR), false, 14,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "falling_star", 100, SkillData.SkillType.RUSH, FallingStar.class, List.of(SPEAR), false, 30,DefaultTransformTick));
        //二刀流
        registerSkill(new SkillData(getNextSkillId(), "end_revolver", 100, SkillData.SkillType.SIMPLE, EndRevolver.class, List.of(DUALSWORD), false, 5,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "ascade_refraction", 160, SkillData.SkillType.SIMPLE, AscadeRefraction.class, List.of(DUALSWORD), false, 10,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "double_circular", 200, SkillData.SkillType.RUSH, DoubleCircular.class, List.of(DUALSWORD), false, 55,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "star_burst_stream", 600, SkillData.SkillType.SIMPLE, StarBurstStream.class, List.of(DUALSWORD), false, 70,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "the_eclipse", 800, SkillData.SkillType.SIMPLE, TheEclipse.class, List.of(DUALSWORD), false, 115,DefaultTransformTick));
        //刀
        registerSkill(new SkillData(getNextSkillId(), "reaper", 100, SkillData.SkillType.SIMPLE, Reaper.class, List.of(ONE_HANDED_SWORD,KATANA,DAGGER), false, 5,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "sakura_mai", 200, SkillData.SkillType.SIMPLE, SakuraMai.class, List.of(KATANA), false, 20,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "zekku", 90, SkillData.SkillType.SIMPLE, Zekkuu.class, List.of(KATANA), false, 2,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "tumuzi_guruma", 250, SkillData.SkillType.SIMPLE, TumuziGuruma.class, List.of(KATANA), false, 15,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "gengetu", 250, SkillData.SkillType.RUSH, GenGetu.class, List.of(KATANA), false, 20,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "hiougi", 300, SkillData.SkillType.SIMPLE, Hiougi.class, List.of(KATANA), false, 16,DefaultTransformTick));
        //大剣
        registerSkill(new SkillData(getNextSkillId(), "cascade", 80, SkillData.SkillType.SIMPLE, CasCade.class, List.of(TWO_HANDED_SWORD), false, 3,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "aban_rash", 120, SkillData.SkillType.SIMPLE, AbanRash.class, List.of(TWO_HANDED_SWORD), false, 7,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "cyclone", 150, SkillData.SkillType.SIMPLE, Cyclone.class, List.of(TWO_HANDED_SWORD), false, 5,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "stan_hazard", 250, SkillData.SkillType.SIMPLE, StanHazard.class, List.of(TWO_HANDED_SWORD), false, 32,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "slash_blade", 150, SkillData.SkillType.SIMPLE, SlashBlade.class, List.of(TWO_HANDED_SWORD), false, 22,DefaultTransformTick));
        //細剣（レイピア）
        registerSkill(new SkillData(getNextSkillId(), "linear", 90, SkillData.SkillType.SIMPLE, Linear.class, List.of(ONE_HANDED_SWORD,RAPIER), false, 0,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "parallel_sting", 120, SkillData.SkillType.SIMPLE, ParallelSting.class, List.of(RAPIER), false, 10,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "quadraple_pain", 200, SkillData.SkillType.SIMPLE, QuadraplePain.class, List.of(RAPIER), false, 14,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "cruci_fiction", 200, SkillData.SkillType.SIMPLE, Crucifiction.class, List.of(RAPIER), false, 29,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "star_splash", 250, SkillData.SkillType.SIMPLE, StarSplash.class, List.of(RAPIER), false, 31,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "stary_tier", 200, SkillData.SkillType.SIMPLE, StaryTier.class, List.of(RAPIER), false, 21,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "mothers_rosario", 300, SkillData.SkillType.SIMPLE, MothersRosario.class, List.of(RAPIER), false, 37,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "shooting_star", 200, SkillData.SkillType.SIMPLE, ShootingStar.class, List.of(RAPIER), false, 4,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "flashing_penetrator", 500, SkillData.SkillType.SIMPLE, FlashingPenetrator.class, List.of(RAPIER), false, 6,DefaultTransformTick));
        //短剣
        registerSkill(new SkillData(getNextSkillId(), "canine", 60, SkillData.SkillType.SIMPLE, Canine.class, List.of(DAGGER), false, 0,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "face_liner", 100, SkillData.SkillType.SIMPLE, FaceLiner.class, List.of(DAGGER), false, 16,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "fad_edge", 150, SkillData.SkillType.SIMPLE, FadEdge.class, List.of(DAGGER), false, 17,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "rapid_bite", 200, SkillData.SkillType.RUSH, RapidBite.class, List.of(DAGGER), false, 200,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "octagon_edge", 180, SkillData.SkillType.SIMPLE, OctagonEdge.class, List.of(DAGGER), false, 18,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "kodati", 200, SkillData.SkillType.SIMPLE, Kodati.class, List.of(DAGGER), false, 11,DefaultTransformTick));
        //鎌
        registerSkill(new SkillData(getNextSkillId(), "barrick", 80, SkillData.SkillType.SIMPLE, Barrick.class, List.of(SCYTHE), false, 0,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "reaper_of_life", 120, SkillData.SkillType.SIMPLE, ReaperOfLife.class, List.of(SCYTHE), false, 12,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "reaper_of_death", 120, SkillData.SkillType.SIMPLE, ReaperOfDeath.class, List.of(SCYTHE), false, 12,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "shadow_scythe", 150, SkillData.SkillType.SIMPLE, ShadowScythe.class, List.of(SCYTHE), false, 7,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "requiem", 250, SkillData.SkillType.SIMPLE, Requiem.class, List.of(SCYTHE), false, 26,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "storm", 150, SkillData.SkillType.TRANSFORM, Storm.class, List.of(SCYTHE), false, 10,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "storm_mirror", 250, SkillData.SkillType.TRANSFORM_FINISH, StormMirror.class, List.of(SCYTHE), true, 10,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "judgment", 350, SkillData.SkillType.SIMPLE, Judgment.class, List.of(SCYTHE), false, 38,DefaultTransformTick));
        //爪
        registerSkill(new SkillData(getNextSkillId(), "talon", 70, SkillData.SkillType.SIMPLE, Talon.class, List.of(CLAW), false, 0,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "hant", 140, SkillData.SkillType.TRANSFORM, Hant.class, List.of(CLAW), false, 0,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "hant_killer", 200, SkillData.SkillType.TRANSFORM_FINISH, HantKiller.class, List.of(CLAW), true, 16,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "tiger_claw", 160, SkillData.SkillType.SIMPLE, TigerClaw.class, List.of(CLAW), false, 14,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "dragon_claw", 160, SkillData.SkillType.SIMPLE, DragonClaw.class, List.of(CLAW), false, 10,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "beast_claw", 200, SkillData.SkillType.SIMPLE, BeastClaw.class, List.of(CLAW), false, 9,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "golden_claw", 250, SkillData.SkillType.SIMPLE, GoldClaw.class, List.of(CLAW), false, 5,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "fast", 160, SkillData.SkillType.RUSH, Fast.class, List.of(CLAW), false, 10,DefaultTransformTick));
        //メイス
        registerSkill(new SkillData(getNextSkillId(), "grand", 70, SkillData.SkillType.SIMPLE, Grand.class, List.of(MACE), false, 0,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "double_creep", 100, SkillData.SkillType.SIMPLE, DoubleCreep.class, List.of(MACE), false, 10,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "hammer_down", 150, SkillData.SkillType.SIMPLE, HammerDown.class, List.of(MACE), false, 8,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "diastrophism", 180, SkillData.SkillType.SIMPLE, Diastrophism.class, List.of(MACE), false, 18,DefaultTransformTick));
        registerSkill(new SkillData(getNextSkillId(), "mace_impact", 300, SkillData.SkillType.RUSH, MaceImpact.class, List.of(MACE), false, 200,DefaultTransformTick));
    }

    private static int getNextSkillId() {
        return nextSkillId++;
    }

    private static void registerSkill(SkillData skillData) {
        SKILLS.put(skillData.getId(), skillData);
    }
}