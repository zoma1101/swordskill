package com.zoma1101.swordskill.network;

import com.zoma1101.swordskill.client.handler.ClientSkillSlotHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SkillSlotInfoPacket {
    private final int[] skillIds;

    public SkillSlotInfoPacket(int[] skillIds) {
        this.skillIds = skillIds;
    }

    public SkillSlotInfoPacket(FriendlyByteBuf buf) {
        this.skillIds = buf.readVarIntArray();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarIntArray(skillIds);
    }

    public static void handle(SkillSlotInfoPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // クライアントサイドでスキルスロット情報を処理
            ClientSkillSlotHandler.setSkillSlotInfo(msg.skillIds);
        });
        ctx.get().setPacketHandled(true);
    }
}