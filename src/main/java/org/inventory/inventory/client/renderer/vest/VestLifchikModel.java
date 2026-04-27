package org.inventory.inventory.client.renderer.vest;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.inventory.inventory.Inventory;
import org.inventory.inventory.client.renderer.LoadoutAttachmentModel;

public class VestLifchikModel<T extends Entity> extends LoadoutAttachmentModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(Inventory.MODID, "vest_lifchik"), "main");

        public final ModelPart Waist;
    public final ModelPart Body;
    public final ModelPart right_hand;
    public final ModelPart lefthand;

    public VestLifchikModel(ModelPart root) {
                this.Waist = root.getChild("Waist");
                this.Body = this.Waist.getChild("Body");
                this.right_hand = this.Waist.getChild("right_hand");
                this.lefthand = this.Waist.getChild("lefthand");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition Waist = partdefinition.addOrReplaceChild("Waist", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition Body = Waist.addOrReplaceChild("Body",
                CubeListBuilder.create()
                        .texOffs(0, 14).addBox(-1.0F, 5.0F, -3.8F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 14).addBox(-3.2F, 5.0F, -3.8F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 14).addBox(1.2F, 5.0F, -3.8F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 10.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offset(0.0F, -12.0F, 0.0F));
        Body.addOrReplaceChild("cube_r1",
                CubeListBuilder.create()
                        .texOffs(8, 14).addBox(-0.8F, -5.1F, -1.1F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(8, 14).addBox(-3.0F, -5.1F, -1.1F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.0F, 10.0F, -3.0F, -0.0436F, 0.0F, 0.0F));
        Body.addOrReplaceChild("cube_r2",
                CubeListBuilder.create().texOffs(8, 14).addBox(-1.2F, -5.1F, -1.1F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.0F, 10.0F, -3.0F, -0.0436F, 0.0F, 0.0F));
        Body.addOrReplaceChild("cube_r3",
                CubeListBuilder.create().texOffs(16, 18).addBox(-1.0F, -4.0F, -0.7F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, 9.0F, -2.0F, 0.0436F, -0.5672F, 0.0F));
        Body.addOrReplaceChild("cube_r4",
                CubeListBuilder.create().texOffs(16, 18).addBox(-1.4F, -4.0F, -0.6F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.0F, 9.0F, -3.0F, 0.0436F, 0.5672F, 0.0F));
        Body.addOrReplaceChild("cube_r5",
                CubeListBuilder.create().texOffs(16, 14).addBox(-1.2F, -2.0F, 0.2F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, 7.0F, -2.0F, 0.0F, 0.0F, -0.0873F));
        Body.addOrReplaceChild("cube_r6",
                CubeListBuilder.create().texOffs(16, 14).addBox(-1.0F, -2.0F, 0.2F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, 9.0F, -2.0F, 0.0F, 0.0F, 0.0873F));
        Body.addOrReplaceChild("cube_r7",
                CubeListBuilder.create().texOffs(16, 14).addBox(-1.0F, -2.0F, 0.2F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, 7.0F, -2.0F, 0.0F, 0.0F, 0.0873F));
        Body.addOrReplaceChild("cube_r8",
                CubeListBuilder.create().texOffs(16, 14).addBox(-1.2F, -2.0F, 0.2F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, 9.0F, -2.0F, 0.0F, 0.0F, -0.0873F));
                Waist.addOrReplaceChild("right_hand", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
                Waist.addOrReplaceChild("lefthand", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.lefthand.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
        this.right_hand.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack,
                               VertexConsumer vertexConsumer,
                               int packedLight,
                               int packedOverlay,
                               float red,
                               float green,
                               float blue,
                               float alpha) {
                this.Waist.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public void renderOnPlayer(PoseStack poseStack,
                               MultiBufferSource buffer,
                               int packedLight,
                               AbstractClientPlayer player,
                               ModelPart bodyPart,
                               ModelPart rightArmPart,
                               ModelPart leftArmPart,
                               ResourceLocation texture) {
        renderAttachment(poseStack, buffer, packedLight, player, texture, () -> {
                                copyFrom(this.Waist, bodyPart);
            copyFrom(this.Body, bodyPart);
            copyFrom(this.right_hand, rightArmPart);
            copyFrom(this.lefthand, leftArmPart);
        });
                }
}