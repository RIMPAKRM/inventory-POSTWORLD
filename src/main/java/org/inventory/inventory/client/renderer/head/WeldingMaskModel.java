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

public class WeldingMaskModel<T extends Entity> extends LoadoutAttachmentModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(Inventory.MODID, "welding_mask"), "main");

    public final ModelPart Waist;
    public final ModelPart Head;

    public WeldingMaskModel(ModelPart root) {
        this.Waist = root.getChild("Waist");
        this.Head = this.Waist.getChild("Head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition Waist = partdefinition.addOrReplaceChild("Waist", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition Head = Waist.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(0, 9).addBox(-4.6F, -6.0F, -3.6F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(0, 9).addBox(3.6F, -6.0F, -3.6F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(18, 0).addBox(-3.4F, -6.0F, 3.4F, 7.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(18, 9).addBox(-3.6F, -6.0F, 3.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.0F, 0.0F));

        Head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(18, 4).addBox(-3.0F, -6.0F, -1.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(18, 4).addBox(-8.0F, -6.0F, -1.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(18, 2).addBox(-7.0F, -6.0F, -1.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, -1.0F, -4.9F, -0.0349F, 0.0F, 0.0F));

        Head.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(18, 2).addBox(-2.0F, -6.0F, -1.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.0F, -5.0F, -0.0349F, 0.0F, 0.0F));

        Head.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 18).addBox(-4.0F, -2.0F, -1.0F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -4.0F, -5.4F, -1.9984F, 0.0F, -1.5708F));

        Head.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -7.0F, -1.0F, 8.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.0F, -4.7F, -0.0349F, 0.0F, 0.0F));

        Head.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(0, 18).addBox(-4.0F, -2.0F, -1.0F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1F, -4.0F, -5.0F, -1.1432F, 0.0F, -1.5708F));

        Head.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(0, 18).addBox(-4.0F, -2.0F, -1.0F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -7.1F, -5.1F, -1.2217F, 0.0F, 0.0F));

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
                               AbstractClientPlayer player,
                               ModelPart bodyPart,
                               ModelPart headPart,
                               ResourceLocation texture) {
        renderAttachment(poseStack, buffer, packedLight, player, texture, () -> {
            copyFrom(this.Waist, bodyPart);
            copyFrom(this.Head, headPart);
        });
    }
}