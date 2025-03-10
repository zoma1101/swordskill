package com.zoma1101.SwordSkill.swordskills;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;


public class SkillSound {

    public static void SimpleSkillSound(Level level, Vec3 SoundPos) {
        // 攻撃音を再生
        level.playSound(
                null, // 再生するプレイヤー（nullの場合はワールド全体に再生）
                SoundPos.x, // X座標
                SoundPos.y, // Y座標
                SoundPos.z, // Z座標
                SoundEvents.PLAYER_ATTACK_SWEEP, // 再生する音の種類
                net.minecraft.sounds.SoundSource.PLAYERS, // 音源の種類
                1.0f, // 音量
                1.47f // ピッチ
        );
    }

    public static void StrongSkillSound(Level level, Vec3 SoundPos) {
        // 攻撃音を再生
        level.playSound(
                null, // 再生するプレイヤー（nullの場合はワールド全体に再生）
                SoundPos.x, // X座標
                SoundPos.y, // Y座標
                SoundPos.z, // Z座標
                SoundEvents.PLAYER_ATTACK_STRONG, // 再生する音の種類
                net.minecraft.sounds.SoundSource.PLAYERS, // 音源の種類
                1.0f, // 音量
                0.6f // ピッチ
        );
    }
    public static void GodSkillSound(Level level, Vec3 SoundPos) {
        // 攻撃音を再生
        level.playSound(
                null, // 再生するプレイヤー（nullの場合はワールド全体に再生）
                SoundPos.x, // X座標
                SoundPos.y, // Y座標
                SoundPos.z, // Z座標
                SoundEvents.TOTEM_USE, // 再生する音の種類
                net.minecraft.sounds.SoundSource.PLAYERS, // 音源の種類
                1.0f, // 音量
                0.6f // ピッチ
        );
    }



}
