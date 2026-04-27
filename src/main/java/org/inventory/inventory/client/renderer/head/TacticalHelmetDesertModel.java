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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import org.inventory.inventory.client.renderer.LoadoutAttachmentModel;
import org.inventory.inventory.Inventory;

public class TacticalHelmetDesertModel<T extends Entity> extends LoadoutAttachmentModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(Inventory.MODID, "tactical_helmet_desert"), "main");

    public final ModelPart Waist;
    public final ModelPart Head;

    public TacticalHelmetDesertModel(ModelPart root) {
        this.Waist = root.getChild("Waist");
        this.Head = this.Waist.getChild("Head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition Waist = partdefinition.addOrReplaceChild("Waist", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));

        PartDefinition Head = Waist.addOrReplaceChild("Head",
                CubeListBuilder.create().texOffs(18, 28).addBox(-4.0F, -8.3F, -4.7F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 28).addBox(-4.0F, -8.3F, 3.7F, 8.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 5).addBox(-4.7F, -6.3F, 1.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(18, 9).addBox(-4.7F, -8.3F, -4.0F, 1.0F, 2.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-4.0F, -9.0F, -4.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 9).addBox(3.7F, -8.3F, -4.0F, 1.0F, 2.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 5).addBox(3.7F, -6.3F, 1.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -12.0F, 0.0F));

        Head.addOrReplaceChild("cube_r1",
                CubeListBuilder.create().texOffs(0, 19).addBox(-1.0F, -1.41F, -3.0F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(5.0F, -8.0F, -1.0F, 0.0F, 0.0F, -0.7854F));

        Head.addOrReplaceChild("cube_r2",
                CubeListBuilder.create().texOffs(18, 19).addBox(-1.0F, -1.41F, -3.0F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.0F, -8.0F, -1.0F, 0.0F, 0.0F, -0.7854F));

        Head.addOrReplaceChild("cube_r3",
                CubeListBuilder.create().texOffs(32, 0).addBox(-1.0F, -1.9F, -3.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 0).addBox(7.4F, -1.9F, -3.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.7F, -4.7F, -0.2F, -0.274F, 0.0F, 0.0F));

        Head.addOrReplaceChild("cube_r4",
                CubeListBuilder.create().texOffs(18, 31).addBox(-9.0F, -1.42F, -2.98F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(5.0F, -9.4F, 6.4F, 0.7854F, 0.0F, 0.0F));

        Head.addOrReplaceChild("cube_r5",
                CubeListBuilder.create().texOffs(2, 33).addBox(-1.0F, -3.0F, -1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(2, 33).addBox(-1.0F, -3.0F, -1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, -5.3F, 4.7F, 0.0F, -0.7854F, 0.0F));

        Head.addOrReplaceChild("cube_r6",
                CubeListBuilder.create().texOffs(2, 33).addBox(-1.0F, -3.0F, -1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, -5.3F, 4.7F, 0.0F, -0.7854F, 0.0F));

        Head.addOrReplaceChild("cube_r7",
                CubeListBuilder.create().texOffs(6, 33).addBox(-1.0F, -3.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, -5.3F, -3.3F, 0.0F, -0.7854F, 0.0F));

        Head.addOrReplaceChild("cube_r8",
                CubeListBuilder.create().texOffs(6, 33).addBox(-1.0F, -3.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, -5.3F, -3.3F, 0.0F, -0.7854F, 0.0F));

        Head.addOrReplaceChild("cube_r9",
                CubeListBuilder.create().texOffs(18, 31).addBox(-9.0F, -1.41F, -2.98F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(5.0F, -9.4F, -1.6F, 0.7854F, 0.0F, 0.0F));

        Head.addOrReplaceChild("cube_r10",
                CubeListBuilder.create().texOffs(0, 32).addBox(-4.0F, -2.0F, -1.0F, 8.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 2.2F, -2.0F, -0.274F, 0.0F, 0.0F));

        Head.addOrReplaceChild("cube_r11",
                CubeListBuilder.create().texOffs(0, 33).addBox(-0.99F, -5.0F, 0.0F, 0.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 33).addBox(-9.01F, -5.0F, 0.0F, 0.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(5.0F, -1.9F, -1.9F, -0.274F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        Waist.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
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
                        copyFrom(this.Head, headPart);
                });
        }
}