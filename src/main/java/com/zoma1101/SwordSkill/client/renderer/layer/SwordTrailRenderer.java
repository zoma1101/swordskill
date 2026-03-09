package com.zoma1101.swordskill.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.client.event.RenderLevelStageEvent;

public class SwordTrailRenderer {
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;

        Minecraft mc = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = mc.renderBuffers().bufferSource();

        for (SwordTrailLayer.TrailSession session : SwordTrailManager.getAll()) {
            SwordTrailLayer.renderTrail(poseStack, buffer, session);
        }

        if (buffer instanceof net.minecraft.client.renderer.MultiBufferSource.BufferSource bs) {
            bs.endBatch();
        }
    }
}