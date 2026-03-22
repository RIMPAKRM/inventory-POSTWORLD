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
    // Creates a new Block with the id "inventory:example_block", combining the namespace and path
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    // Creates a new BlockItem with the id "inventory:example_block", combining the namespace and path
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));
    // Creates a new food item with the id "inventory:example_id", nutrition 1 and saturation 2
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().alwaysEat().nutrition(1).saturationMod(2f).build())));
    public static final RegistryObject<Item> RUGGED_VEST = ITEMS.register("rugged_vest", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> TACTICAL_VEST = ITEMS.register("tactical_vest", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> TACTICAL_VEST_BLACK = ITEMS.register("tactical_vest_black", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> TRAVEL_BACKPACK = ITEMS.register("travel_backpack", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FIELD_JACKET = ITEMS.register("field_jacket", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> TACTICAL_GLOVES = ITEMS.register("tactical_gloves", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> TACTICAL_BOOTS = ITEMS.register("tactical_boots", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CARGO_PANTS = ITEMS.register("cargo_pants", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PATROL_JACKET = ITEMS.register("patrol_jacket", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CHEST_RIG = ITEMS.register("chest_rig", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> EXPEDITION_PACK = ITEMS.register("expedition_pack", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> REINFORCED_CARGO_PANTS = ITEMS.register("reinforced_cargo_pants", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> WORK_GLOVES = ITEMS.register("work_gloves", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DESERT_CAP = ITEMS.register("desert_cap", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BALACLAVA = ITEMS.register("balaclava", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CAP = ITEMS.register("cap", () -> new Item(new Item.Properties().stacksTo(1)));
    // Creates a creative tab with the id "inventory:example_tab" for the example item, that is placed after the combat tab
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder().withTabsBefore(CreativeModeTabs.COMBAT).icon(() -> EXAMPLE_ITEM.get().getDefaultInstance()).displayItems((parameters, output) -> {
        output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
    }).build());

    // ---- Custom Inventory Menu ----
    // Register the custom inventory menu type
    public static final RegistryObject<MenuType<CustomInventoryMenu>> CUSTOM_INVENTORY_MENU = MENU_TYPES.register("custom_inventory", () -> IForgeMenuType.create(CustomInventoryMenu::new));

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

        // Phase B: register all network packets
        event.enqueueWork(ModNetwork::registerPackets);

        if (Config.logDirtBlock) LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) event.accept(EXAMPLE_BLOCK_ITEM);
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(RUGGED_VEST.get());
            event.accept(TACTICAL_VEST.get());
            event.accept(TACTICAL_VEST_BLACK.get());
            event.accept(TRAVEL_BACKPACK.get());
            event.accept(FIELD_JACKET.get());
            event.accept(TACTICAL_GLOVES.get());
            event.accept(TACTICAL_BOOTS.get());
            event.accept(CARGO_PANTS.get());
            event.accept(PATROL_JACKET.get());
            event.accept(CHEST_RIG.get());
            event.accept(EXPEDITION_PACK.get());
            event.accept(REINFORCED_CARGO_PANTS.get());
            event.accept(WORK_GLOVES.get());
            event.accept(DESERT_CAP.get());
            event.accept(BALACLAVA.get());
            event.accept(CAP.get());
        }
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

            // Phase B: register custom inventory screen for CustomInventoryMenu
            event.enqueueWork(() ->
                net.minecraft.client.gui.screens.MenuScreens.register(
                        CUSTOM_INVENTORY_MENU.get(),
                        org.inventory.inventory.client.screen.InventoryScreen::new)
            );

        }

        @SubscribeEvent
        public static void onRegisterLayerDefinitions(net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(
                    org.inventory.inventory.client.renderer.backpack.BackpackModel.LAYER_LOCATION,
                    org.inventory.inventory.client.renderer.backpack.BackpackModel::createBodyLayer
            );
            event.registerLayerDefinition(
                    org.inventory.inventory.client.renderer.head.CapModel.LAYER_LOCATION,
                    org.inventory.inventory.client.renderer.head.CapModel::createBodyLayer
            );
            event.registerLayerDefinition(
                    org.inventory.inventory.client.renderer.vest.TacticalVestModel.LAYER_LOCATION,
                    org.inventory.inventory.client.renderer.vest.TacticalVestModel::createBodyLayer
            );
        }
    }
}
