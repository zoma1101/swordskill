package com.zoma1101.swordskill.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerSkills {

    // 習得したスキルのIDリスト
    private final Set<Integer> unlockedSkills = new HashSet<>();

    // 武器種ごとのスキルスロット
    private final Map<String, int[]> skillSlots = new HashMap<>();

    // 現在選択中のスロット番号 (0~4)
    private int selectedSlot = 0;

    // ★追加: データ移行済みフラグ
    private boolean isMigrated = false;

    // --- スキル習得関連 ---
    public void unlockSkill(int skillId) {
        unlockedSkills.add(skillId);
    }

    public boolean isSkillUnlocked(int skillId) {
        return unlockedSkills.contains(skillId);
    }

    public Set<Integer> getUnlockedSkills() {
        return new HashSet<>(unlockedSkills);
    }

    // --- スキルスロット関連 ---
    public void setSkillSlot(String weaponName, int slotIndex, int skillId) {
        int[] slots = skillSlots.computeIfAbsent(weaponName, k -> new int[5]);
        if (slotIndex >= 0 && slotIndex < slots.length) {
            slots[slotIndex] = skillId;
        }
    }

    public int[] getSkillSlots(String weaponName) {
        return skillSlots.getOrDefault(weaponName, new int[5]);
    }

    // --- 選択スロット関連 ---
    public void setSelectedSlot(int index) {
        if (index < 0) index = 0;
        if (index > 4) index = 4;
        this.selectedSlot = index;
    }

    public int getSelectedSlot() {
        return this.selectedSlot;
    }

    // --- ★追加: 移行フラグ関連 ---
    public boolean isMigrated() {
        return isMigrated;
    }

    public void setMigrated(boolean migrated) {
        this.isMigrated = migrated;
    }

    // --- データ管理関連 ---
    public void copyFrom(PlayerSkills source) {
        this.unlockedSkills.clear();
        this.unlockedSkills.addAll(source.unlockedSkills);

        this.skillSlots.clear();
        source.skillSlots.forEach((key, value) -> this.skillSlots.put(key, value.clone()));

        this.selectedSlot = source.selectedSlot;

        // ★追加: 移行フラグのコピー
        this.isMigrated = source.isMigrated;
    }

    public void saveNBT(CompoundTag nbt) {
        // 習得スキルの保存
        ListTag list = new ListTag();
        for (Integer id : unlockedSkills) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("id", id);
            list.add(tag);
        }
        nbt.put("UnlockedSkills", list);

        // スキルスロットの保存
        CompoundTag slotsTag = new CompoundTag();
        skillSlots.forEach(slotsTag::putIntArray);
        nbt.put("SkillSlots", slotsTag);

        // 選択スロットの保存
        nbt.putInt("SelectedSlot", selectedSlot);

        // ★追加: 移行フラグの保存
        nbt.putBoolean("IsMigrated", isMigrated);
    }

    public void loadNBT(CompoundTag nbt) {
        // 習得スキルの読み込み
        unlockedSkills.clear();
        if (nbt.contains("UnlockedSkills")) {
            ListTag list = nbt.getList("UnlockedSkills", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                unlockedSkills.add(list.getCompound(i).getInt("id"));
            }
        }

        // スキルスロットの読み込み
        skillSlots.clear();
        if (nbt.contains("SkillSlots")) {
            CompoundTag slotsTag = nbt.getCompound("SkillSlots");
            for (String key : slotsTag.getAllKeys()) {
                skillSlots.put(key, slotsTag.getIntArray(key));
            }
        }

        // 選択スロットの読み込み
        if (nbt.contains("SelectedSlot")) {
            this.selectedSlot = nbt.getInt("SelectedSlot");
        }

        // ★追加: 移行フラグの読み込み
        if (nbt.contains("IsMigrated")) {
            this.isMigrated = nbt.getBoolean("IsMigrated");
        }
    }
}