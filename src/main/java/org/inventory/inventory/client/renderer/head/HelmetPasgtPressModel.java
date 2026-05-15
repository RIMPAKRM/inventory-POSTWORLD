package org.inventory.inventory.client.renderer.head;

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
import net.minecraft.world.entity.Entity;
import org.inventory.inventory.Inventory;
import org.inventory.inventory.client.renderer.LoadoutAttachmentModel;

public class HelmetPasgtPressModel<T extends Entity> extends LoadoutAttachmentModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(Inventory.MODID, "helmet_pasgt_press"), "main");

    public final ModelPart Waist;
    public final ModelPart head;
    public final ModelPart base;
    public final ModelPart strap;

    public HelmetPasgtPressModel(ModelPart root) {
        this.Waist = root.getChild("Waist");
        this.head = this.Waist.getChild("head");
        this.base = this.head.getChild("base");
        this.strap = this.head.getChild("strap");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition Waist = partdefinition.addOrReplaceChild("Waist", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition head = Waist.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -12.0F, 0.0F));

        PartDefinition base = head.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.7F, -4.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        base.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(18, 29).addBox(-2.999F, -2.91F, -0.96F, 8.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -5.0F, -4.0F, -0.0873F, 0.0F, 0.0F));
        base.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(18, 33).addBox(-1.2F, -2.0F, -0.92F, 1.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(0, 9).addBox(-1.0F, -3.9F, -4.0F, 1.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.0873F));
        base.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(36, 15).addBox(-1.14F, -1.25F, 3.01F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(0, 20).addBox(-1.15F, -1.26F, -4.01F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, -7.0F, 0.0F, 0.0F, 0.0F, 0.7418F));
        base.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(0, 35).addBox(-3.5F, -2.95F, -1.0F, 1.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(0, 35).addBox(-11.5F, -2.95F, -1.0F, 1.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0F, -5.0F, 3.0F, 0.0436F, 0.0F, 0.0F));
        base.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(0, 29).addBox(-3.999F, -3.0F, -0.11F, 8.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.0F, 4.0F, 0.0436F, 0.0F, 0.0F));
        base.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(18, 9).addBox(0.0F, -3.9F, -4.0F, 1.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)).texOffs(30, 33).addBox(0.2F, -2.0F, -0.92F, 1.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -4.0F, 0.0F, 0.0F, 0.0F, -0.0873F));
        base.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(18, 20).addBox(0.15F, -1.26F, -4.01F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)).texOffs(36, 17).addBox(0.14F, -1.26F, 3.01F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -7.0F, 0.0F, 0.0F, 0.0F, -0.7418F));
        base.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(32, 6).addBox(3.01F, -1.09F, 0.29F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(32, 2).addBox(-4.01F, -1.09F, 0.3F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -7.0F, 4.0F, 0.8727F, 0.0F, 0.0F));
        base.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(36, 19).addBox(-4.01F, -1.19F, -1.19F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(32, 0).addBox(-3.99F, -1.2F, -1.2F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -7.0F, -4.0F, -0.7854F, 0.0F, 0.0F));

        PartDefinition strap = head.addOrReplaceChild("strap", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        strap.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(6, 35).addBox(-1.2F, -6.0F, 0.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(10, 35).addBox(6.2F, -6.0F, 0.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -3.0F, -1.0036F, 0.0F, 0.0F));
        strap.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(36, 6).addBox(-1.2F, -6.0F, 0.0F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(14, 35).addBox(6.2F, -6.0F, 0.0F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, 0.0F, -4.0F, -0.1745F, 0.0F, 0.0F));
        strap.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(36, 13).addBox(-4.21F, -0.91F, -1.01F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(32, 4).addBox(-3.79F, -0.9F, -1.0F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, 0.5236F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.Waist.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public void renderOnPlayer(PoseStack poseStack,
                               MultiBufferSource buffer,
                               int packedLight,
                               ModelPart bodyPart,
                               ModelPart headPart,
                               ResourceLocation texture,
                               AbstractClientPlayer player) {
        renderAttachment(poseStack, buffer, packedLight, player, texture, () -> {
            copyFrom(this.Waist, bodyPart);
            copyFrom(this.head, headPart);
        });
    }
}