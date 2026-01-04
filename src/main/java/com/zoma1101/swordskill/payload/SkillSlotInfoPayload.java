package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.ClientSkillSlotHandler;
import com.zoma1101.swordskill.swordskills.SkillData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

public record SkillSlotInfoPayload(int[] skillIds, String weaponName, Set<SkillData.WeaponType> weaponTypes)
        implements CustomPacketPayload {

    public static final Type<SkillSlotInfoPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "skill_slot_info"));

    public static final StreamCodec<FriendlyByteBuf, SkillSlotInfoPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, SkillSlotInfoPayload payload) {
            buf.writeVarIntArray(payload.skillIds);
            buf.writeUtf(payload.weaponName);
            buf.writeVarInt(payload.weaponTypes.size());
            for (SkillData.WeaponType type : payload.weaponTypes) {
                buf.writeEnum(type);
            }
        }

        @Override
        public @NotNull SkillSlotInfoPayload decode(FriendlyByteBuf buf) {
            int[] skillIds = buf.readVarIntArray();
            String weaponName = buf.readUtf();
            int typeCount = buf.readVarInt();
            Set<SkillData.WeaponType> weaponTypes = EnumSet.noneOf(SkillData.WeaponType.class);
            for (int i = 0; i < typeCount; i++) {
                weaponTypes.add(buf.readEnum(SkillData.WeaponType.class));
            }
            return new SkillSlotInfoPayload(skillIds, weaponName, weaponTypes);
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(SkillSlotInfoPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // クライアント側専用処理（安全に実行）
            if (ctx.flow().isClientbound()) {
                ClientSkillSlotHandler.updateAllInfo(msg.skillIds(), msg.weaponName(), msg.weaponTypes());
            }
        });
    }
}
