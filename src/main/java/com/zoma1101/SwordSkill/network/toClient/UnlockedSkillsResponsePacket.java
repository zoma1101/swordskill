package com.zoma1101.swordskill.network.toClient;

import com.zoma1101.swordskill.client.gui.SwordSkillSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;        // Log4jを追加
import org.apache.logging.log4j.Logger;         // Log4jを追加


import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class UnlockedSkillsResponsePacket {
    private static final Logger LOGGER = LogManager.getLogger(); // クライアント側ログ用
    private final int[] unlockedSkills; // サーバーから送られてくるスキルID配列

    // サーバー側で送信時に使うコンストラクタ
    public UnlockedSkillsResponsePacket(int[] unlockedSkills) {
        this.unlockedSkills = unlockedSkills;
    }

    // クライアント側で受信時に使うコンストラクタ (ByteBufから読み込む)
    public UnlockedSkillsResponsePacket(FriendlyByteBuf buf) {
        this.unlockedSkills = buf.readVarIntArray();
    }

    // サーバー側で送信時に使うエンコードメソッド (ByteBufに書き込む)
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarIntArray(this.unlockedSkills);
    }

    // この handle はクライアントサイドで実行される
    public static void handle(UnlockedSkillsResponsePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> { // クライアントのメインスレッドで実行依頼
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                net.minecraft.client.gui.screens.Screen currentScreen = Minecraft.getInstance().screen;
                if (currentScreen instanceof SwordSkillSelectionScreen selectionScreen) {
                    LOGGER.debug("Updating SwordSkillSelectionScreen with skills: {}", msg.unlockedSkills); // デバッグログ
                    // 画面の unlockedSkills セットを更新
                    Set<Integer> receivedSkills = Arrays.stream(msg.unlockedSkills)
                            .boxed()
                            .collect(Collectors.toSet());
                    selectionScreen.unlockedSkills.clear();
                    selectionScreen.unlockedSkills.addAll(receivedSkills);
                } else {
                    LOGGER.debug("Received UnlockedSkillsResponsePacket but screen is not SwordSkillSelectionScreen (Current: {})", currentScreen);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}