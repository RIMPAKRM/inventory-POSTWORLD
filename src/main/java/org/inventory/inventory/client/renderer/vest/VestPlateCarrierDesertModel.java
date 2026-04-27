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
import net.minecraft.world.entity.Entity;
import org.inventory.inventory.Inventory;
import org.inventory.inventory.client.renderer.LoadoutAttachmentModel;

public class VestPlateCarrierDesertModel<T extends Entity> extends LoadoutAttachmentModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(Inventory.MODID, "vest_plate_carrier_desert"), "main");

    public final ModelPart Waist;
    public final ModelPart Body;
    public final ModelPart bone;

    public VestPlateCarrierDesertModel(ModelPart root) {
        this.Waist = root.getChild("Waist");
        this.Body = this.Waist.getChild("Body");
        this.bone = this.Waist.getChild("bone");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition Waist = partdefinition.addOrReplaceChild("Waist", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition Body = Waist.addOrReplaceChild("Body",
                CubeListBuilder.create()
                        .texOffs(0, 12).addBox(-3.0F, 2.0F, 1.8F, 6.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 21).addBox(-2.0F, 1.0F, -2.8F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 6).addBox(-4.0F, 5.0F, 1.5F, 8.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 24).addBox(-2.0F, 1.0F, 1.8F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(18, 9).addBox(-3.0F, 2.7F, -2.78F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(14, 12).addBox(-3.0F, 3.0F, -2.8F, 6.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-4.0F, 5.0F, -2.5F, 8.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(18, 0).addBox(3.3F, 5.0F, -2.0F, 1.0F, 5.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(18, 0).addBox(-4.3F, 5.0F, -2.0F, 1.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -12.0F, 0.0F));
        Body.addOrReplaceChild("Body_r1", CubeListBuilder.create().texOffs(14, 20).addBox(0.0F, -0.25F, -2.01F, 2.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(22, 25).addBox(0.0F, -0.24F, 1.01F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0436F));
        Body.addOrReplaceChild("Body_r2", CubeListBuilder.create().texOffs(22, 25).addBox(-2.0F, -0.24F, 1.01F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(14, 20).addBox(-2.0F, -0.25F, -2.01F, 2.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0436F));
        Body.addOrReplaceChild("Body_r3", CubeListBuilder.create().texOffs(10, 21).addBox(0.0F, 0.0F, -3.21F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(10, 21).addBox(0.4F, 0.0F, -3.22F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, 1.0F, 5.0F, 0.0F, 0.0F, 0.7854F));
        Body.addOrReplaceChild("Body_r4", CubeListBuilder.create().texOffs(10, 23).addBox(0.0F, 0.0F, -3.21F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(10, 23).addBox(0.0F, 0.4F, -3.22F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 1.0F, 5.0F, 0.0F, 0.0F, 0.7854F));
        Body.addOrReplaceChild("Body_r5", CubeListBuilder.create().texOffs(10, 25).addBox(0.0F, 0.0F, -0.25F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(16, 25).addBox(0.0F, 0.0F, 3.25F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.4363F));
        Body.addOrReplaceChild("Body_r6", CubeListBuilder.create().texOffs(10, 25).addBox(-2.0F, 0.0F, -0.25F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(16, 25).addBox(-2.0F, 0.0F, 3.25F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 0.0F, -2.0F, 0.0F, 0.0F, -0.4363F));
        Body.addOrReplaceChild("Body_r7", CubeListBuilder.create().texOffs(0, 26).addBox(0.0F, 0.0F, -2.79F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0472F));
        Body.addOrReplaceChild("Body_r8", CubeListBuilder.create().texOffs(26, 20).addBox(-2.0F, 0.0F, -2.79F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 1.0F, 0.0F, 0.0F, 0.0F, -1.0472F));
        Waist.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
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
                               ResourceLocation texture) {
        renderAttachment(poseStack, buffer, packedLight, player, texture, () -> {
                                copyFrom(this.Waist, bodyPart);
            copyFrom(this.Body, bodyPart);
            copyFrom(this.bone, bodyPart);
        });
    }
}