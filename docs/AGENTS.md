# AGENTS.md — AI Agent Guide

## Project Overview
Minecraft Forge mod (MC 1.20.1, Forge 47.4.10, Java 17) implementing a DayZ-style expanded inventory system with custom equipment slots, transactional server logic, and data-driven crafting.

**Entry point**: `src/main/java/org/inventory/inventory/Inventory.java` (`@Mod(Inventory.MODID)`)  
**Mod ID**: `inventory` (must match everywhere: class constant, `mods.toml`, `gradle.properties`)

---

## Build & Run
```
./gradlew build            # compile + reobfJar (always finalized)
./gradlew runClient        # launch game client (working dir: run/)
./gradlew runServer        # launch dedicated server (--nogui)
./gradlew runData          # data generation → src/generated/resources/
./gradlew runGameTestServer # run all registered gametests then exit
```
- All version/ID properties live in `gradle.properties` — edit there, not in `build.gradle`.
- JVM max heap is set to 3G (`org.gradle.jvmargs`), daemon is disabled.
- `src/generated/resources/` is auto-included in `sourceSets.main.resources`.
- After changing MCP mappings, re-run the setup task to update the workspace.

---

## Architecture & Domain Model
See `docs/ARCHITECTURE.md` and `docs/ROADMAP.md` for full spec. Key concepts:

| Entity | Role |
|---|---|
| `PlayerLoadout` | Per-player equipment + dynamic storage slots state (capability + NBT) |
| `EquipmentSlotType` | `HEAD, FACE, CHEST, VEST, BACKPACK, GLOVES, LEGS, FEET` |
| `StorageProfile` | Capacity granted by an equipped item (data-driven JSON) |
| `ProtectionProfile` | Armor/durability/weight/tags per item (data-driven JSON) |
| `CraftCategory` / `CraftCard` | Data-driven crafting categories and recipes |

Data files go in `src/main/resources/data/inventory/`:
- `storage_profiles/*.json`, `protection_profiles/*.json`
- `craft_categories/*.json`, `craft_cards/*.json`

---

## Critical Conventions

### Server is sole source of truth
All inventory/craft mutations execute **only on the server**. The client sends requests; the server validates, applies, and syncs back. Never mutate inventory state client-side without server confirmation.

### Transactional operations (5-step pattern)
```
begin (check loadoutVersion, lock player)
  → plan (calculate moves/drops)
  → commit (apply to inventory + world)
  → sync (send S2C_LoadoutSync)
  → unlock
```

### Idempotency on C2S packets
Every `C2S_ClickCustomSlot` and `C2S_RequestCraft` must carry a `requestId`. The server maintains a dedup cache with TTL. Duplicate `requestId` must never cause a second item grant/consume.

### Canonical slot index mapping
Vanilla slots (hotbar/armor/offhand) use **fixed** indices. Dynamic slots use a **separate** index range. Never shift vanilla ranges. Store `slotActive: boolean` per dynamic slot.

### Saga pattern for multi-step operations
Overflow and craft use a Saga orchestrator: each step logs `opId`, failures trigger compensations in reverse order (`releaseReservation → rollbackMove → cancelDropMark`).

### Forge registration pattern
All registry objects use `DeferredRegister` registered to `modEventBus` in the `@Mod` constructor:
```java
public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
// register to bus in constructor:
ITEMS.register(modEventBus);
```

### Config pattern
Use `ForgeConfigSpec.Builder` in `Config.java`. Static fields are populated in `@SubscribeEvent static void onLoad(ModConfigEvent)`.

### Event bus routing
- Mod lifecycle events → `FMLJavaModLoadingContext.get().getModEventBus()`
- Game events (server start, player events) → `MinecraftForge.EVENT_BUS`
- Client-only events → inner static class with `@Mod.EventBusSubscriber(value = Dist.CLIENT)`

---

## Capability & NBT
- `PlayerLoadout` is persisted as a Forge capability with NBT serialization.
- `schemaVersion` field is **mandatory from v1** — migrations run on player load.
- Atomic write protocol: `prepare → write temp → commit marker`. On crash, recover from last successful marker.
- `pendingOverflow` written to NBT for disconnect recovery.

---

## Network Packets (planned)
| Packet | Direction | Key fields |
|---|---|---|
| `C2S_ClickCustomSlot` | Client→Server | `requestId`, `clientViewVersion` |
| `C2S_RequestCraft` | Client→Server | `requestId` |
| `S2C_LoadoutSync` | Server→Client | `serverVersion` |

Sync is debounced 50–200 ms (configurable). Immediate send only for critical commits (overflow, craft completion).

---

## Current State (Roadmap Phase A — not started)
The source tree contains only the Forge MDK scaffold (`Inventory.java`, `Config.java`). All domain classes listed above are **yet to be implemented**. Start with:
1. `schemaVersion` + capability migrator
2. `PlayerLoadout` / `StorageProfile` / `slotActive` + canonical mapping
3. Per-player lock/queue + overflow transaction

Refer to the ordered task list in `docs/ROADMAP.md § 8` for implementation sequencing.

