package org.inventory.inventory.client.renderer.backpack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.inventory.inventory.Inventory;

/**
 * Backpack model exported from Blockbench.
 */
public class BackpackModel<T extends Entity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(Inventory.MODID, "backpack"), "main");

    public final ModelPart bone;
    public final ModelPart bb_main;

    public BackpackModel(ModelPart root) {
        this.bone = root.getChild("bone");
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bone = partdefinition.addOrReplaceChild("bone",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -12.0F, 0.0F, 8.0F, 12.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(22, 0)
                        .addBox(-4.1F, -2.0F, -4.0F, 0.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(14, 15)
                        .addBox(4.1F, -2.0F, -4.0F, 0.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 15)
                        .addBox(-3.0F, -11.0F, 3.0F, 6.0F, 10.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 36.0F, 2.0F));

        PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main",
                CubeListBuilder.create()
                        .texOffs(0, 26)
                        .addBox(-4.0F, -12.0F, -4.1F, 1.0F, 12.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(22, 7)
                        .addBox(-4.0F, -12.1F, -4.0F, 1.0F, 0.0F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(2, 26)
                        .addBox(3.0F, -12.0F, -4.1F, 1.0F, 12.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(14, 22)
                        .addBox(3.0F, -12.1F, -4.0F, 1.0F, 0.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 36.0F, 2.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // No animation needed for a static backpack.
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
        bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        bb_main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
