package com.zoma1101.swordskill.network.toClient;

import com.zoma1101.swordskill.client.gui.SwordSkillSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class UnlockedSkillsResponsePacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final int[] unlockedSkills;

    public UnlockedSkillsResponsePacket(int[] unlockedSkills) {
        this.unlockedSkills = unlockedSkills;
    }

    public UnlockedSkillsResponsePacket(FriendlyByteBuf buf) {
        this.unlockedSkills = buf.readVarIntArray();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarIntArray(this.unlockedSkills);
    }

    public static void handle(UnlockedSkillsResponsePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                net.minecraft.client.gui.screens.Screen currentScreen = Minecraft.getInstance().screen;
                if (currentScreen instanceof SwordSkillSelectionScreen selectionScreen) {

                    // ★最適化: Stream APIを使わず、単純なループで処理
                    selectionScreen.unlockedSkills.clear();
                    for (int id : msg.unlockedSkills) {
                        selectionScreen.unlockedSkills.add(id);
                    }

                } else {
                    LOGGER.debug("Received UnlockedSkillsResponsePacket but screen is not SwordSkillSelectionScreen (Current: {})", currentScreen);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}