package org.inventory.inventory.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Capability provider that wraps PlayerLoadout and exposes it via the Forge
 * capability system. Attached to every Player entity via AttachCapabilitiesEvent.
 */
public class PlayerLoadoutProvider implements ICapabilitySerializable<CompoundTag> {

    private final PlayerLoadout loadout = new PlayerLoadout();
    private final LazyOptional<IPlayerLoadout> lazyOptional = LazyOptional.of(() -> loadout);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return LoadoutCapability.PLAYER_LOADOUT.orEmpty(cap, lazyOptional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return loadout.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        loadout.deserializeNBT(nbt);
    }

    /** Must be called when the capability provider is invalidated (entity removal). */
    public void invalidate() {
        lazyOptional.invalidate();
    }
}

