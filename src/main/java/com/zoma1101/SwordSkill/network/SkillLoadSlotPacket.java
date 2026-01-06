package com.zoma1101.swordskill.network;

import com.zoma1101.swordskill.capability.PlayerSkillsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.function.Supplier;

import static com.zoma1101.swordskill.server.handler.ServerEventHandler.sendSkillSlotInfo;

public class SkillLoadSlotPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final String weaponName;

    public SkillLoadSlotPacket(String weaponName) {
        this.weaponName = weaponName;
    }

    public SkillLoadSlotPacket(FriendlyByteBuf buf) {
        this.weaponName = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(Objects.requireNonNullElse(weaponName, "type123"));
    }

    public static void handle(SkillLoadSlotPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                LOGGER.warn("プレイヤーがnullです。スキル選択を中止します。");
                return;
            }

            // ★修正: DataManager (JSON) を廃止し、Capability (NBT) を使用するように変更
            player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(skills -> {
                // Capabilityからデータを取得してクライアントに同期する
                // (ServerEventHandler.sendSkillSlotInfo も Capability を使うように修正されている必要があります)
                sendSkillSlotInfo(player);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}