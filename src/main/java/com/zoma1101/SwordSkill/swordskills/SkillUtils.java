package com.zoma1101.swordskill.swordskills;
 
import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.data.WeaponTypeUtils;
import com.zoma1101.swordskill.entity.SwordSkill_Entities;
import com.zoma1101.swordskill.entity.custom.AttackEffectEntity;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
 
import net.minecraftforge.network.PacketDistributor;
import com.zoma1101.swordskill.network.NetworkHandler;
import com.zoma1101.swordskill.network.toClient.SyncTrailActivePacket;
 
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
 
public class SkillUtils {
    public static void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size, LivingEntity owner,
            double damage, double knockback, int duration, String skill_particle, Vec3 Movement, boolean followOwner) {
        spawnAttackEffect(level, position, rotation, size, owner, damage, knockback, duration, skill_particle,
                java.util.Collections.emptyList(), Movement, followOwner);
    }
 
    public static void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size, LivingEntity owner,
            double damage, double knockback, int duration, String skill_particle, java.util.List<SkillTag> tags,
            Vec3 Movement, boolean followOwner) {
        
        // 1. 基本となる色を決定 (デフォルト -> スキルの設定色)
        int trailColor = 0xFF33AAFF;
        java.util.List<SkillTag> finalTags = tags;
        float trailLen = 0f;
        boolean isThrustShape = size.x() < 1.0f && size.y() < 1.0f;
 
        if (!level.isClientSide && owner instanceof ServerPlayer player) {
            com.zoma1101.swordskill.server.handler.SkillExecutionManager.SkillExecutionData data = 
                com.zoma1101.swordskill.server.handler.SkillExecutionManager.skillExecutions.get(player.getUUID());
            
            if (data != null) {
                SkillData skillData = SwordSkillRegistry.SKILLS.get(data.skillId);
                if (skillData != null) {
                    trailColor = skillData.getTrailColor();
                    trailLen = skillData.getTrailTipOffset() - skillData.getTrailBaseOffset();
                    
                    // 特殊なスキル名に基づくタグ付与 (RAY 等)
                    boolean isThrustType = isThrustShape || skillData.getName().contains("thrust") || 
                                           skillData.getName().contains("pierce") || skillData.getName().contains("spike") ||
                                           skillData.getName().contains("linear");
                    boolean isSpecialAlways = skillData.getName().equals("vorpal_strike") 
                                           || skillData.getName().equals("rapid_bite");
 
                    if (isThrustType || isSpecialAlways) {
                        if (!finalTags.contains(SkillTag.RAY)) {
                            finalTags = new java.util.ArrayList<>(finalTags);
                            finalTags.add(SkillTag.RAY);
                        }
                        if (isSpecialAlways && !finalTags.contains(SkillTag.POWERFUL_THRUST)) {
                            if (finalTags == tags) finalTags = new java.util.ArrayList<>(finalTags);
                            finalTags.add(SkillTag.POWERFUL_THRUST);
                        }
                    }
                }
            }
 
            // 2. エンチャントによる色と属性タグの上書き (常に実行)
            Object[] result = getEffectiveEffectSettings(player, trailColor, finalTags);
            trailColor = (int) result[0];
            finalTags = (java.util.List<SkillTag>) result[1];
 
            // 3. 形状に基づく自動タグ付与
            if (SkillTexture.Spia_ParticleType.contains(skill_particle) && isThrustShape && !finalTags.contains(SkillTag.SHAPE_THRUST)) {
                if (finalTags == tags) finalTags = new java.util.ArrayList<>(finalTags);
                finalTags.add(SkillTag.SHAPE_THRUST);
            }
 
            // 4. 突きスキルの場合、波動の大きさを武器の長さに合わせる
            if (trailLen > 0 && (SkillTexture.Spia_ParticleType.contains(skill_particle) || finalTags.contains(SkillTag.RAY)) && isThrustShape) {
                size.set(trailLen * 0.66f, trailLen * 0.66f, size.z());
            }
        }
 
        spawnAttackEffect(level, position, rotation, size, owner, damage, knockback, duration, skill_particle, finalTags,
                trailColor, Movement, followOwner);
    }
 
    public static void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size, LivingEntity owner,
            double damage, double knockback, int duration, String skill_particle, java.util.List<SkillTag> tags,
            int trailColor, Vec3 Movement, boolean followOwner) {
        spawnAttackEffect(level, position, rotation, size, owner, damage, knockback, duration, skill_particle, tags,
                trailColor, Movement, followOwner, true); // デフォルトは相対
    }
 
    public static void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size, LivingEntity owner,
            double damage, double knockback, int duration, String skill_particle, java.util.List<SkillTag> tags,
            int trailColor, Vec3 Movement, boolean followOwner, boolean isRelativeRotation) {
 
        // 引数の trailColor にかかわらず、エンチャントがあればそれを最優先する
        Object[] result = getEffectiveEffectSettings(owner, trailColor, tags);
        int finalColor = (int) result[0];
        java.util.List<SkillTag> finalTags = (java.util.List<SkillTag>) result[1];
 
        if (!level.isClientSide) {
            AttackEffectEntity effect = new AttackEffectEntity(SwordSkill_Entities.ATTACK_EFFECT.get(), level);
            effect.setPos(position.x, position.y, position.z);
            effect.setRotation((float) rotation.z);
            
            if (isRelativeRotation) {
                effect.setXRot((float) (owner.getXRot() + rotation.x));
                effect.setYRot((float) (owner.getYRot() + rotation.y));
            } else {
                effect.setXRot((float) rotation.x);
                effect.setYRot((float) rotation.y);
            }
            
            effect.setOwner(owner);
            effect.setDamage(damage);
            effect.setKnockbackStrength(knockback);
            effect.setEffectRadius(size);
            effect.setSkillParticle(skill_particle);
            effect.setSkillTags(finalTags);
            effect.setTrailColor(finalColor);
            effect.setDuration(duration);
            effect.setMovement(Movement);
            effect.setFollowOwner(followOwner);
            level.addFreshEntity(effect);
            SwingArm((ServerPlayer) owner);
        }
    }
 
    public static void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size, LivingEntity owner,
            double damage, double knockback, int duration, String skill_particle, Vec3 Movement) {
        spawnAttackEffect(level, position, rotation, size, owner, damage, knockback, duration, skill_particle, Movement,
                false);
    }
 
    public static double BaseDamage(ServerPlayer player) {
        if (Objects.requireNonNull(WeaponTypeUtils.getWeaponType(player)).contains(SkillData.WeaponType.DUALSWORD)) {
            ItemStack mainHandItem = player.getMainHandItem();
            ItemStack offHandItem = player.getOffhandItem();
            double mainHandDamage = mainHandItem.getAttributeModifiers(EquipmentSlot.MAINHAND)
                    .get(Attributes.ATTACK_DAMAGE).stream().mapToDouble(attributeModifier -> attributeModifier.getAmount()).sum();
            double offHandDamage = offHandItem.getAttributeModifiers(EquipmentSlot.MAINHAND)
                    .get(Attributes.ATTACK_DAMAGE).stream().mapToDouble(attributeModifier -> attributeModifier.getAmount()).sum();
            return (mainHandDamage + offHandDamage + 1) * ServerConfig.damageMultiplier.get();
        } else {
            return (player.getAttributeValue(Attributes.ATTACK_DAMAGE))
                    * ServerConfig.damageMultiplier.get();
        }
    }
 
    public static float BaseKnowBack(ServerPlayer player) {
        return (float) Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_KNOCKBACK)).getValue() + 1;
    }
 
    public static double RushDamage(ServerPlayer player) {
        double MoveX_ABS = Math.abs(player.getDeltaMovement().x);
        double MoveY_ABS = Math.abs(player.getDeltaMovement().y);
        double MoveZ_ABS = Math.abs(player.getDeltaMovement().z);
        float Motion = (float) (MoveX_ABS + MoveY_ABS + MoveZ_ABS) * 3f;
        float Motion_BaseDamage = (float) (MoveX_ABS + MoveY_ABS + MoveZ_ABS) * 0.5f + 1;
        return BaseDamage(player) * Motion_BaseDamage + Motion;
    }
 
    public static void swingArm(ServerPlayer player, HumanoidArm arm) {
        InteractionHand hand = arm == HumanoidArm.LEFT ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        player.swing(hand);
        player.connection.send(new ClientboundAnimatePacket(player, arm == HumanoidArm.LEFT ? 3 : 0));
    }
 
    private static final Map<UUID, Boolean> swingRight = new HashMap<>();
    private static final Map<UUID, Integer> lastSwingTick = new HashMap<>();
 
    public static void SwingArm(ServerPlayer player) {
        if (lastSwingTick.getOrDefault(player.getUUID(), -1) == player.tickCount) {
            return;
        }
        lastSwingTick.put(player.getUUID(), player.tickCount);
        if (Objects.requireNonNull(WeaponTypeUtils.getWeaponType(player)).contains(SkillData.WeaponType.DUALSWORD)
                || Objects.requireNonNull(WeaponTypeUtils.getWeaponName(player)).contains("dual_claw")) {
            UUID playerId = player.getUUID();
            boolean isSwingRight = swingRight.getOrDefault(playerId, true);
 
            if (isSwingRight) {
                player.connection.send(new ClientboundAnimatePacket(player, 0));
            } else {
                player.connection.send(new ClientboundAnimatePacket(player, 3));
            }
 
            swingRight.put(playerId, !isSwingRight);
        } else {
            swingArm(player, player.getMainArm());
        }
    }
 
    public static Boolean SkillTargetEntity(LivingEntity entity, LivingEntity owner) {
        if (entity == owner) {
            return false;
        } else if (!ServerConfig.PvP.get() && entity instanceof Player) {
            return false;
        } else if (entity.getType().getCategory() == MobCategory.CREATURE
                || entity.getType().getCategory() == MobCategory.AMBIENT) {
            return ServerConfig.attackNeutralMobs.get();
        } else {
            return true;
        }
    }
 
    public static Vec3 rotateLookVec(Entity entity, double pitch, double yaw) {
        float RotX = (float) (entity.getXRot() + pitch);
        float RotY = (float) (entity.getYRot() + yaw);
        float f = RotX * ((float) Math.PI / 180F);
        float f1 = -RotY * ((float) Math.PI / 180F);
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        return new Vec3(f3 * f4, -f5, f2 * f4);
    }
 
    public static void setTrailActive(ServerPlayer player, boolean active) {
        NetworkHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new SyncTrailActivePacket(player.getId(), active));
    }
 
    public static Object[] getEffectiveEffectSettings(LivingEntity owner, int defaultColor, java.util.List<SkillTag> tags) {
        int trailColor = defaultColor;
        java.util.List<SkillTag> finalTags = tags;
 
        if (owner instanceof Player player) {
            net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
            int fire = stack.getEnchantmentLevel(com.zoma1101.swordskill.enchantment.ModEnchantments.ENMA_JIN.get());
            int water = stack.getEnchantmentLevel(com.zoma1101.swordskill.enchantment.ModEnchantments.SUI_JIN.get());
            int wind = stack.getEnchantmentLevel(com.zoma1101.swordskill.enchantment.ModEnchantments.FU_JIN.get());
            int holy = stack.getEnchantmentLevel(com.zoma1101.swordskill.enchantment.ModEnchantments.KO_JIN.get());
            int dark = stack.getEnchantmentLevel(com.zoma1101.swordskill.enchantment.ModEnchantments.AN_JIN.get());
            int soul = stack.getEnchantmentLevel(com.zoma1101.swordskill.enchantment.ModEnchantments.KON_JIN.get());
 
            SkillTag elementTag = null;
            if (fire > 0) { trailColor = 0xFFFE210C; elementTag = SkillTag.FIRE; }
            else if (water > 0) { trailColor = 0xFF0690F8; elementTag = SkillTag.WATER; }
            else if (wind > 0) { trailColor = 0xFF27D480; elementTag = SkillTag.WIND; }
            else if (holy > 0) { trailColor = 0xFFFFD700; elementTag = SkillTag.HOLY_ENCHANT; }
            else if (dark > 0) { trailColor = 0xFF1A1A1A; elementTag = SkillTag.DARK; }
            else if (soul > 0) { trailColor = 0xFF27D4C6; elementTag = SkillTag.SONIC; }
 
            if (elementTag != null && !finalTags.contains(elementTag)) {
                finalTags = new java.util.ArrayList<>(finalTags);
                finalTags.add(elementTag);
            }
        }
        return new Object[]{trailColor, finalTags};
    }
}
