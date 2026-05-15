package org.inventory.inventory;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.inventory.inventory.capability.LoadoutCapability;
import org.inventory.inventory.data.DataDrivenContentLoader;
import org.inventory.inventory.menu.CustomInventoryMenu;
import org.inventory.inventory.menu.StorageBrowserMenu;
import org.inventory.inventory.item.GearTooltipItem;
import org.inventory.inventory.network.ModNetwork;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Inventory.MODID)
public class Inventory {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "inventory";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // ---- Deferred Registers ----
    // Create a Deferred Register to hold Blocks which will all be registered under the "inventory" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "inventory" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "inventory" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // Create a Deferred Register to hold MenuType which will all be registered under the "inventory" namespace
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    // ---- Example content (MDK scaffold — replace with mod content) ----
    private static Item gearItem(Item.Properties properties) {
        return new GearTooltipItem(properties);
    }

    private static Item.Properties lightGearProperties() {
        return new Item.Properties().stacksTo(1).durability(296);
    }

    private static Item.Properties mediumGearProperties() {
        return new Item.Properties().stacksTo(1).durability(432);
    }

    private static Item.Properties heavyGearProperties() {
        return new Item.Properties().stacksTo(1).durability(598);
    }

    public static final RegistryObject<Item> RUGGED_VEST = ITEMS.register("rugged_vest", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> TACTICAL_VEST = ITEMS.register("tactical_vest", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> TACTICAL_VEST_BLACK = ITEMS.register("tactical_vest_black", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> DDR_BELT = ITEMS.register("ddr_belt", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> VEST_LIFCHIK = ITEMS.register("vest_lifchik", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> VEST_6SH117_DESERT = ITEMS.register("vest_6sh117_desert", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> VEST_6B2_TAN = ITEMS.register("vest_6b2_tan", () -> gearItem(mediumGearProperties()));
    public static final RegistryObject<Item> VEST_PLATE_CARRIER_DESERT = ITEMS.register("vest_plate_carrier_desert", () -> gearItem(heavyGearProperties()));
    public static final RegistryObject<Item> TACTICAL_HELMET_DESERT = ITEMS.register("tactical_helmet_desert", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> HELMET_6B47_DESERT_EMR = ITEMS.register("helmet_6b47_desert_emr", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> M40_GASMASK = ITEMS.register("m40_gasmask", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> USA_HAZMAT_CAP = ITEMS.register("usa_hazmat_cap", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> USA_HAZMAT_CHESTPLATE = ITEMS.register("usa_hazmat_chestplate", () -> gearItem(mediumGearProperties()));
    public static final RegistryObject<Item> USA_HAZMAT_LEGGINGS = ITEMS.register("usa_hazmat_leggings", () -> gearItem(mediumGearProperties()));
    public static final RegistryObject<Item> BLACK_SHOULDER_BAG = ITEMS.register("black_shoulder_bag", () -> gearItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> TRAVEL_BACKPACK = ITEMS.register("travel_backpack", () -> gearItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SHIRT_RED = ITEMS.register("shirt_red", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> SHIRT_GREEN = ITEMS.register("shirt_green", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> SHIRT_BLUE = ITEMS.register("shirt_blue", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> TACTICAL_GLOVES = ITEMS.register("tactical_gloves", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> TACTICAL_BOOTS = ITEMS.register("tactical_boots", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> SNEAKERS_RED = ITEMS.register("sneakers_red", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> SNEAKERS_GREEN = ITEMS.register("sneakers_green", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> SNEAKERS_BLUE = ITEMS.register("sneakers_blue", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> RUBBER_GLOVES_CHEMICAL_PROTECTION = ITEMS.register("rubber_gloves_chemical_protection", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> RUBBER_BOOTS_CHEMICAL_PROTECTION = ITEMS.register("rubber_boots_chemical_protection", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> CARGO_PANTS = ITEMS.register("cargo_pants", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> JEANS_BLACK = ITEMS.register("jeans_black", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> PATROL_JACKET = ITEMS.register("patrol_jacket", () -> gearItem(mediumGearProperties()));

    public static final RegistryObject<Item> REINFORCED_CARGO_PANTS = ITEMS.register("reinforced_cargo_pants", () -> gearItem(lightGearProperties()));

    public static final RegistryObject<Item> BALACLAVA = ITEMS.register("balaclava", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> CAP = ITEMS.register("cap", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> CAP_BLUE = ITEMS.register("cap_blue", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> CAP_WHITE = ITEMS.register("cap_white", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> CAP_BLACK = ITEMS.register("cap_black", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> HAT_BLACK = ITEMS.register("hat_black", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> HAT_GRAY = ITEMS.register("hat_gray", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> HAT_BLUE = ITEMS.register("hat_blue", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> HAT_GREEN = ITEMS.register("hat_green", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> HAT_RED = ITEMS.register("hat_red", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> WELDING_MASK = ITEMS.register("welding_mask", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> WELDING_MASK_KILL = ITEMS.register("welding_mask_kill", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> HELMET_PASGT_PRESS = ITEMS.register("helmet_pasgt_press", () -> gearItem(mediumGearProperties()));
    public static final RegistryObject<Item> BALACLAVA_GREEN = ITEMS.register("balaclava_green", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> BALACLAVA_WHITE = ITEMS.register("balaclava_white", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> LEOPARD_PRESS_VEST = ITEMS.register("leopard_press_vest", () -> gearItem(mediumGearProperties()));
    public static final RegistryObject<Item> HOODIE_BLUE = ITEMS.register("hoodie_blue", () -> gearItem(mediumGearProperties()));
    public static final RegistryObject<Item> VEST_AND_WHITE_SHIRT = ITEMS.register("vest_and_white_shirt", () -> gearItem(mediumGearProperties()));
    public static final RegistryObject<Item> JACKET = ITEMS.register("jacket", () -> gearItem(mediumGearProperties()));
    public static final RegistryObject<Item> HOMEMADE_REINFORCED_SHIRT = ITEMS.register("homemade_reinforced_shirt", () -> gearItem(mediumGearProperties()));
    public static final RegistryObject<Item> HOMEMADE_REINFORCED_GLOVES = ITEMS.register("homemade_reinforced_gloves", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> BLACK_GLOVES = ITEMS.register("black_gloves", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> BUSINESS_PANTS = ITEMS.register("business_pants", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> HOMEMADE_REINFORCED_PANTS = ITEMS.register("homemade_reinforced_pants", () -> gearItem(mediumGearProperties()));
    public static final RegistryObject<Item> SHOES = ITEMS.register("shoes", () -> gearItem(lightGearProperties()));
    public static final RegistryObject<Item> HOMEMADE_REINFORCED_BOOTS = ITEMS.register("homemade_reinforced_boots", () -> gearItem(lightGearProperties()));

    // Creates a creative tab for all mod items
    public static final RegistryObject<CreativeModeTab> INVENTORY_TAB = CREATIVE_MODE_TABS.register("inventory_tab", () -> CreativeModeTab.builder().withTabsBefore(CreativeModeTabs.COMBAT).icon(() -> BLACK_SHOULDER_BAG.get().getDefaultInstance()).displayItems((parameters, output) -> {
        output.accept(RUGGED_VEST.get());
        output.accept(TACTICAL_VEST.get());
        output.accept(TACTICAL_VEST_BLACK.get());
        output.accept(DDR_BELT.get());
        output.accept(VEST_LIFCHIK.get());
        output.accept(VEST_6SH117_DESERT.get());
        output.accept(VEST_6B2_TAN.get());
        output.accept(VEST_PLATE_CARRIER_DESERT.get());
        output.accept(TACTICAL_HELMET_DESERT.get());
        output.accept(HELMET_6B47_DESERT_EMR.get());
        output.accept(M40_GASMASK.get());
        output.accept(USA_HAZMAT_CAP.get());
        output.accept(USA_HAZMAT_CHESTPLATE.get());
        output.accept(USA_HAZMAT_LEGGINGS.get());
        output.accept(BLACK_SHOULDER_BAG.get());
        output.accept(TRAVEL_BACKPACK.get());
        output.accept(SHIRT_RED.get());
        output.accept(SHIRT_GREEN.get());
        output.accept(SHIRT_BLUE.get());
        output.accept(TACTICAL_GLOVES.get());
        output.accept(RUBBER_GLOVES_CHEMICAL_PROTECTION.get());
        output.accept(TACTICAL_BOOTS.get());
        output.accept(RUBBER_BOOTS_CHEMICAL_PROTECTION.get());
        output.accept(SNEAKERS_RED.get());
        output.accept(SNEAKERS_GREEN.get());
        output.accept(SNEAKERS_BLUE.get());
        output.accept(CARGO_PANTS.get());
        output.accept(JEANS_BLACK.get());
        output.accept(PATROL_JACKET.get());
        output.accept(REINFORCED_CARGO_PANTS.get());
        output.accept(BALACLAVA.get());
        output.accept(CAP.get());
        output.accept(CAP_BLUE.get());
        output.accept(CAP_WHITE.get());
        output.accept(CAP_BLACK.get());
        output.accept(HAT_BLACK.get());
        output.accept(HAT_GRAY.get());
        output.accept(HAT_BLUE.get());
        output.accept(HAT_GREEN.get());
        output.accept(HAT_RED.get());
        output.accept(WELDING_MASK.get());
        output.accept(WELDING_MASK_KILL.get());
        output.accept(HELMET_PASGT_PRESS.get());
        output.accept(BALACLAVA_GREEN.get());
        output.accept(BALACLAVA_WHITE.get());
        output.accept(LEOPARD_PRESS_VEST.get());
        output.accept(HOODIE_BLUE.get());
        output.accept(VEST_AND_WHITE_SHIRT.get());
        output.accept(JACKET.get());
        output.accept(HOMEMADE_REINFORCED_SHIRT.get());
        output.accept(HOMEMADE_REINFORCED_GLOVES.get());
        output.accept(BLACK_GLOVES.get());
        output.accept(BUSINESS_PANTS.get());
        output.accept(HOMEMADE_REINFORCED_PANTS.get());
        output.accept(SHOES.get());
        output.accept(HOMEMADE_REINFORCED_BOOTS.get());
    }).build());

    // ---- Custom Inventory Menu ----
    // Register the custom inventory menu type
    public static final RegistryObject<MenuType<CustomInventoryMenu>> CUSTOM_INVENTORY_MENU = MENU_TYPES.register("custom_inventory", () -> IForgeMenuType.create(CustomInventoryMenu::new));
    public static final RegistryObject<MenuType<StorageBrowserMenu>> STORAGE_BROWSER_MENU = MENU_TYPES.register("storage_browser", () -> IForgeMenuType.create(StorageBrowserMenu::new));

    @SuppressWarnings("removal")
    public Inventory() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so menu types get registered
        MENU_TYPES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Register capability (must be on MOD bus)
        modEventBus.addListener(LoadoutCapability::onRegisterCapabilities);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("[Inventory] common setup");

        // Set the static MenuType reference used by CustomInventoryMenu's super() call
        CustomInventoryMenu.TYPE = CUSTOM_INVENTORY_MENU.get();
        StorageBrowserMenu.TYPE = STORAGE_BROWSER_MENU.get();

        // Phase B: register all network packets
        event.enqueueWork(ModNetwork::registerPackets);

        if (Config.logDirtBlock) LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // All mod items are now in the INVENTORY_TAB creative tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Items are added via the INVENTORY_TAB registration
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("[Inventory] server starting");
        DataDrivenContentLoader.reloadAll(event.getServer().getResourceManager());
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("[Inventory] client setup, player={}", Minecraft.getInstance().getUser().getName());

            // Load data-driven craft content on the client too so the craft menu is populated
            // even when the server runs in a separate process.
            DataDrivenContentLoader.reloadAll(Minecraft.getInstance().getResourceManager());

            // Phase B: register custom inventory screen for CustomInventoryMenu
            event.enqueueWork(() ->
                net.minecraft.client.gui.screens.MenuScreens.register(
                        CUSTOM_INVENTORY_MENU.get(),
                        org.inventory.inventory.client.screen.InventoryScreen::new)
            );
                event.enqueueWork(() ->
                    net.minecraft.client.gui.screens.MenuScreens.register(
                        STORAGE_BROWSER_MENU.get(),
                        org.inventory.inventory.client.screen.StorageBrowserScreen::new)
                );

        }

        @SubscribeEvent
        public static void onRegisterLayerDefinitions(net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(
                    org.inventory.inventory.client.renderer.backpack.BackpackModel.LAYER_LOCATION,
                    org.inventory.inventory.client.renderer.backpack.BackpackModel::createBodyLayer
            );
                event.registerLayerDefinition(
                    org.inventory.inventory.client.renderer.backpack.black_shoulder_bag.LAYER_LOCATION,
                    org.inventory.inventory.client.renderer.backpack.black_shoulder_bag::createBodyLayer
                );
            event.registerLayerDefinition(
                    org.inventory.inventory.client.renderer.head.CapModel.LAYER_LOCATION,
                    org.inventory.inventory.client.renderer.head.CapModel::createBodyLayer
            );
            event.registerLayerDefinition(
                org.inventory.inventory.client.renderer.head.HatModel.LAYER_LOCATION,
                org.inventory.inventory.client.renderer.head.HatModel::createBodyLayer
            );
                event.registerLayerDefinition(
                    org.inventory.inventory.client.renderer.head.TacticalHelmetDesertModel.LAYER_LOCATION,
                    org.inventory.inventory.client.renderer.head.TacticalHelmetDesertModel::createBodyLayer
                );
                event.registerLayerDefinition(
                    org.inventory.inventory.client.renderer.head.Helmet6b47DesertEmrModel.LAYER_LOCATION,
                    org.inventory.inventory.client.renderer.head.Helmet6b47DesertEmrModel::createBodyLayer
                );
            event.registerLayerDefinition(
                org.inventory.inventory.client.renderer.head.HelmetPasgtPressModel.LAYER_LOCATION,
                org.inventory.inventory.client.renderer.head.HelmetPasgtPressModel::createBodyLayer
            );
                event.registerLayerDefinition(
                        org.inventory.inventory.client.renderer.face.M40GasmaskModel.LAYER_LOCATION,
                        org.inventory.inventory.client.renderer.face.M40GasmaskModel::createBodyLayer
                );
            event.registerLayerDefinition(
                org.inventory.inventory.client.renderer.head.WeldingMaskModel.LAYER_LOCATION,
                org.inventory.inventory.client.renderer.head.WeldingMaskModel::createBodyLayer
            );
                event.registerLayerDefinition(
                        org.inventory.inventory.client.renderer.head.UsaHazmatCapModel.LAYER_LOCATION,
                        org.inventory.inventory.client.renderer.head.UsaHazmatCapModel::createBodyLayer
                );
                event.registerLayerDefinition(
                    org.inventory.inventory.client.renderer.vest.VestLifchikModel.LAYER_LOCATION,
                    org.inventory.inventory.client.renderer.vest.VestLifchikModel::createBodyLayer
                );
                event.registerLayerDefinition(
                    org.inventory.inventory.client.renderer.vest.Vest6sh117DesertModel.LAYER_LOCATION,
                    org.inventory.inventory.client.renderer.vest.Vest6sh117DesertModel::createBodyLayer
                );
                event.registerLayerDefinition(
                    org.inventory.inventory.client.renderer.vest.Vest6b2TanModel.LAYER_LOCATION,
                    org.inventory.inventory.client.renderer.vest.Vest6b2TanModel::createBodyLayer
                );
                event.registerLayerDefinition(
                    org.inventory.inventory.client.renderer.vest.VestPlateCarrierDesertModel.LAYER_LOCATION,
                    org.inventory.inventory.client.renderer.vest.VestPlateCarrierDesertModel::createBodyLayer
                );
                event.registerLayerDefinition(
                        org.inventory.inventory.client.renderer.vest.LeopardPressVestModel.LAYER_LOCATION,
                        org.inventory.inventory.client.renderer.vest.LeopardPressVestModel::createBodyLayer
                );
                event.registerLayerDefinition(
                    org.inventory.inventory.client.renderer.vest.DdrBeltModel.LAYER_LOCATION,
                    org.inventory.inventory.client.renderer.vest.DdrBeltModel::createBodyLayer
            );
            event.registerLayerDefinition(
                    org.inventory.inventory.client.renderer.vest.TacticalVestModel.LAYER_LOCATION,
                    org.inventory.inventory.client.renderer.vest.TacticalVestModel::createBodyLayer
            );
        }
    }
}
