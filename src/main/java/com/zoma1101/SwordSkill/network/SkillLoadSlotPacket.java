package com.zoma1101.swordskill.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zoma1101.swordskill.data.DataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

import static com.zoma1101.swordskill.server.handler.ServerEventHandler.sendSkillSlotInfo;

public class SkillLoadSlotPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final String weaponName; // 追加

    public SkillLoadSlotPacket(String weaponName) { // 修正
        this.weaponName = weaponName;
    }

    public SkillLoadSlotPacket(FriendlyByteBuf buf) {
        this.weaponName = buf.readUtf(); // 修正
    }

    public void encode(FriendlyByteBuf buf) {
        if (weaponName != null){
            buf.writeUtf(weaponName);
        }
        else {
            buf.writeUtf("type123");
        }
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
            JsonArray skillSlot = weaponSkills.getAsJsonArray(msg.weaponName); // 修正
            if (skillSlot == null) {
                skillSlot = new JsonArray();
                for (int i = 0; i < 5; i++) {
                    skillSlot.add(0); // 初期化
                }
                weaponSkills.add(msg.weaponName, skillSlot); // 修正
            }
            sendSkillSlotInfo(player);
        });
        ctx.get().setPacketHandled(true);
    }
}