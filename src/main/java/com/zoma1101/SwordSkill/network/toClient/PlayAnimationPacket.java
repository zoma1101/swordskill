package com.zoma1101.swordskill.network.toClient;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.zoma1101.swordskill.IsAnimation.PlayerAnimation;

public class PlayAnimationPacket {
    private final int skillId;
    private final String animationType;

    // サーバー側で送信時に使うコンストラクタ
    public PlayAnimationPacket(int skillId, String animationType) {
        this.skillId = skillId;
        this.animationType = animationType;
    }

    // クライアント側で受信時に使うコンストラクタ
    public PlayAnimationPacket(FriendlyByteBuf buf) {
        this.skillId = buf.readVarInt();
        this.animationType = buf.readUtf();
    }

    // サーバー側で送信時に使うエンコードメソッド
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.skillId);
        buf.writeUtf(this.animationType);
    }

    // クライアント側で受信時に実行される handle メソッド
    public static void handle(PlayAnimationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // クライアントサイドでのみ実行
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // ★ ClientAnimationHandler のメソッドを呼び出す
                PlayerAnimation(msg.skillId, msg.animationType);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}