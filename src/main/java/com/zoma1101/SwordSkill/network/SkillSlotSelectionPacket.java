package com.zoma1101.SwordSkill.network;

import com.zoma1101.SwordSkill.data.DataManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.function.Supplier;

import static com.zoma1101.SwordSkill.server.handler.ServerEventHandler.sendSkillSlotInfo;

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
            if (player == null) {
                LOGGER.warn("プレイヤーがnullです。スキル選択を中止します。");
                return;
            }
            JsonObject playerData = DataManager.loadPlayerData(player);
            JsonObject weaponSkills = playerData.getAsJsonObject("weaponSkills"); // 修正
            if (weaponSkills == null) {
                weaponSkills = new JsonObject();
                playerData.add("weaponSkills", weaponSkills);
            }
            JsonArray skillSlot = weaponSkills.getAsJsonArray(msg.weaponName); // 修正
            if (skillSlot == null) {
                skillSlot = new JsonArray();
                for (int i = 0; i < 5; i++) {
                    skillSlot.add(0); // 初期化
                }
                weaponSkills.add(msg.weaponName, skillSlot); // 修正
            }
            skillSlot.set(msg.slotIndex, new JsonPrimitive(msg.skillId));
            DataManager.savePlayerData(player, playerData);
            LOGGER.info("SkillSlotSelectionPacket: スキルID {} をスロット {} に保存", msg.skillId, msg.slotIndex);
            sendSkillSlotInfo(player);
        });
        ctx.get().setPacketHandled(true);
    }
}