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

public class LeopardPressVestModel<T extends Entity> extends LoadoutAttachmentModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(Inventory.MODID, "leopard_press_vest"), "main");

    public final ModelPart Waist;
    public final ModelPart Body;
    public final ModelPart bone;

    public LeopardPressVestModel(ModelPart root) {
        this.Waist = root.getChild("Waist");
        this.Body = this.Waist.getChild("Body");
        this.bone = this.Waist.getChild("bone");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition Waist = partdefinition.addOrReplaceChild("Waist", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
        Waist.addOrReplaceChild("Body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5F, 0.0F, 1.8F, 7.0F, 9.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 5).addBox(-4.0F, 5.0F, 1.5F, 8.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 10).addBox(-3.5F, 1.0F, -2.8F, 7.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 19).addBox(1.5F, 0.0F, -2.8F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(6, 19).addBox(-3.5F, 0.0F, -2.8F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 0).addBox(-4.0F, 5.0F, -2.5F, 8.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 10).addBox(3.3F, 5.0F, -2.0F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 10).addBox(-4.3F, 5.0F, -2.0F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 18).addBox(-3.5F, -0.25F, -2.01F, 2.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 18).addBox(1.5F, -0.25F, -2.01F, 2.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        Waist.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
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
                               ModelPart rightArmPart,
                               ModelPart leftArmPart,
                               ResourceLocation texture,
                               AbstractClientPlayer player) {
        renderAttachment(poseStack, buffer, packedLight, player, texture, () -> {
            copyFrom(this.Waist, bodyPart);
            copyFrom(this.Body, bodyPart);
        });
    }
}