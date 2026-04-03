package com.zoma1101.swordskill.item;

import com.zoma1101.swordskill.capability.PlayerSkillsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MartialArtsScrollItem extends Item {
    public MartialArtsScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(skills -> {
                if (!skills.isMartialArtsUnlocked()) {
                    skills.setMartialArtsUnlocked(true);
                    player.displayClientMessage(Component.translatable("message.swordskill.martial_arts_unlocked"), true);
                    
                    // 演出効果: トーテム使用音
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), 
                        net.minecraft.sounds.SoundEvents.TOTEM_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                    
                    // 演出効果: エンドロッドのパーティクル散布
                    if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, 
                            player.getX(), player.getY() + 1.0, player.getZ(), 50, 0.5, 1.0, 0.5, 0.1);
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, 
                            player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.1, 0.1, 0.1, 0.0);
                    }

                    if (!player.getAbilities().instabuild) {
                        itemstack.shrink(1);
                    }
                } else {
                    player.displayClientMessage(Component.translatable("message.swordskill.martial_arts_already_unlocked"), true);
                }
            });
        }
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }
}
