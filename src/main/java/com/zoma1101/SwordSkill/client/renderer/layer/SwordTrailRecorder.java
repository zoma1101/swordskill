package com.zoma1101.swordskill.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;

public class SwordTrailRecorder {
    public static void record(Player player, PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.getEntityRenderDispatcher().camera;
        SwordTrailLayer.TrailSession session = SwordTrailManager.getSession(player.getUUID());

        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;
        Quaternionf camRot = camera.rotation();

        SwordTrailLayer.capturePoint(poseStack, session, camX, camY, camZ, camRot);
    }
}
