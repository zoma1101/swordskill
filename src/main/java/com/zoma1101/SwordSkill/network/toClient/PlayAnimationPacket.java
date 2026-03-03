package com.zoma1101.swordskill.network.toClient;

import com.zoma1101.swordskill.AnimationUtils;
import com.zoma1101.swordskill.IsAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayAnimationPacket {
    private final int entityId; // ★追加: 誰がアニメーションするか
    private final int skillId;
    private final String animationType;

    // サーバー側で送信時に使うコンストラクタ
    public PlayAnimationPacket(int entityId, int skillId, String animationType) {
        this.entityId = entityId;
        this.skillId = skillId;
        this.animationType = animationType;
    }

    // クライアント側で受信時に使うコンストラクタ
    public PlayAnimationPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt(); // ★追加
        this.skillId = buf.readVarInt();
        this.animationType = buf.readUtf();
    }

    // サーバー側で送信時に使うエンコードメソッド
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId); // ★追加
        buf.writeVarInt(this.skillId);
        buf.writeUtf(this.animationType);
    }

    // クライアント側で受信時に実行される handle メソッド
    public static void handle(PlayAnimationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // クライアントサイドでのみ実行
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // playeranimatorが導入されているか確認
                if (IsAnimation.isPlayerAnimator()) {
                    var level = Minecraft.getInstance().level;
                    if (level != null) {
                        // IDからエンティティを特定
                        Entity entity = level.getEntity(msg.entityId);
                        // プレイヤーであればアニメーション再生
                        if (entity instanceof Player player) {
                            AnimationUtils.PlayerAnim(player, msg.skillId, msg.animationType);
                            // 視点方向に腕を振る（バニラの腕振り）
                            player.swing(InteractionHand.MAIN_HAND);
                        }
                    }
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
