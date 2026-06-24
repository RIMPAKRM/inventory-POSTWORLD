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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import org.inventory.inventory.Inventory;
import org.inventory.inventory.capability.IPlayerLoadout;
import org.inventory.inventory.capability.LoadoutCapability;
import org.inventory.inventory.client.renderer.backpack.BackpackModel;
import org.inventory.inventory.client.renderer.backpack.black_shoulder_bag;
import org.inventory.inventory.client.renderer.face.M40GasmaskModel;
import org.inventory.inventory.client.renderer.head.CapModel;
import org.inventory.inventory.client.renderer.head.HatModel;
import org.inventory.inventory.client.renderer.head.Helmet6b47DesertEmrModel;
import org.inventory.inventory.client.renderer.head.HelmetPasgtPressModel;
import org.inventory.inventory.client.renderer.head.TacticalHelmetDesertModel;
import org.inventory.inventory.client.renderer.head.UsaHazmatCapModel;
import org.inventory.inventory.client.renderer.head.WeldingMaskModel;
import org.inventory.inventory.domain.EquipmentSlotType;
import org.inventory.inventory.client.renderer.vest.DdrBeltModel;
import org.inventory.inventory.client.renderer.vest.LeopardPressVestModel;
import org.inventory.inventory.client.renderer.vest.TacticalVestModel;
import org.inventory.inventory.client.renderer.vest.Vest6b2TanModel;
import org.inventory.inventory.client.renderer.vest.Vest6sh117DesertModel;
import org.inventory.inventory.client.renderer.vest.VestLifchikModel;
import org.inventory.inventory.client.renderer.vest.VestPlateCarrierDesertModel;

import java.util.Optional;

/**
 * Draws equipped custom loadout pieces directly on player model parts.
 */
public final class LoadoutArmorLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation VANILLA_LAYER_1 = ResourceLocation.withDefaultNamespace("textures/models/armor/leather_layer_1.png");
    private static final ResourceLocation VANILLA_LAYER_2 = ResourceLocation.withDefaultNamespace("textures/models/armor/leather_layer_2.png");
    private static final ResourceLocation VANILLA_CHAIN_LAYER_1 = ResourceLocation.withDefaultNamespace("textures/models/armor/chainmail_layer_1.png");
    private static final ResourceLocation VANILLA_IRON_LAYER_2 = ResourceLocation.withDefaultNamespace("textures/models/armor/iron_layer_2.png");

    private final BackpackModel<AbstractClientPlayer> backpackModel;
    private final black_shoulder_bag<AbstractClientPlayer> blackShoulderBagModel;
    private final CapModel<AbstractClientPlayer> capModel;
    private final HatModel<AbstractClientPlayer> hatModel;
    private final WeldingMaskModel<AbstractClientPlayer> weldingMaskModel;
    private final Helmet6b47DesertEmrModel<AbstractClientPlayer> helmet6b47DesertEmrModel;
    private final HelmetPasgtPressModel<AbstractClientPlayer> helmetPasgtPressModel;
    private final M40GasmaskModel<AbstractClientPlayer> m40GasmaskModel;
    private final UsaHazmatCapModel<AbstractClientPlayer> usaHazmatCapModel;
    private final DdrBeltModel<AbstractClientPlayer> ddrBeltModel;
    private final VestLifchikModel<AbstractClientPlayer> vestLifchikModel;
    private final Vest6sh117DesertModel<AbstractClientPlayer> vest6sh117DesertModel;
    private final Vest6b2TanModel<AbstractClientPlayer> vest6b2TanModel;
    private final VestPlateCarrierDesertModel<AbstractClientPlayer> vestPlateCarrierDesertModel;
    private final LeopardPressVestModel<AbstractClientPlayer> leopardPressVestModel;
    private final TacticalVestModel<AbstractClientPlayer> tacticalVestModel;
    private final TacticalHelmetDesertModel<AbstractClientPlayer> tacticalHelmetDesertModel;

    public LoadoutArmorLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
        this.backpackModel = new BackpackModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(BackpackModel.LAYER_LOCATION));
        this.blackShoulderBagModel = new black_shoulder_bag<>(Minecraft.getInstance().getEntityModels().bakeLayer(black_shoulder_bag.LAYER_LOCATION));
        this.capModel = new CapModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(CapModel.LAYER_LOCATION));
        this.hatModel = new HatModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(HatModel.LAYER_LOCATION));
        this.weldingMaskModel = new WeldingMaskModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(WeldingMaskModel.LAYER_LOCATION));
        this.helmet6b47DesertEmrModel = new Helmet6b47DesertEmrModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(Helmet6b47DesertEmrModel.LAYER_LOCATION));
        this.helmetPasgtPressModel = new HelmetPasgtPressModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(HelmetPasgtPressModel.LAYER_LOCATION));
        this.m40GasmaskModel = new M40GasmaskModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(M40GasmaskModel.LAYER_LOCATION));
        this.usaHazmatCapModel = new UsaHazmatCapModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(UsaHazmatCapModel.LAYER_LOCATION));
        this.ddrBeltModel = new DdrBeltModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(DdrBeltModel.LAYER_LOCATION));
        this.vestLifchikModel = new VestLifchikModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(VestLifchikModel.LAYER_LOCATION));
        this.vest6sh117DesertModel = new Vest6sh117DesertModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(Vest6sh117DesertModel.LAYER_LOCATION));
        this.vest6b2TanModel = new Vest6b2TanModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(Vest6b2TanModel.LAYER_LOCATION));
        this.vestPlateCarrierDesertModel = new VestPlateCarrierDesertModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(VestPlateCarrierDesertModel.LAYER_LOCATION));
        this.leopardPressVestModel = new LeopardPressVestModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(LeopardPressVestModel.LAYER_LOCATION));
        this.tacticalVestModel = new TacticalVestModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(TacticalVestModel.LAYER_LOCATION));
        this.tacticalHelmetDesertModel = new TacticalHelmetDesertModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(TacticalHelmetDesertModel.LAYER_LOCATION));
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

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            return;
        }

        String itemPath = itemId.getPath();
        Optional<ResourceLocation> textureOpt = resolveTexture(stack, slotType);
        if (textureOpt.isEmpty()) {
            return;
        }
        ResourceLocation texture = textureOpt.get();

        if (slotType == EquipmentSlotType.HEAD && ("cap".equals(itemPath) || "cap_blue".equals(itemPath) || "cap_black".equals(itemPath) || "cap_white".equals(itemPath))) {
            this.capModel.renderOnPlayer(poseStack, buffer, packedLight, player, this.getParentModel().body, this.getParentModel().head, texture);
            return;
        }

        if (slotType == EquipmentSlotType.HEAD && ("hat_black".equals(itemPath) || "hat_gray".equals(itemPath) || "hat_blue".equals(itemPath) || "hat_green".equals(itemPath) || "hat_red".equals(itemPath))) {
            this.hatModel.renderOnPlayer(poseStack, buffer, packedLight, player, this.getParentModel().body, this.getParentModel().head, texture);
            return;
        }

        if (slotType == EquipmentSlotType.HEAD && "helmet_6b47_desert_emr".equals(itemPath)) {
            this.helmet6b47DesertEmrModel.renderOnPlayer(poseStack, buffer, packedLight, player, this.getParentModel().body, this.getParentModel().head, texture);
            return;
        }

        if (slotType == EquipmentSlotType.HEAD && "helmet_pasgt_press".equals(itemPath)) {
            this.helmetPasgtPressModel.renderOnPlayer(poseStack, buffer, packedLight, this.getParentModel().body, this.getParentModel().head, texture, player);
            return;
        }

        if (slotType == EquipmentSlotType.HEAD && ("welding_mask".equals(itemPath) || "welding_mask_kill".equals(itemPath))) {
            this.weldingMaskModel.renderOnPlayer(poseStack, buffer, packedLight, player, this.getParentModel().body, this.getParentModel().head, texture);
            return;
        }

        if (slotType == EquipmentSlotType.FACE && "m40_gasmask".equals(itemPath)) {
            this.m40GasmaskModel.renderOnPlayer(poseStack, buffer, packedLight, player, this.getParentModel().body, this.getParentModel().head, texture);
            return;
        }

        if (slotType == EquipmentSlotType.HEAD && "usa_hazmat_cap".equals(itemPath)) {
            this.usaHazmatCapModel.renderOnPlayer(poseStack, buffer, packedLight, player, this.getParentModel().body, this.getParentModel().head, texture);
            return;
        }

        if (slotType == EquipmentSlotType.VEST && "ddr_belt".equals(itemPath)) {
            this.ddrBeltModel.renderOnPlayer(poseStack, buffer, packedLight, player, this.getParentModel().body, texture);
            return;
        }

        if (slotType == EquipmentSlotType.VEST && "vest_lifchik".equals(itemPath)) {
            this.vestLifchikModel.renderOnPlayer(poseStack, buffer, packedLight, player, this.getParentModel().body, this.getParentModel().rightArm, this.getParentModel().leftArm, texture);
            return;
        }

        if (slotType == EquipmentSlotType.VEST && "vest_6sh117_desert".equals(itemPath)) {
            this.vest6sh117DesertModel.renderOnPlayer(poseStack, buffer, packedLight, player, this.getParentModel().body, this.getParentModel().rightArm, this.getParentModel().leftArm, texture);
            return;
        }

        if (slotType == EquipmentSlotType.VEST && "vest_6b2_tan".equals(itemPath)) {
            this.vest6b2TanModel.renderOnPlayer(poseStack, buffer, packedLight, player, this.getParentModel().body, this.getParentModel().rightArm, this.getParentModel().leftArm, texture);
            return;
        }

        if (slotType == EquipmentSlotType.VEST && "vest_plate_carrier_desert".equals(itemPath)) {
            this.vestPlateCarrierDesertModel.renderOnPlayer(poseStack, buffer, packedLight, player, this.getParentModel().body, texture);
            return;
        }

        if (slotType == EquipmentSlotType.VEST && "leopard_press_vest".equals(itemPath)) {
            this.leopardPressVestModel.renderOnPlayer(poseStack, buffer, packedLight, this.getParentModel().body, this.getParentModel().rightArm, this.getParentModel().leftArm, texture, player);
            return;
        }

        if (slotType == EquipmentSlotType.HEAD && "tactical_helmet_desert".equals(itemPath)) {
            this.tacticalHelmetDesertModel.renderOnPlayer(poseStack, buffer, packedLight, this.getParentModel().body, this.getParentModel().head, texture, player);
            return;
        }

        if (slotType == EquipmentSlotType.VEST && ("tactical_vest".equals(itemPath) || "tactical_vest_black".equals(itemPath))) {
            this.tacticalVestModel.renderOnPlayer(poseStack, buffer, packedLight, player, this.getParentModel().body, texture);
            return;
        }

        if (slotType == EquipmentSlotType.BACKPACK) {
            if ("black_shoulder_bag".equals(itemPath)) {
                this.blackShoulderBagModel.renderOnPlayer(poseStack, buffer, packedLight, player, this.getParentModel().body, texture);
            } else {
                renderBackpack(poseStack, buffer, packedLight, player, texture);
            }
            return;
        }

        model.setAllVisible(false);
        setVisiblePartsForSlot(model, slotType);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        poseStack.pushPose();
        inflateVisibleParts(model, slotType);
        model.renderToBuffer(poseStack, vc, packedLight, LivingEntityRenderer.getOverlayCoords(player, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
        deflateVisibleParts(model, slotType);
        poseStack.popPose();
    }

    private void inflateVisibleParts(PlayerModel<AbstractClientPlayer> model, EquipmentSlotType slotType) {
        float inflation = 0.01F;
        switch (slotType) {
            case HEAD -> inflate(model.hat, inflation);
            case FACE -> inflate(model.head, inflation);
            case CHEST -> {
                inflate(model.body, inflation);
                inflate(model.rightArm, inflation);
                inflate(model.leftArm, inflation);
                inflate(model.rightSleeve, inflation);
                inflate(model.leftSleeve, inflation);
            }
            case GLOVES -> {
                inflate(model.rightArm, inflation);
                inflate(model.leftArm, inflation);
                inflate(model.rightSleeve, inflation);
                inflate(model.leftSleeve, inflation);
            }
            case LEGS, FEET -> {
                inflate(model.rightLeg, inflation);
                inflate(model.leftLeg, inflation);
                inflate(model.rightPants, inflation);
                inflate(model.leftPants, inflation);
            }
        }
    }

    private void deflateVisibleParts(PlayerModel<AbstractClientPlayer> model, EquipmentSlotType slotType) {
        float inflation = -0.01F;
        switch (slotType) {
            case HEAD -> inflate(model.hat, inflation);
            case FACE -> inflate(model.head, inflation);
            case CHEST -> {
                inflate(model.body, inflation);
                inflate(model.rightArm, inflation);
                inflate(model.leftArm, inflation);
                inflate(model.rightSleeve, inflation);
                inflate(model.leftSleeve, inflation);
            }
            case GLOVES -> {
                inflate(model.rightArm, inflation);
                inflate(model.leftArm, inflation);
                inflate(model.rightSleeve, inflation);
                inflate(model.leftSleeve, inflation);
            }
            case LEGS, FEET -> {
                inflate(model.rightLeg, inflation);
                inflate(model.leftLeg, inflation);
                inflate(model.rightPants, inflation);
                inflate(model.leftPants, inflation);
            }
        }
    }

    private void inflate(net.minecraft.client.model.geom.ModelPart part, float delta) {
        part.xScale += delta;
        part.yScale += delta;
        part.zScale += delta;
    }

    private void setVisiblePartsForSlot(PlayerModel<AbstractClientPlayer> model, EquipmentSlotType slotType) {
        switch (slotType) {
            case HEAD -> model.hat.visible = true;
            case FACE -> model.head.visible = true;
            case CHEST -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
                model.rightSleeve.visible = true;
                model.leftSleeve.visible = true;
            }
            case VEST -> model.jacket.visible = true;
            case GLOVES -> {
                model.rightArm.visible = true;
                model.leftArm.visible = true;
                model.rightSleeve.visible = true;
                model.leftSleeve.visible = true;
            }
            case LEGS -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
                model.rightPants.visible = true;
                model.leftPants.visible = true;
            }
            case FEET -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
                model.rightPants.visible = true;
                model.leftPants.visible = true;
            }
            case BACKPACK -> {
            }
        }
    }

    private void renderBackpack(PoseStack poseStack,
                                MultiBufferSource buffer,
                                int packedLight,
                                AbstractClientPlayer player,
                                ResourceLocation texture) {
        this.backpackModel.bone.copyFrom(this.getParentModel().body);
        this.backpackModel.bb_main.copyFrom(this.getParentModel().body);

        double verticalY = player.isCrouching() ? 9.5D / 16.0D : 12.0D / 16.0D;
        double forwardZ = player.isCrouching() ? 7.5D / 16.0D : 2.0D / 16.0D;

        poseStack.pushPose();
        poseStack.translate(0.0D, verticalY, forwardZ);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        this.backpackModel.renderToBuffer(poseStack, vc, packedLight, LivingEntityRenderer.getOverlayCoords(player, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    private Optional<ResourceLocation> resolveTexture(ItemStack stack, EquipmentSlotType slotType) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            return Optional.empty();
        }

        String itemName = itemId.getPath();

        if ("cargo_pants".equals(itemName)) return firstExisting(customEntity("cargo_pants"));
        if ("business_pants".equals(itemName)) return firstExisting(customEntity("business_pants"));
        if ("jeans_black".equals(itemName)) return firstExisting(customEntity("jeans_black"));
        if ("patrol_jacket".equals(itemName)) return firstExisting(customEntity("patrol_jacket"));
        if ("hoodie_blue".equals(itemName)) return firstExisting(customEntity("hoodie_blue"));
        if ("vest_and_white_shirt".equals(itemName)) return firstExisting(customEntity("vest_and_white_shirt"));
        if ("jacket".equals(itemName)) return firstExisting(customEntity("jacket"));
        if ("homemade_reinforced_shirt".equals(itemName)) return firstExisting(customEntity("homemade_reinforced_shirt"));
        if ("shirt_red".equals(itemName)) return firstExisting(customEntity("shirt_red"));
        if ("shirt_green".equals(itemName)) return firstExisting(customEntity("shirt_green"));
        if ("shirt_blue".equals(itemName)) return firstExisting(customEntity("shirt_blue"));
        if ("sneakers_red".equals(itemName)) return firstExisting(customEntity("sneakers_red"));
        if ("sneakers_green".equals(itemName)) return firstExisting(customEntity("sneakers_green"));
        if ("sneakers_blue".equals(itemName)) return firstExisting(customEntity("sneakers_blue"));
        if ("shoes".equals(itemName)) return firstExisting(customEntity("shoes"));
        if ("travel_backpack".equals(itemName)) return firstExisting(customEntity("backpack"));
        if ("black_shoulder_bag".equals(itemName)) return firstExisting(customEntity("black_shoulder_bag"));
        if ("cap".equals(itemName)) return firstExisting(customEntity("cap"));
        if ("cap_blue".equals(itemName)) return firstExisting(customEntity("cap_blue"));
        if ("cap_white".equals(itemName)) return firstExisting(customEntity("cap_white"));
        if ("cap_black".equals(itemName)) return firstExisting(customEntity("cap_black"));
        if ("hat_black".equals(itemName)) return firstExisting(customEntity("hat_black"));
        if ("hat_gray".equals(itemName)) return firstExisting(customEntity("hat_gray"));
        if ("hat_blue".equals(itemName)) return firstExisting(customEntity("hat_blue"));
        if ("hat_green".equals(itemName)) return firstExisting(customEntity("hat_green"));
        if ("hat_red".equals(itemName)) return firstExisting(customEntity("hat_red"));
        if ("welding_mask".equals(itemName)) return firstExisting(customEntity("welding_mask"));
        if ("welding_mask_kill".equals(itemName)) return firstExisting(customEntity("welding_mask_kill"));
        if ("helmet_pasgt_press".equals(itemName)) return firstExisting(customEntity("helmet_pasgt_press"));
        if ("helmet_6b47_desert_emr".equals(itemName)) return firstExisting(customEntity("helmet_6b47_desert_emr"));
        if ("m40_gasmask".equals(itemName)) return firstExisting(customEntity("m40_gasmask"));
        if ("balaclava_green".equals(itemName)) return firstExisting(customEntity("balaclava_green"));
        if ("balaclava_white".equals(itemName)) return firstExisting(customEntity("balaclava_white"));
        if ("usa_hazmat_cap".equals(itemName)) return firstExisting(customEntity("usa_hazmat_cap"));
        if ("usa_hazmat_chestplate".equals(itemName)) return firstExisting(customEntity("usa_hazmat_chestplate"));
        if ("usa_hazmat_leggings".equals(itemName)) return firstExisting(customEntity("usa_hazmat_leggings"));
        if ("tactical_helmet_desert".equals(itemName)) return firstExisting(customEntity("tactical_helmet_desert"));
        if ("leopard_press_vest".equals(itemName)) return firstExisting(customEntity("leopard_press_vest"));
        if ("balaclava".equals(itemName)) return firstExisting(customEntity("balaclava_black"), customEntity("balaclava"));
        if ("tactical_vest".equals(itemName)) return firstExisting(customEntity("tactical_vest"));
        if ("tactical_vest_black".equals(itemName)) return firstExisting(customEntity("tactical_vest_black"), customEntity("tactical_vest"));
        if ("ddr_belt".equals(itemName)) return firstExisting(customEntity("ddr_belt"));
        if ("vest_lifchik".equals(itemName)) return firstExisting(customEntity("vest_lifchik"));
        if ("vest_6sh117_desert".equals(itemName)) return firstExisting(customEntity("vest_6sh117_desert"));
        if ("vest_6b2_tan".equals(itemName)) return firstExisting(customEntity("vest_6b2_tan"));
        if ("vest_plate_carrier_desert".equals(itemName)) return firstExisting(customEntity("vest_plate_carrier_desert"));
        if ("homemade_reinforced_gloves".equals(itemName)) return firstExisting(customEntity("homemade_reinforced_gloves"));
        if ("black_gloves".equals(itemName)) return firstExisting(customEntity("black_gloves"));
        if ("tactical_gloves".equals(itemName)) return firstExisting(customEntity("tactical_gloves"));
        if ("rubber_gloves_chemical_protection".equals(itemName)) return firstExisting(customEntity("rubber_gloves_chemical_protection"));
        if ("tactical_boots".equals(itemName)) return firstExisting(customEntity("tactical_boots"));
        if ("homemade_reinforced_boots".equals(itemName)) return firstExisting(customEntity("homemade_reinforced_boots"));
        if ("rubber_boots_chemical_protection".equals(itemName)) return firstExisting(customEntity("rubber_boots_chemical_protection"));
        if ("homemade_reinforced_pants".equals(itemName)) return firstExisting(customEntity("homemade_reinforced_pants"));
        if ("patrol_jacket_emr".equals(itemName)) return firstExisting(customEntity("patrol_jacket_emr"));

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

