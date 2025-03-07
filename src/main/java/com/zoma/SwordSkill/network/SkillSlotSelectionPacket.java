package com.zoma.SwordSkill.network;

import com.zoma.SwordSkill.data.DataManager;
import com.zoma.SwordSkill.swordskills.SkillData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.function.Supplier;

import static com.zoma.SwordSkill.server.handler.ServerEventHandler.sendSkillSlotInfo;

public class SkillSlotSelectionPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final int skillId;
    private final int slotIndex;
    private final SkillData.WeaponType weaponType; // 追加

    public SkillSlotSelectionPacket(int skillId, int slotIndex, SkillData.WeaponType weaponType) { // 修正
        this.skillId = skillId;
        this.slotIndex = slotIndex;
        this.weaponType = weaponType;
    }

    public SkillSlotSelectionPacket(FriendlyByteBuf buf) {
        this.skillId = buf.readInt();
        this.slotIndex = buf.readInt();
        this.weaponType = buf.readEnum(SkillData.WeaponType.class); // 修正
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(skillId);
        buf.writeInt(slotIndex);
        buf.writeEnum(weaponType); // 修正
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
            JsonArray skillSlot = weaponSkills.getAsJsonArray(msg.weaponType.name()); // 修正
            if (skillSlot == null) {
                skillSlot = new JsonArray();
                for (int i = 0; i < 5; i++) {
                    skillSlot.add(0); // 初期化
                }
                weaponSkills.add(msg.weaponType.name(), skillSlot); // 修正
            }
            skillSlot.set(msg.slotIndex, new JsonPrimitive(msg.skillId));
            DataManager.savePlayerData(player, playerData);
            LOGGER.info("SkillSlotSelectionPacket: スキルID {} をスロット {} に保存", msg.skillId, msg.slotIndex);
            sendSkillSlotInfo(player);
        });
        ctx.get().setPacketHandled(true);
    }
}