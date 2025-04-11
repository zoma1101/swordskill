package com.zoma1101.swordskill.network;

import com.zoma1101.swordskill.client.gui.SwordSkillSelectionScreen;
import com.zoma1101.swordskill.data.SkillDataFetcher;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CheckSkillUnlockedPacket {
    public CheckSkillUnlockedPacket() {}

    public CheckSkillUnlockedPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(CheckSkillUnlockedPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            SwordSkillSelectionScreen selectScreen = (SwordSkillSelectionScreen) Minecraft.getInstance().screen;
            if (selectScreen != null) {
                int[] unlockedSkills = SkillDataFetcher.getUnlockedSkills(player);
                selectScreen.unlockedSkills.clear(); // 既存のデータをクリア
                selectScreen.unlockedSkills.addAll(Arrays.stream(unlockedSkills).boxed().collect(Collectors.toSet()));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}