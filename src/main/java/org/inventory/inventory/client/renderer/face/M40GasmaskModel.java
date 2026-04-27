package org.inventory.inventory.client.renderer.face;

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

public class M40GasmaskModel<T extends Entity> extends LoadoutAttachmentModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(Inventory.MODID, "m40_gasmask"), "main");

        public final ModelPart Waist;
        public final ModelPart Head;

    public M40GasmaskModel(ModelPart root) {
                this.Waist = root.getChild("Waist");
                this.Head = this.Waist.getChild("Head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
                PartDefinition Waist = partdefinition.addOrReplaceChild("Waist", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
                PartDefinition Head = Waist.addOrReplaceChild("Head",
                CubeListBuilder.create()
                        .texOffs(0, 5).addBox(-4.0F, -8.0F, -5.0F, 8.0F, 9.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(18, 5).addBox(3.2F, -8.0F, -4.99F, 1.0F, 9.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(28, 0).addBox(1.0F, -8.01F, -0.99F, 1.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(8, 30).addBox(-2.0F, -6.01F, 3.01F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(8, 33).addBox(-2.0F, -2.01F, 3.01F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(14, 23).addBox(-2.0F, -4.01F, 3.01F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(24, 0).addBox(1.0F, -4.01F, 3.01F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-4.0F, -8.01F, -4.99F, 8.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(28, 0).addBox(-2.0F, -8.01F, -0.99F, 1.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(18, 18).addBox(-4.2F, -8.0F, -4.99F, 1.0F, 9.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 15).addBox(-4.01F, -6.01F, -2.99F, 2.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 15).addBox(-4.01F, -2.01F, -2.99F, 2.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 15).addBox(2.01F, -6.01F, -2.99F, 2.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 15).addBox(2.01F, -2.01F, -2.99F, 2.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
                        .texOffs(28, 19).addBox(0.19F, -6.0F, -5.5F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(28, 29).addBox(-4.19F, -6.0F, -5.5F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(26, 34).addBox(-1.0F, -3.8F, -7.8F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(28, 7).addBox(-1.3F, -2.0F, -7.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 30).addBox(-1.7F, -2.0F, -6.99F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -12.0F, 0.0F));
        Head.addOrReplaceChild("cube_r1",
                CubeListBuilder.create().texOffs(28, 13).addBox(-3.1F, -1.6F, -2.8F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(18, 31).addBox(-4.1F, -1.1F, -2.3F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(5.0F, 0.0F, -7.0F, 0.0F, 0.0436F, 0.2618F));
        Head.addOrReplaceChild("cube_r2",
                CubeListBuilder.create().texOffs(18, 31).addBox(-2.0F, -1.2F, -2.2F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.0F, -1.0F, -7.0F, 0.0F, -0.0436F, -0.2618F));
        Head.addOrReplaceChild("cube_r3",
                CubeListBuilder.create().texOffs(28, 24).addBox(-2.0F, -1.4F, -2.2F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.0F, 1.0F, -7.0F, 0.2618F, 0.0F, 0.0F));
        Head.addOrReplaceChild("cube_r4",
                CubeListBuilder.create().texOffs(0, 23).addBox(-2.5F, -3.0F, -2.4F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.0F, 0.0F, -7.0F, 0.0436F, 0.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.Head.yRot = netHeadYaw / (180F / (float) Math.PI);
        this.Head.xRot = headPitch / (180F / (float) Math.PI);
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
                                                           ModelPart headPart,
                               ResourceLocation texture) {
                renderAttachment(poseStack, buffer, packedLight, player, texture, () -> {
                        copyFrom(this.Waist, bodyPart);
                        copyFrom(this.Head, headPart);
                });
    }
}