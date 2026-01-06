package com.zoma1101.swordskill.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerSkillsProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<PlayerSkills> PLAYER_SKILLS = CapabilityManager.get(new CapabilityToken<>() {});

    private PlayerSkills backend = null;
    private final LazyOptional<PlayerSkills> optional = LazyOptional.of(this::createPlayerSkills);

    private PlayerSkills createPlayerSkills() {
        if (this.backend == null) {
            this.backend = new PlayerSkills();
        }
        return this.backend;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == PLAYER_SKILLS) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createPlayerSkills().saveNBT(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createPlayerSkills().loadNBT(nbt);
    }
}