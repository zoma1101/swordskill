package com.zoma1101.SwordSkill.swordskills;

import java.util.HashSet;
import java.util.Set;

public class SkillTexture {


    public static String NomalSkillTexture() {
        return "skill_particle_sword/blue";
    }
    public static String RedSkillTexture() {
        return "skill_particle_sword/red";
    }
    public static String YellowSkillTexture() {
        return "skill_particle_sword/holy";
    }
    public static String BlackSkillTexture() {
        return "skill_particle_sword/black";
    }
    public static String PurpleSkillTexture() {
        return "skill_particle_sword/purple";
    }
    public static String GreenSkillTexture() {
        return "skill_particle_sword/green";
    }
    public static String GoldSkillTexture() {
        return "skill_particle_sword/gold";
    }

    public static String AxeBlueSkillTexture() {
        return "axe_particle/simple_blue";
    }
    public static String AxeGreenSkillTexture() {
        return "axe_particle/simple_green";
    }
    public static String AxeBloodSkillTexture() {
        return "axe_particle/simple_blackred";
    }
    public static String AxeRedSkillTexture() {
        return "axe_particle/simple_red";
    }
    public static String AxeKingSkillTexture() {
        return "axe_particle/simple_king";
    }
    public static String AxePinkSkillTexture() {
        return "axe_particle/simple_pink";
    }
    public static String AxePurpleSkillTexture() {
        return "axe_particle/simple_purple";
    }

    public static String Spia_Particle() {
        return "spear_particle/simple_blue";
    }
    public static String Spia_Particle_red() {
        return "spear_particle/simple_red";
    }
    public static String Spia_Particle_AxeGreen() {
        return "spear_particle/simple_green";
    }
    public static String Spia_Particle_SoftRed() {
        return "spear_particle/simple_softred";
    }
    public static String Spia_Particle_Purple() {
        return "spear_particle/simple_purple";
    }
    public static String FlashingPenetrator_Texture() {
        return "spear_particle/super_blue";
    }

    public static String MaceBlue_Texture() {
        return "mace_particle/blue";
    }
    public static String MaceGreen_Texture() {
        return "mace_particle/green";
    }
    public static String MacePurple_Texture() {
        return "mace_particle/purple";
    }



    public static String BlueRollTexture() {
        return "skill_particle_sword/roll_blue";
    }




    public static Set<String> Spia_ParticleType = new HashSet<>();
    public static Set<String> Axe_ParticleType = new HashSet<>();
    public static Set<String> Simple_ParticleType = new HashSet<>();
    public static Set<String> Mace_ParticleType = new HashSet<>();
    static {
        Spia_ParticleType.add(Spia_Particle());
        Spia_ParticleType.add(Spia_Particle_red());
        Spia_ParticleType.add(Spia_Particle_AxeGreen());
        Spia_ParticleType.add(Spia_Particle_SoftRed());
        Spia_ParticleType.add(Spia_Particle_Purple());

        Axe_ParticleType.add(AxeBlueSkillTexture());
        Axe_ParticleType.add(AxeGreenSkillTexture());
        Axe_ParticleType.add(AxeBloodSkillTexture());
        Axe_ParticleType.add(AxeRedSkillTexture());
        Axe_ParticleType.add(AxeKingSkillTexture());
        Axe_ParticleType.add(AxePinkSkillTexture());
        Axe_ParticleType.add(AxePurpleSkillTexture());

        Simple_ParticleType.add(NomalSkillTexture());
        Simple_ParticleType.add(RedSkillTexture());
        Simple_ParticleType.add(YellowSkillTexture());
        Simple_ParticleType.add(BlackSkillTexture());
        Simple_ParticleType.add(PurpleSkillTexture());
        Simple_ParticleType.add(GreenSkillTexture());
        Simple_ParticleType.add(GoldSkillTexture());

        Mace_ParticleType.add(MaceBlue_Texture());
        Mace_ParticleType.add(MaceGreen_Texture());
        Mace_ParticleType.add(MacePurple_Texture());
    }
}
