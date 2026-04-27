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
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.inventory.inventory.Inventory;
import org.inventory.inventory.client.renderer.LoadoutRenderTransforms;

// Made with Blockbench 5.1.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class black_shoulder_bag<T extends Entity> extends EntityModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(Inventory.MODID, "black_shoulder_bag"), "main");
	/** Positive values move the bag downward. */
	public float yOffset = 1.5F;
	private final ModelPart bb_main;

	public black_shoulder_bag(ModelPart root) {
		this.bb_main = root.getChild("bb_main");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition cube_r1 = bb_main.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(14, 21).addBox(-1.0F, 5.0F, -1.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(24, 20).addBox(-1.0F, 1.0F, 2.2F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.1F, -18.0F, -1.1F, 0.0F, 0.0F, -0.6545F));

		PartDefinition cube_r2 = bb_main.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 26).addBox(-1.0F, -4.0F, 0.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.1F, -14.2F, 4.2F, 0.0F, 0.0F, 0.48F));

		PartDefinition cube_r3 = bb_main.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 7).addBox(-3.0F, -4.0F, -3.0F, 6.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -14.5F, 4.1F, 0.0F, 0.0F, 0.48F));

		PartDefinition cube_r4 = bb_main.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(0, 21).addBox(0.0F, -1.1F, -3.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.2F, -15.9F, 4.2F, 0.0F, 0.0F, 0.48F));

		PartDefinition cube_r5 = bb_main.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(0, 13).addBox(0.0F, -4.0F, -3.0F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, -16.3F, 4.2F, 0.0F, 0.0F, 0.48F));

		PartDefinition cube_r6 = bb_main.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(0, 21).addBox(0.0F, -1.1F, -3.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.2F, -13.0F, 4.2F, 0.0F, 0.0F, 0.48F));

		PartDefinition cube_r7 = bb_main.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(14, 13).addBox(0.0F, -4.0F, -3.0F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.4F, -13.4F, 4.2F, 0.0F, 0.0F, 0.48F));

		PartDefinition cube_r8 = bb_main.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(10, 13).addBox(-1.02F, -7.0F, -1.0F, 1.0F, 13.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.2F, -18.3F, -1.1F, 0.0F, 0.0F, -0.6196F));

		PartDefinition cube_r9 = bb_main.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(26, 0).addBox(-1.0F, -6.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.5F, -18.4F, -1.1F, 0.0F, 0.0F, -0.2182F));

		PartDefinition cube_r10 = bb_main.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(20, 7).addBox(-1.2F, 0.0F, -3.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.6F, -24.3F, 1.1F, 0.0F, 0.0F, -0.2182F));

		PartDefinition cube_r11 = bb_main.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(24, 12).addBox(-1.0F, -6.0F, -2.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.5F, -18.4F, 3.1F, 0.0F, 0.0F, -0.2182F));

		PartDefinition cube_r12 = bb_main.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -4.0F, -1.0F, 10.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.8F, -13.5F, 3.0F, 0.0F, 0.0F, 0.48F));

		return LayerDefinition.create(meshdefinition, 32, 32);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		bb_main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void renderOnPlayer(PoseStack poseStack,
	                           MultiBufferSource buffer,
	                           int packedLight,
	                           AbstractClientPlayer player,
	                           ModelPart bodyPart,
	                           ResourceLocation texture) {
		poseStack.pushPose();
		poseStack.translate(0.0D, this.yOffset, 0.0D);
		LoadoutRenderTransforms.applyCrouchOffset(
				poseStack,
				player,
				LoadoutRenderTransforms.DEFAULT_BACKPACK_VERTICAL_OFFSET,
				LoadoutRenderTransforms.DEFAULT_BACKPACK_FORWARD_OFFSET
		);
		

		this.bb_main.copyFrom(bodyPart);

		VertexConsumer vc = buffer.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(texture));
		this.renderToBuffer(
				poseStack,
				vc,
				packedLight,
				net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(player, 0.0F),
				1.0F, 1.0F, 1.0F, 1.0F
		);
		poseStack.popPose();
	}
}