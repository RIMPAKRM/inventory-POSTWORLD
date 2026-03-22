package org.inventory.inventory.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.inventory.inventory.client.renderer.backpack.BackpackModel;
import org.inventory.inventory.client.renderer.head.CapModel;
import org.inventory.inventory.client.renderer.vest.TacticalVestModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import org.inventory.inventory.Inventory;
import org.inventory.inventory.capability.IPlayerLoadout;
import org.inventory.inventory.capability.LoadoutCapability;
import org.inventory.inventory.domain.EquipmentSlotType;

import java.util.Optional;

/**
 * Draws equipped custom loadout pieces directly on player model parts.
 */
public final class LoadoutArmorLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation VANILLA_LAYER_1 =
            ResourceLocation.withDefaultNamespace("textures/models/armor/leather_layer_1.png");
    private static final ResourceLocation VANILLA_LAYER_2 =
            ResourceLocation.withDefaultNamespace("textures/models/armor/leather_layer_2.png");
    private static final ResourceLocation VANILLA_CHAIN_LAYER_1 =
            ResourceLocation.withDefaultNamespace("textures/models/armor/chainmail_layer_1.png");
    private static final ResourceLocation VANILLA_IRON_LAYER_2 =
            ResourceLocation.withDefaultNamespace("textures/models/armor/iron_layer_2.png");

    private final BackpackModel<AbstractClientPlayer> backpackModel;
    private final CapModel<AbstractClientPlayer> capModel;
    private final TacticalVestModel<AbstractClientPlayer> tacticalVestModel;

    public LoadoutArmorLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
        this.backpackModel = new BackpackModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(BackpackModel.LAYER_LOCATION));
        this.capModel = new CapModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(CapModel.LAYER_LOCATION));
        this.tacticalVestModel = new TacticalVestModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(TacticalVestModel.LAYER_LOCATION));
    }

    @Override
    public void render(PoseStack poseStack,
                       MultiBufferSource buffer,
                       int packedLight,
                       AbstractClientPlayer player,
                       float limbSwing,
                       float limbSwingAmount,
                       float partialTick,
                       float ageInTicks,
                       float netHeadYaw,
                       float headPitch) {
        LazyOptional<IPlayerLoadout> loadoutOpt = player.getCapability(LoadoutCapability.PLAYER_LOADOUT);
        IPlayerLoadout loadout = loadoutOpt.resolve().orElse(null);
        if (loadout == null) {
            return;
        }

        PlayerModel<AbstractClientPlayer> model = this.getParentModel();
        renderSlot(poseStack, buffer, packedLight, player, model, loadout, EquipmentSlotType.HEAD);
        renderSlot(poseStack, buffer, packedLight, player, model, loadout, EquipmentSlotType.FACE);
        renderSlot(poseStack, buffer, packedLight, player, model, loadout, EquipmentSlotType.CHEST);
        renderSlot(poseStack, buffer, packedLight, player, model, loadout, EquipmentSlotType.VEST);
        renderSlot(poseStack, buffer, packedLight, player, model, loadout, EquipmentSlotType.BACKPACK);
        renderSlot(poseStack, buffer, packedLight, player, model, loadout, EquipmentSlotType.GLOVES);
        renderSlot(poseStack, buffer, packedLight, player, model, loadout, EquipmentSlotType.LEGS);
        renderSlot(poseStack, buffer, packedLight, player, model, loadout, EquipmentSlotType.FEET);

        // Restore default visibility so downstream layers are unaffected.
        model.setAllVisible(true);
    }

    private void renderSlot(PoseStack poseStack,
                            MultiBufferSource buffer,
                            int packedLight,
                            AbstractClientPlayer player,
                            PlayerModel<AbstractClientPlayer> model,
                            IPlayerLoadout loadout,
                            EquipmentSlotType slotType) {
        ItemStack stack = loadout.getEquipment(slotType);
        if (stack.isEmpty()) {
            return;
        }

        ResourceLocation texture = resolveTexture(stack, slotType).orElse(null);
        if (texture == null) {
            return;
        }

        // Head cap is rendered as a dedicated 3D model layer (custom blockbench model)
        if (slotType == EquipmentSlotType.HEAD && "cap".equals(ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath())) {
            renderCap(poseStack, buffer, packedLight, player);
            return;
        }

        // Tactical vests are rendered as a dedicated model layer.
        if (slotType == EquipmentSlotType.VEST && ("tactical_vest".equals(ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath()) || "tactical_vest_black".equals(ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath()))) {
            renderTacticalVest(poseStack, buffer, packedLight, player, texture);
            return;
        }

        // Backpack is rendered as a dedicated 3D model layer (like modern warfare mods do)
        if (slotType == EquipmentSlotType.BACKPACK) {
            renderBackpack(poseStack, buffer, packedLight, player, texture);
            return;
        }

        model.setAllVisible(false);
        setVisiblePartsForSlot(model, slotType);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        model.renderToBuffer(
                poseStack,
                vc,
                packedLight,
                LivingEntityRenderer.getOverlayCoords(player, 0.0F),
                1.0F, 1.0F, 1.0F, 1.0F
        );
    }

    private void setVisiblePartsForSlot(PlayerModel<AbstractClientPlayer> model, EquipmentSlotType slotType) {
        switch (slotType) {
            // Head slot = vanilla helmet layer (headwear)
            case HEAD -> {
                model.hat.visible = true;
            }
            // Face slot = inner head layer only (head model, not hat)
            case FACE -> {
                model.head.visible = true;
            }
            // Chest slot = inner body layer (body + arms + sleeves)
            // (sleeves are added so chest and gloves can overlap properly)
            case CHEST -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
                model.rightSleeve.visible = true;
                model.leftSleeve.visible = true;
            }
            // Vest = outer body layer (jacket)
            case VEST -> {
                model.jacket.visible = true;
            }
            // Gloves = outer arm layer (sleeves + arms)
            // Render gloves after chest so gloves override overlapping pixels.
            case GLOVES -> {
                model.rightArm.visible = true;
                model.leftArm.visible = true;
                model.rightSleeve.visible = true;
                model.leftSleeve.visible = true;
            }
            // Legs = inner leg layer + outer leg layer (pants)
            // so legs slot renders both inner and outer, allowing boots to override later
            case LEGS -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
                model.rightPants.visible = true;
                model.leftPants.visible = true;
            }
            // Feet = outer leg layer (boots) + inner leg layer (to allow boots to override base leg where overlapping)
            case FEET -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
                model.rightPants.visible = true;
                model.leftPants.visible = true;
            }
            // Backpack is handled by a dedicated model layer
            case BACKPACK -> {
                // No default player model parts should be visible for the backpack.
            }
        }
    }

    private void renderBackpack(PoseStack poseStack,
                                MultiBufferSource buffer,
                                int packedLight,
                                AbstractClientPlayer player,
                                ResourceLocation texture) {
        // Copy rotation/position from the player's body so the backpack follows movement and sneaking.
        this.backpackModel.bone.copyFrom(this.getParentModel().body);
        this.backpackModel.bb_main.copyFrom(this.getParentModel().body);

        // Apply a fixed offset so the backpack sits slightly above and closer to the body.
        // When crouching, adjust both vertical and forward offset so it stays aligned with the body.
        double verticalY = player.isCrouching() ? 9.5D / 16.0D : 12.0D / 16.0D;
        double forwardZ = player.isCrouching() ? 7.5D / 16.0D : 2.0D / 16.0D;

        poseStack.pushPose();
        poseStack.translate(0.0D, verticalY, forwardZ);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        this.backpackModel.renderToBuffer(
                poseStack,
                vc,
                packedLight,
                LivingEntityRenderer.getOverlayCoords(player, 0.0F),
                1.0F, 1.0F, 1.0F, 1.0F
        );
        poseStack.popPose();
    }

    private void renderTacticalVest(PoseStack poseStack,
                                    MultiBufferSource buffer,
                                    int packedLight,
                                    AbstractClientPlayer player,
                                    ResourceLocation texture) {
        this.tacticalVestModel.Waist.copyFrom(this.getParentModel().body);
        this.tacticalVestModel.Body.copyFrom(this.getParentModel().body);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        this.tacticalVestModel.renderToBuffer(
                poseStack,
                vc,
                packedLight,
                LivingEntityRenderer.getOverlayCoords(player, 0.0F),
                1.0F, 1.0F, 1.0F, 1.0F
        );
    }

    private void renderCap(PoseStack poseStack,
                           MultiBufferSource buffer,
                           int packedLight,
                           AbstractClientPlayer player) {
        // Follow head rotation and position, then apply the offset from the model.
        this.capModel.Waist.copyFrom(this.getParentModel().body);
        this.capModel.Head.copyFrom(this.getParentModel().head);

        poseStack.pushPose();
        // Поставлено 0, чтобы не было NaN/Infinity. Если нужно смещение, выставьте в пикселях в дробных долях блока.
        poseStack.translate(0.0D, 0.0D, 0.0D);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(new ResourceLocation(Inventory.MODID, "textures/entities/cap.png")));
        this.capModel.renderToBuffer(
                poseStack,
                vc,
                packedLight,
                LivingEntityRenderer.getOverlayCoords(player, 0.0F),
                1.0F, 1.0F, 1.0F, 1.0F
        );
        poseStack.popPose();
    }

    private Optional<ResourceLocation> resolveTexture(ItemStack stack, EquipmentSlotType slotType) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            return Optional.empty();
        }

        String itemName = itemId.getPath();

        // Explicit custom textures that user provided in assets/inventory/textures/entities.
        if ("cargo_pants".equals(itemName)) {
            return firstExisting(customEntity("cargo_pants"));
        }
        if ("patrol_jacket".equals(itemName)) {
            return firstExisting(customEntity("patrol_jacket"));
        }
        if ("travel_backpack".equals(itemName)) {
            return firstExisting(customEntity("backpack"));
        }
        if ("cap".equals(itemName)) {
            return firstExisting(customEntity("cap"));
        }
        if ("desert_cap".equals(itemName)) {
            return firstExisting(customEntity("desert_cap"), customEntity("desert_cap_t"));
        }
        if ("balaclava".equals(itemName)) {
            return firstExisting(customEntity("balaclava_black"), customEntity("balaclava"));
        }
        if ("tactical_vest".equals(itemName)) {
            return firstExisting(customEntity("tactical_vest"));
        }
        if ("tactical_vest_black".equals(itemName)) {
            return firstExisting(customEntity("tactical_vest_black"), customEntity("tactical_vest"));
        }
        if ("tactical_gloves".equals(itemName)) {
            return firstExisting(customEntity("tactical_gloves"));
        }
        if ("tactical_boots".equals(itemName)) {
            return firstExisting(customEntity("tactical_boots"));
        }

        // Vanilla fallback for items without dedicated entity texture.
        return Optional.of(vanillaFallback(slotType, itemName));
    }

    private ResourceLocation vanillaFallback(EquipmentSlotType slotType, String itemName) {
        if ("reinforced_cargo_pants".equals(itemName)) {
            return VANILLA_IRON_LAYER_2;
        }
        return switch (slotType) {
            case LEGS -> VANILLA_LAYER_2;
            case CHEST, VEST, BACKPACK, GLOVES -> VANILLA_CHAIN_LAYER_1;
            default -> VANILLA_LAYER_1;
        };
    }

    private Optional<ResourceLocation> firstExisting(ResourceLocation... candidates) {
        for (ResourceLocation candidate : candidates) {
            if (resourceExists(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private ResourceLocation customEntity(String name) {
        return ResourceLocation.fromNamespaceAndPath("inventory", "textures/entities/" + name + ".png");
    }

    private boolean resourceExists(ResourceLocation texture) {
        return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();
    }
}

