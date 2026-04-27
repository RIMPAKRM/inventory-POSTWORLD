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

public class Vest6sh117DesertModel<T extends Entity> extends LoadoutAttachmentModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(Inventory.MODID, "vest_6sh117_desert"), "main");

    public final ModelPart Waist;
    public final ModelPart Body;
    public final ModelPart right_hand;
    public final ModelPart lefthand;

    public Vest6sh117DesertModel(ModelPart root) {
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
                        .texOffs(10, 5).addBox(2.0F, -0.56F, -2.0F, 2.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(10, 5).addBox(-4.0F, -0.56F, -2.0F, 2.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 5).addBox(-4.3F, 7.0F, -2.0F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 5).addBox(3.3F, 7.0F, -2.0F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(10, 10).addBox(1.0F, 4.0F, -2.8F, 3.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(18, 24).addBox(-1.0F, 4.0F, -2.5F, 2.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(26, 12).addBox(2.0F, 0.0F, -2.8F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(10, 18).addBox(-4.0F, 4.0F, -2.8F, 3.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(26, 17).addBox(-4.0F, 0.0F, -2.8F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 13).addBox(-2.0F, 2.0F, 1.8F, 4.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 19).addBox(-2.3F, 5.0F, -4.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(18, 17).addBox(0.3F, 5.0F, -4.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(30, 5).addBox(-1.0F, 1.0F, 1.8F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-4.0F, 7.0F, 1.8F, 8.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -12.0F, 0.0F));
        Body.addOrReplaceChild("Body_r1", CubeListBuilder.create().texOffs(30, 0).addBox(0.0F, -4.4F, -0.5F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.0F, 2.0F, 0.0F, 0.0F, 0.5672F));
        Body.addOrReplaceChild("Body_r2", CubeListBuilder.create().texOffs(24, 29).addBox(-1.2F, -4.0F, -0.5F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 3.0F, 2.0F, 0.0F, 0.0F, -0.5672F));
        Body.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(18, 10).addBox(-0.7F, -5.0F, -0.9F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, 10.0F, -3.0F, 0.0F, -0.2182F, 0.0F));
        Body.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(22, 7).addBox(-0.2F, -2.8F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(24, 24).addBox(-0.7F, -5.1F, -1.2F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, 10.0F, -3.0F, -0.0436F, -0.2182F, 0.0F));
        Body.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 26).addBox(-0.7F, -5.1F, -1.3F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(10, 31).addBox(-0.2F, -2.8F, -1.1F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(14, 31).addBox(-2.8F, -2.8F, -1.1F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(26, 7).addBox(-3.3F, -5.1F, -1.3F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 10.0F, -3.0F, -0.0436F, 0.0F, 0.0F));
        Body.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(8, 26).addBox(-1.3F, -5.1F, -1.2F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(6, 31).addBox(-0.8F, -2.8F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, 10.0F, -3.0F, -0.0436F, 0.2182F, 0.0F));
        Body.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(22, 0).addBox(-1.3F, -5.0F, -0.9F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, 10.0F, -3.0F, 0.0F, 0.2182F, 0.0F));
        Body.addOrReplaceChild("Body_r3", CubeListBuilder.create().texOffs(30, 29).addBox(-1.0F, -4.0F, -0.79F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 4.0F, -2.0F, 0.0F, 0.0F, -0.2618F));
        Body.addOrReplaceChild("Body_r4", CubeListBuilder.create().texOffs(18, 0).addBox(0.0F, -4.0F, -0.79F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 4.0F, -2.0F, 0.0F, 0.0F, 0.2618F));
        Body.addOrReplaceChild("Body_r5", CubeListBuilder.create().texOffs(0, 31).addBox(-0.5F, -1.0F, 0.2F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, 3.0F, -4.0F, 0.0F, 0.0F, 0.0873F));
        Body.addOrReplaceChild("Body_r6", CubeListBuilder.create().texOffs(0, 31).addBox(-1.4F, -1.0F, 0.2F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, 3.0F, -4.0F, 0.0F, 0.0F, -0.0436F));
        Body.addOrReplaceChild("Body_r7", CubeListBuilder.create().texOffs(26, 22).addBox(2.0F, -1.6F, -2.3F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(26, 22).addBox(8.0F, -1.6F, -2.3F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, 0.6109F, 0.0F, 0.0F));
        Waist.addOrReplaceChild("right_hand", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
        Waist.addOrReplaceChild("lefthand", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 64, 64);
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