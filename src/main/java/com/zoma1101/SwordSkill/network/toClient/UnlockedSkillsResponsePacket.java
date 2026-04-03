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
    private final boolean martialArtsUnlocked;

    public UnlockedSkillsResponsePacket(int[] unlockedSkills, boolean martialArtsUnlocked) {
        this.unlockedSkills = unlockedSkills;
        this.martialArtsUnlocked = martialArtsUnlocked;
    }

    public UnlockedSkillsResponsePacket(FriendlyByteBuf buf) {
        this.unlockedSkills = buf.readVarIntArray();
        this.martialArtsUnlocked = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarIntArray(this.unlockedSkills);
        buf.writeBoolean(this.martialArtsUnlocked);
    }

    public static void handle(UnlockedSkillsResponsePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                net.minecraft.client.gui.screens.Screen currentScreen = Minecraft.getInstance().screen;
                if (currentScreen instanceof SwordSkillSelectionScreen selectionScreen) {
                    selectionScreen.unlockedSkills.clear();
                    for (int id : msg.unlockedSkills) {
                        selectionScreen.unlockedSkills.add(id);
                    }
                    selectionScreen.martialArtsUnlocked = msg.martialArtsUnlocked;
                } else {
                    LOGGER.debug("Received UnlockedSkillsResponsePacket but screen is not SwordSkillSelectionScreen (Current: {})", currentScreen);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}