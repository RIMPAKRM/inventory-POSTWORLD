package org.inventory.inventory.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;

public final class LoadoutRenderTransforms {

    public static final double DEFAULT_VERTICAL_OFFSET = -3D / 16.0D;
    public static final double DEFAULT_FORWARD_OFFSET = 1.5D / 16.0D;
    public static final float DEFAULT_CROUCH_ROTATION_X = -0.5F;

    public static final double DEFAULT_BACKPACK_VERTICAL_OFFSET = -3.0D / 16.0D;
    public static final double DEFAULT_BACKPACK_FORWARD_OFFSET = 11.5D / 16.0D;


    private LoadoutRenderTransforms() {
    }

    public static void applyCrouchOffset(PoseStack poseStack,
                                         AbstractClientPlayer player,
                                         double verticalOffset,
                                         double forwardOffset) {
        if (player.isCrouching()) {
            poseStack.translate(0.0D, verticalOffset, forwardOffset);
        }
    }

    public static void applyCrouchRotation(PoseStack poseStack,
                                           AbstractClientPlayer player,
                                           float rotationX) {
        if (player.isCrouching()) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotation(rotationX));
        }
    }

    public static void applyAttachmentTransform(PoseStack poseStack,
                                                AbstractClientPlayer player) {
        applyCrouchOffset(poseStack, player,
                DEFAULT_VERTICAL_OFFSET,
                DEFAULT_FORWARD_OFFSET);
        applyCrouchRotation(poseStack, player,
                DEFAULT_CROUCH_ROTATION_X);
    }
}