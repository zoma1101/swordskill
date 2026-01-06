package com.zoma1101.swordskill.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.zoma1101.swordskill.capability.PlayerSkillsProvider;
import com.zoma1101.swordskill.data.DataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

import static com.zoma1101.swordskill.server.handler.ServerEventHandler.sendSkillSlotInfo;

public class SkillSlotSelectionPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final int skillId;
    private final int slotIndex;
    private final String weaponName; // 追加

    public SkillSlotSelectionPacket(int skillId, int slotIndex, String weaponName) { // 修正
        this.skillId = skillId;
        this.slotIndex = slotIndex;
        this.weaponName = weaponName;
    }

    public SkillSlotSelectionPacket(FriendlyByteBuf buf) {
        this.skillId = buf.readInt();
        this.slotIndex = buf.readInt();
        this.weaponName = buf.readUtf(); // 修正
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(skillId);
        buf.writeInt(slotIndex);
        buf.writeUtf(weaponName); // 修正
    }

    public static void handle(SkillSlotSelectionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // ★修正: DataManagerを使わずCapabilityを使用
            player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(skills -> {
                skills.setSkillSlot(msg.weaponName, msg.slotIndex, msg.skillId);
                LOGGER.info("SkillSlotSelectionPacket: ID {} -> Slot {} ({})", msg.skillId, msg.slotIndex, msg.weaponName);

                // 必要であればここでスロット情報の同期パケットを送る
                sendSkillSlotInfo(player);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}