package com.zoma1101.SwordSkill.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zoma1101.SwordSkill.data.DataManager;
import com.zoma1101.SwordSkill.swordskills.SkillData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

import static com.zoma1101.SwordSkill.server.handler.ServerEventHandler.sendSkillSlotInfo;

public class SkillLoadSlotPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final SkillData.WeaponType weaponType; // 追加

    public SkillLoadSlotPacket(SkillData.WeaponType weaponType) { // 修正
        this.weaponType = weaponType;
    }

    public SkillLoadSlotPacket(FriendlyByteBuf buf) {
        this.weaponType = buf.readEnum(SkillData.WeaponType.class); // 修正
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(weaponType); // 修正
    }

    public static void handle(SkillLoadSlotPacket msg, Supplier<NetworkEvent.Context> ctx) {
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
            sendSkillSlotInfo(player);
        });
        ctx.get().setPacketHandled(true);
    }
}