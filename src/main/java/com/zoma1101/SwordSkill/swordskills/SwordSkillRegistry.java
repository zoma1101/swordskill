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
import com.zoma1101.swordskill.swordskills.skill.martial_arts.*;
import com.zoma1101.swordskill.swordskills.skill.two_handed_sword.*;
import com.zoma1101.swordskill.swordskills.SkillData.FollowBone;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.zoma1101.swordskill.swordskills.SkillData.WeaponType.*;
public class SwordSkillRegistry {
        public static final Map<Integer, SkillData> SKILLS = new HashMap<>();
        private static int nextSkillId = 0; // 自動ID生成用
        private static final int DefaultTransformTick = 20;

        private static final List<SkillData.WeaponType> AllWeapons = List.of(ONE_HANDED_SWORD, TWO_HANDED_SWORD, KATANA,
                        AXE, RAPIER, CLAW, SPEAR, SCYTHE, DAGGER, DUALSWORD, MACE, MARTIAL_ARTS);

        static {
                // スキル登録
                // 片手剣
                registerSkill(new SkillData(getNextSkillId(), "how_to_use", 100, 0.0, SkillData.SkillType.SIMPLE,
                                HowToUse.class,
                                AllWeapons, false, 0, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 8, 1.0f, 1.0f));
                registerSkill(new SkillData(getNextSkillId(), "slant", 50, 5.0, SkillData.SkillType.SIMPLE,
                                Slant.class,
                                List.of(ONE_HANDED_SWORD, TWO_HANDED_SWORD, KATANA, RAPIER, DUALSWORD), false, 0,
                                DefaultTransformTick, SkillColor.ELECTRIC_BLUE , "simple_2", 8, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "horizontal", 60, 8.0, SkillData.SkillType.TRANSFORM,
                                Horizontal.class, List.of(ONE_HANDED_SWORD, KATANA, DUALSWORD), false, 3,
                                DefaultTransformTick, SkillColor.ELECTRIC_BLUE, "simple_2", 8, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "horizontal_arc", 120, 7.0, SkillData.SkillType.TRANSFORM,
                                Horizontal_arc.class, List.of(ONE_HANDED_SWORD, KATANA, DUALSWORD), true, 8,
                                DefaultTransformTick, SkillColor.ELECTRIC_BLUE, "simple_2", 8, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "horizontal_square", 240, 15.0,
                                SkillData.SkillType.TRANSFORM_FINISH,
                                Horizontal_square.class, List.of(ONE_HANDED_SWORD, KATANA, DUALSWORD), true, 20,
                                DefaultTransformTick, SkillColor.ELECTRIC_BLUE, "simple_2", 8, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "vertical", 60, 8.0, SkillData.SkillType.TRANSFORM,
                                Vertical.class,
                                List.of(ONE_HANDED_SWORD, KATANA, DUALSWORD), false, 0, DefaultTransformTick,
                        SkillColor.ELECTRIC_BLUE, "simple_2", 8, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "vertical_arc", 120, 7.0, SkillData.SkillType.TRANSFORM,
                                Vertical_arc.class, List.of(ONE_HANDED_SWORD, KATANA, DUALSWORD), true, 0,
                                DefaultTransformTick, SkillColor.ELECTRIC_BLUE, "simple_2", 8, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "vertical_square", 240, 15.0,
                                SkillData.SkillType.TRANSFORM_FINISH,
                                Vertical_square.class, List.of(ONE_HANDED_SWORD, KATANA, DUALSWORD), true, 10,
                                DefaultTransformTick, SkillColor.ELECTRIC_BLUE, "simple_2", 8, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "x_break", 80, 10.0, SkillData.SkillType.SIMPLE,
                                XBreak.class,
                                List.of(ONE_HANDED_SWORD, KATANA, DUALSWORD), false, 12, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 15, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "sharp_neil", 200, 20.0, SkillData.SkillType.SIMPLE,
                                SharpNeil.class,
                                List.of(ONE_HANDED_SWORD, DUALSWORD), false, 19, DefaultTransformTick,
                                0xFFF01616, "simple_2", 25, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "holy_cross_brade", 140, 15.0, SkillData.SkillType.SIMPLE,
                                HolyCrossBrade.class, List.of(ONE_HANDED_SWORD, DUALSWORD), false, 12,
                                DefaultTransformTick,0xFFFFFF33, "simple_2", 15, 1.6f, 3.0f));
                registerSkill(new SkillData(getNextSkillId(), "rage_spike", 105, 10.0, SkillData.SkillType.RUSH,
                                RageSpike.class,
                                List.of(ONE_HANDED_SWORD, DUALSWORD), false, 9, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 15, 1.0f, 2.5f));
                registerSkill(new SkillData(getNextSkillId(), "sonic_reap", 250, 20.0, SkillData.SkillType.RUSH,
                                SonicReap.class,
                                List.of(ONE_HANDED_SWORD, DUALSWORD, DAGGER), false, 35, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 20, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "savage_fulcrum", 100, 15.0, SkillData.SkillType.SIMPLE,
                                SavageFulcrum.class, List.of(ONE_HANDED_SWORD, DUALSWORD), false, 20,
                                DefaultTransformTick, SkillColor.ELECTRIC_BLUE, "simple_2", 15, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "howling_octave", 225, 20.0, SkillData.SkillType.SIMPLE,
                                HowlingOctave.class, List.of(ONE_HANDED_SWORD, DUALSWORD), false, 32,
                                DefaultTransformTick, SkillColor.ELECTRIC_BLUE, "simple_2", 15, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "vorpal_strike", 250, 30.0, SkillData.SkillType.SIMPLE,
                                VorpalStrike.class, List.of(ONE_HANDED_SWORD, DUALSWORD), false, 16,
                                DefaultTransformTick, SkillColor.NEON_RED, "simple_2", 20, 1.8f, 4.0f));
                registerSkill(new SkillData(getNextSkillId(), "star_sword", 250, 35.0, SkillData.SkillType.SIMPLE,
                                StarSword.class,
                                List.of(ONE_HANDED_SWORD, DUALSWORD), false, 32, DefaultTransformTick,
                                0xFFFFFF00, "simple_2", 20, 1.6f, 3.5f));
                registerSkill(new SkillData(getNextSkillId(), "world_nova", 250, 30.0, SkillData.SkillType.SIMPLE,
                                WorldNova.class,
                                List.of(ONE_HANDED_SWORD, DUALSWORD), false, 25, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 25, 2.0f, 4.0f)); // 低クオ
                // 斧
                registerSkill(new SkillData(getNextSkillId(), "wall_wind", 120, 7.0, SkillData.SkillType.SIMPLE,
                                WallWind.class,
                                List.of(AXE, TWO_HANDED_SWORD, MACE), false, 7, DefaultTransformTick
                ,0xFF2ff21e, "simple_2", 10, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "blood_finisher", 280, 15.0, SkillData.SkillType.SIMPLE,
                                BloodFinisher.class, List.of(AXE), false, 16, DefaultTransformTick
                        ,0xFFF01616, "simple_2", 10, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "excite", 90, 10.0, SkillData.SkillType.TRANSFORM,
                                Excite.class,
                                List.of(AXE, TWO_HANDED_SWORD, MACE), false, 0, DefaultTransformTick
                        ,0xFF2ff21e, "simple_2", 10, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "excite_line", 150, 8.0, SkillData.SkillType.TRANSFORM,
                                Excite_line.class, List.of(AXE, TWO_HANDED_SWORD, MACE), true, 0,
                                DefaultTransformTick
                        ,0xFF2ff21e, "simple_2", 10, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "excite_triangle", 250, 12.0,
                                SkillData.SkillType.TRANSFORM_FINISH,
                                Excite_triangle.class, List.of(AXE, TWO_HANDED_SWORD, MACE), true, 7,
                                DefaultTransformTick
                        ,0xFF2ff21e, "simple_2", 10, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "king_axe", 600, 60.0, SkillData.SkillType.SIMPLE,
                                KingAxe.class,
                                List.of(AXE), false, 0, DefaultTransformTick
                        ,0xFFffee49, "simple_2", 10, 2.4f, 7.2f));
                registerSkill(new SkillData(getNextSkillId(), "lumber_jack", 250, 10.0, SkillData.SkillType.SIMPLE,
                                LumberJack.class,
                                List.of(AXE, MACE), false, 30, DefaultTransformTick,0xFF00e872, "simple_2", 10, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "grand_upper", 150, 20.0, SkillData.SkillType.RUSH,
                                GrandUpper.class,
                                List.of(AXE, TWO_HANDED_SWORD, MACE), false, 16, DefaultTransformTick,0xFF00e872, "simple_2", 10, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "ryuuko_souhaku", 900, 70.0, SkillData.SkillType.SIMPLE,
                                RyuukoSouhaku.class, List.of(AXE), false, 31, DefaultTransformTick,0xFF48f4b2, "simple_2", 10, 1.6f, 3.2f));
                // 槍
                registerSkill(new SkillData(getNextSkillId(), "thrusts", 100, 8.0, SkillData.SkillType.SIMPLE,
                                Thrusts.class,
                                List.of(SPEAR), false, 0, DefaultTransformTick,0xFFFFFF33, "simple_2", 15, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "spinning_spear", 150, 12.0, SkillData.SkillType.SIMPLE,
                                SpinningSpear.class, List.of(SPEAR), false, 33, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 15, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "spark_thrust", 90, 20.0, SkillData.SkillType.SIMPLE,
                                SparkThrust.class, List.of(RAPIER, SPEAR), false, 22, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 15, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "sorvelte_charge", 200, 30.0, SkillData.SkillType.RUSH,
                                SorvelteCharge.class, List.of(SPEAR), false, 14, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 15, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "longlonglong", 225, 50.0, SkillData.SkillType.SIMPLE,
                                LongLongLong.class, List.of(SPEAR), false, 14, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 15, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "falling_star", 250, 40.0, SkillData.SkillType.RUSH,
                                FallingStar.class,
                                List.of(SPEAR), false, 30, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 20, 1.6f, 3.2f));
                // 二刀流
                registerSkill(new SkillData(getNextSkillId(), "end_revolver", 100, 10.0, SkillData.SkillType.SIMPLE,
                                EndRevolver.class, List.of(DUALSWORD), false, 5, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 15, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "ascade_refraction", 160, 20.0,
                                SkillData.SkillType.SIMPLE,
                                AscadeRefraction.class, List.of(DUALSWORD), false, 10, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 15, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "double_circular", 150, 30.0, SkillData.SkillType.RUSH,
                                DoubleCircular.class, List.of(DUALSWORD), false, 55, DefaultTransformTick,
                                0xFFF01616, "simple_2", 20, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "star_burst_stream", 600, 50.0,
                                SkillData.SkillType.SIMPLE,
                                StarBurstStream.class, List.of(DUALSWORD), false, 70, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 30, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "the_eclipse", 750, 70.0, SkillData.SkillType.SIMPLE,
                                TheEclipse.class,
                                List.of(DUALSWORD), false, 115, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 40, 1.6f, 3.2f));
                // 刀
                registerSkill(new SkillData(getNextSkillId(), "reaper", 100, 6.0, SkillData.SkillType.SIMPLE,
                                Reaper.class,
                                List.of(ONE_HANDED_SWORD, KATANA, DAGGER), false, 5, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 15, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "sakura_mai", 175, 20.0, SkillData.SkillType.SIMPLE,
                                SakuraMai.class,
                                List.of(KATANA), false, 20, DefaultTransformTick,
                                0xFFE3B7EB, "simple_2", 15, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "zekku", 90, 15.0, SkillData.SkillType.SIMPLE,
                                Zekkuu.class,
                                List.of(KATANA), false, 2, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 15, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "tumuzi_guruma", 250, 20.0, SkillData.SkillType.SIMPLE,
                                TumuziGuruma.class, List.of(KATANA), false, 15, DefaultTransformTick,
                                SkillColor.NEON_RED, "simple_2", 15, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "gengetu", 200, 30.0, SkillData.SkillType.RUSH,
                                GenGetu.class,
                                List.of(KATANA), false, 20, DefaultTransformTick,
                                SkillColor.NEON_RED, "simple_2", 15, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "hiougi", 300, 50.0, SkillData.SkillType.SIMPLE,
                                Hiougi.class,
                                List.of(KATANA), false, 16, DefaultTransformTick,
                                SkillColor.NEON_RED, "simple_2", 20, 1.6f, 3.2f));
                // 大剣
                registerSkill(new SkillData(getNextSkillId(), "cascade", 80, 8.0, SkillData.SkillType.SIMPLE,
                                CasCade.class,
                                List.of(TWO_HANDED_SWORD), false, 3, DefaultTransformTick,
                                0xFFFF4400, "simple_2", 15, 2.0f, 4.0f));
                registerSkill(new SkillData(getNextSkillId(), "aban_rash", 120, 15.0, SkillData.SkillType.SIMPLE,
                                AbanRash.class,
                                List.of(TWO_HANDED_SWORD), false, 7, DefaultTransformTick,
                                0xFFFF4400, "simple_2", 15, 2.0f, 4.0f));
                registerSkill(new SkillData(getNextSkillId(), "cyclone", 140, 20.0, SkillData.SkillType.SIMPLE,
                                Cyclone.class,
                                List.of(TWO_HANDED_SWORD), false, 5, DefaultTransformTick,
                                0xFFFF4400, "simple_2", 15, 2.0f, 4.0f));
                registerSkill(new SkillData(getNextSkillId(), "stan_hazard", 250, 25.0, SkillData.SkillType.SIMPLE,
                                StanHazard.class,
                                List.of(TWO_HANDED_SWORD), false, 32, DefaultTransformTick,
                                0xFFFF4400, "simple_2", 15, 2.0f, 4.0f));
                registerSkill(new SkillData(getNextSkillId(), "slash_blade", 150, 20.0, SkillData.SkillType.SIMPLE,
                                SlashBlade.class,
                                List.of(TWO_HANDED_SWORD), false, 22, DefaultTransformTick,
                                0xFFFF4400, "simple_2", 15, 2.0f, 4.0f));
                // 細剣（レイピア）
                registerSkill(new SkillData(getNextSkillId(), "linear", 80, 6.0, SkillData.SkillType.SIMPLE,
                                Linear.class,
                                List.of(ONE_HANDED_SWORD, RAPIER), false, 0, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 15, 1.4f, 2.8f));
                registerSkill(new SkillData(getNextSkillId(), "parallel_sting", 120, 10.0, SkillData.SkillType.SIMPLE,
                                ParallelSting.class, List.of(RAPIER), false, 10, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 15, 1.4f, 2.8f));
                registerSkill(new SkillData(getNextSkillId(), "quadraple_pain", 200, 14.0, SkillData.SkillType.SIMPLE,
                                QuadraplePain.class, List.of(RAPIER), false, 14, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 15, 1.4f, 2.8f));
                registerSkill(new SkillData(getNextSkillId(), "cruci_fiction", 200, 20.0, SkillData.SkillType.SIMPLE,
                                Crucifiction.class, List.of(RAPIER), false, 29, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 15, 1.4f, 2.8f));
                registerSkill(new SkillData(getNextSkillId(), "star_splash", 250, 25.0, SkillData.SkillType.SIMPLE,
                                StarSplash.class,
                                List.of(RAPIER), false, 31, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 15, 1.4f, 2.8f));
                registerSkill(new SkillData(getNextSkillId(), "stary_tier", 200, 20.0, SkillData.SkillType.SIMPLE,
                                StaryTier.class,
                                List.of(RAPIER), false, 21, DefaultTransformTick,
                                SkillColor.ETHEREAL_PURPLE, "simple_2", 15, 1.4f, 3.0f));
                registerSkill(new SkillData(getNextSkillId(), "mothers_rosario", 350, 30.0, SkillData.SkillType.SIMPLE,
                                MothersRosario.class, List.of(RAPIER), false, 37, DefaultTransformTick,
                                SkillColor.ETHEREAL_PURPLE, "simple_2", 25, 1.6f, 3.2f));
                registerSkill(new SkillData(getNextSkillId(), "shooting_star", 150, 35.0, SkillData.SkillType.SIMPLE,
                                ShootingStar.class, List.of(RAPIER), false, 4, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 15, 1.4f, 2.8f));
                registerSkill(new SkillData(getNextSkillId(), "flashing_penetrator", 400, 40.0,
                                SkillData.SkillType.SIMPLE,
                                FlashingPenetrator.class, List.of(RAPIER), false, 6, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 15, 1.4f, 2.8f));
                // 短剣
                registerSkill(new SkillData(getNextSkillId(), "canine", 60, 6.0, SkillData.SkillType.SIMPLE,
                                Canine.class,
                                List.of(DAGGER), false, 0, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 10, 1.2f, 2.4f));
                registerSkill(new SkillData(getNextSkillId(), "face_liner", 100, 10.0, SkillData.SkillType.SIMPLE,
                                FaceLiner.class,
                                List.of(DAGGER), false, 16, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 10, 1.2f, 2.4f));
                registerSkill(new SkillData(getNextSkillId(), "fad_edge", 150, 18.0, SkillData.SkillType.SIMPLE,
                                FadEdge.class,
                                List.of(DAGGER), false, 17, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 10, 1.2f, 2.4f));
                registerSkill(new SkillData(getNextSkillId(), "rapid_bite", 150, 20.0, SkillData.SkillType.RUSH,
                                RapidBite.class,
                                List.of(DAGGER), false, 200, DefaultTransformTick,
                                SkillColor.LIME_GLOW, "simple_2", 15, 1.2f, 2.4f));
                registerSkill(new SkillData(getNextSkillId(), "octagon_edge", 160, 20.0, SkillData.SkillType.SIMPLE,
                                OctagonEdge.class, List.of(DAGGER), false, 18, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 10, 1.2f, 2.4f));
                registerSkill(new SkillData(getNextSkillId(), "kodati", 200, 15.0, SkillData.SkillType.SIMPLE,
                                Kodati.class,
                                List.of(DAGGER), false, 11, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 10, 1.2f, 2.4f));
                // 鎌
                registerSkill(new SkillData(getNextSkillId(), "barrick", 80, 8.0, SkillData.SkillType.SIMPLE,
                                Barrick.class,
                                List.of(SCYTHE), false, 0, DefaultTransformTick,0xFFF01616, "simple_2", 12, 2.4f, 4.0f));
                registerSkill(new SkillData(getNextSkillId(), "reaper_of_life", 10, 15.0, SkillData.SkillType.SIMPLE,
                                ReaperOfLife.class, List.of(SCYTHE), false, 12, DefaultTransformTick,0xFFecf447, "simple_2", 12, 2.4f, 4.0f));
                registerSkill(new SkillData(getNextSkillId(), "reaper_of_death", 120, 15.0, SkillData.SkillType.SIMPLE,
                                ReaperOfDeath.class, List.of(SCYTHE), false, 12, DefaultTransformTick,0xFF110220, "simple_2", 12, 2.4f, 4.0f));
                registerSkill(new SkillData(getNextSkillId(), "shadow_scythe", 160, 25.0, SkillData.SkillType.SIMPLE,
                                ShadowScythe.class, List.of(SCYTHE), false, 7, DefaultTransformTick,0xFFa30dd8, "simple_2", 12, 2.4f, 4.0f));
                registerSkill(new SkillData(getNextSkillId(), "requiem", 210, 20.0, SkillData.SkillType.SIMPLE,
                                Requiem.class,
                                List.of(SCYTHE), false, 26, DefaultTransformTick,0xFFa30dd8, "simple_2", 12, 2.4f, 4.0f));
                registerSkill(new SkillData(getNextSkillId(), "storm", 105, 10.0, SkillData.SkillType.TRANSFORM,
                                Storm.class,
                                List.of(SCYTHE), false, 10, DefaultTransformTick,0xFFa30dd8, "simple_2", 12, 2.4f, 4.0f));
                registerSkill(new SkillData(getNextSkillId(), "storm_mirror", 170, 10.0,
                                SkillData.SkillType.TRANSFORM_FINISH,
                                StormMirror.class, List.of(SCYTHE), true, 10, DefaultTransformTick,0xFFa30dd8, "simple_2", 12, 2.4f, 4.0f));
                registerSkill(new SkillData(getNextSkillId(), "judgment", 300, 30.0, SkillData.SkillType.SIMPLE,
                                Judgment.class,
                                List.of(SCYTHE), false, 38, DefaultTransformTick,0xFFa30dd8, "simple_2", 12, 2.4f, 4.0f));
                // 爪
                registerSkill(new SkillData(getNextSkillId(), "talon", 70, 4.0, SkillData.SkillType.SIMPLE,
                                Talon.class,
                                List.of(CLAW), false, 0, DefaultTransformTick
                        ,0xFF2af32b, "simple_2", 10, 1.2f, 2.4f));
                registerSkill(new SkillData(getNextSkillId(), "hant", 140, 6.0, SkillData.SkillType.TRANSFORM,
                                Hant.class,
                                List.of(CLAW), false, 0, DefaultTransformTick,0xFF2af32b, "simple_2", 10, 1.2f, 2.4f));
                registerSkill(new SkillData(getNextSkillId(), "hant_killer", 200, 12.0,
                                SkillData.SkillType.TRANSFORM_FINISH,
                                HantKiller.class, List.of(CLAW), true, 16, DefaultTransformTick,0xFF2af32b, "simple_2", 10, 1.2f, 2.4f));
                registerSkill(new SkillData(getNextSkillId(), "tiger_claw", 160, 15.0, SkillData.SkillType.SIMPLE,
                                TigerClaw.class,
                                List.of(CLAW), false, 14, DefaultTransformTick,0xFF2af32b, "simple_2", 10, 1.2f, 2.4f));
                registerSkill(new SkillData(getNextSkillId(), "dragon_claw", 160, 20.0, SkillData.SkillType.SIMPLE,
                                DragonClaw.class,
                                List.of(CLAW), false, 10, DefaultTransformTick,0xFF2af32b, "simple_2", 10, 1.2f, 2.4f));
                registerSkill(new SkillData(getNextSkillId(), "beast_claw", 200, 20.0, SkillData.SkillType.SIMPLE,
                                BeastClaw.class,
                                List.of(CLAW), false, 9, DefaultTransformTick,0xFFF01616, "simple_2", 10, 1.2f, 2.4f));
                registerSkill(new SkillData(getNextSkillId(), "golden_claw", 225, 30.0, SkillData.SkillType.SIMPLE,
                                GoldClaw.class,
                                List.of(CLAW), false, 5, DefaultTransformTick,0xFFf4d43b, "simple_2", 10, 1.2f, 2.4f));
                registerSkill(new SkillData(getNextSkillId(), "fast", 100, 10.0, SkillData.SkillType.RUSH, Fast.class,
                                List.of(CLAW),
                                false, 10, DefaultTransformTick,0xFF2af32b, "simple_2", 10, 1.2f, 2.4f));
                // メイス
                registerSkill(new SkillData(getNextSkillId(), "grand", 75, 10.0, SkillData.SkillType.SIMPLE,
                                Grand.class,
                                List.of(MACE), false, 0, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 12, 1.8f, 3.6f));
                registerSkill(new SkillData(getNextSkillId(), "double_creep", 100, 20.0, SkillData.SkillType.SIMPLE,
                                DoubleCreep.class, List.of(MACE), false, 10, DefaultTransformTick,
                                0xFFAAFF00, "simple_2", 12, 1.8f, 3.6f));
                registerSkill(new SkillData(getNextSkillId(), "hammer_down", 325, 40.0, SkillData.SkillType.SIMPLE,
                                HammerDown.class,
                                List.of(MACE), false, 8, DefaultTransformTick,
                                SkillColor.ELECTRIC_BLUE, "simple_2", 15, 2.0f, 4.0f));
                registerSkill(new SkillData(getNextSkillId(), "diastrophism", 240, 30.0, SkillData.SkillType.SIMPLE,
                                Diastrophism.class, List.of(MACE), false, 18, DefaultTransformTick,
                                0xFFFF00FF, "simple_2", 18, 2.2f, 4.4f));
                registerSkill(new SkillData(getNextSkillId(), "mace_impact", 450, 60.0, SkillData.SkillType.RUSH,
                                MaceImpact.class,
                                List.of(MACE), false, 200, DefaultTransformTick,
                                0xFF33FF99, "simple_2", 20, 2.4f, 4.8f));
 
                // 体術
                registerSkill(new SkillData(getNextSkillId(), "senda", 20, 10.0, SkillData.SkillType.SIMPLE,
                                Senda.class, List.of(MARTIAL_ARTS), false, 15, DefaultTransformTick,
                                0xFFFFAB00, "simple_2", 10, 1.2f, 2.4f).setFollowBone(FollowBone.RIGHT_HAND));
                registerSkill(new SkillData(getNextSkillId(), "gaken", 40, 4.0, SkillData.SkillType.SIMPLE,
                                Gaken.class, List.of(MARTIAL_ARTS), false, 20, DefaultTransformTick,
                                0xFFD500F9, "simple_2", 12, 1.2f, 2.4f).setFollowBone(FollowBone.LEFT_LEG));
                registerSkill(new SkillData(getNextSkillId(), "tackle_impact", 60, 30.0, SkillData.SkillType.RUSH,
                                TackleImpact.class, List.of(MARTIAL_ARTS), false, 25, DefaultTransformTick,
                                0xFFFFD700, "simple_2", 15, 1.2f, 2.4f).setFollowBone(FollowBone.BOTH_LEGS));
                registerSkill(new SkillData(getNextSkillId(), "gengetu_martial", 40, 15.0, SkillData.SkillType.SIMPLE,
                                GengetuMartial.class, List.of(MARTIAL_ARTS), false, 15, DefaultTransformTick,
                                0xFF99EA22, "simple_5", 15, 1.2f, 2.9f).setFollowBone(FollowBone.BOTH_LEGS));
        }

        private static int getNextSkillId() {
                return nextSkillId++;
        }

        private static void registerSkill(SkillData skillData) {
                SKILLS.put(skillData.getId(), skillData);
        }
}
//バーチカルsquare