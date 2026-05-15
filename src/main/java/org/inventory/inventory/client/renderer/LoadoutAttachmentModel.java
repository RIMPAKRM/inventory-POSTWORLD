package org.inventory.inventory.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public abstract class LoadoutAttachmentModel<T extends Entity> extends EntityModel<T> {

    protected final void renderAttachment(PoseStack poseStack,
                                         MultiBufferSource buffer,
                                         int packedLight,
                                         AbstractClientPlayer player,
                                         ResourceLocation texture,
                                         Runnable copyState) {
        copyState.run();

        poseStack.pushPose();
        LoadoutRenderTransforms.applyAttachmentTransform(poseStack, player);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        this.renderToBuffer(
                poseStack,
                vc,
                packedLight,
                LivingEntityRenderer.getOverlayCoords(player, 0.0F),
                1.0F, 1.0F, 1.0F, 1.0F
        );
        poseStack.popPose();
    }

    protected final void copyFrom(ModelPart target, ModelPart source) {
        target.copyFrom(source);
    }
}