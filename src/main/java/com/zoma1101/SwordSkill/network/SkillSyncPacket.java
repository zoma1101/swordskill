package com.zoma1101.swordskill.network;

import com.zoma1101.swordskill.client.handler.ClientForgeHandler; // クライアント側でのみ使う
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist; // 追加
import net.minecraftforge.fml.DistExecutor; // 追加
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.util.function.Supplier;

public class SkillSyncPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final int selectedSkillIndex;

    // サーバー -> クライアント 送信用コンストラクタ
    public SkillSyncPacket(int selectedSkillIndex) {
        this.selectedSkillIndex = selectedSkillIndex;
    }

    // デコード用コンストラクタ
    public SkillSyncPacket(FriendlyByteBuf buf) {
        this.selectedSkillIndex = buf.readInt();
    }

    // エンコードメソッド
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.selectedSkillIndex);
    }

    // ハンドルメソッド (クライアントサイドで実行)
    public static void handle(SkillSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // クライアントサイドでのみ実行する処理を DistExecutor で囲む
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                LOGGER.debug("SkillSyncPacket を受信しました。 Index: {}", msg.selectedSkillIndex); // デバッグログ追加推奨
                ClientForgeHandler.setSelectedSkillIndex(msg.selectedSkillIndex);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}