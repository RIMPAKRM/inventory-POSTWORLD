package org.inventory.inventory.network;

import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import org.inventory.inventory.domain.CraftCard;
import org.inventory.inventory.domain.CraftCardRegistry;
import org.inventory.inventory.domain.CraftCategory;
import org.inventory.inventory.domain.CraftIngredient;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Supplier;

/**
 * S2C packet to sync craft categories and craft cards from server to client.
 * Sent on player join to ensure client has all craft data.
 */
public class S2CCraftSyncPacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    private List<CraftCategory> categories;
    private List<CraftCard> cards;

    public S2CCraftSyncPacket() {
        this.categories = new ArrayList<>();
        this.cards = new ArrayList<>();
    }

    public S2CCraftSyncPacket(Collection<CraftCategory> categories,
                              Collection<CraftCard> cards) {
        this.categories = new ArrayList<>(categories);
        this.cards = new ArrayList<>(cards);
    }

    public void encode(FriendlyByteBuf buf) {
        // Encode categories
        buf.writeInt(this.categories.size());
        for (CraftCategory category : this.categories) {
            buf.writeResourceLocation(category.getId());
            buf.writeUtf(category.getDisplayName());
            buf.writeInt(category.getSortOrder());
        }

        // Encode cards
        buf.writeInt(this.cards.size());
        for (CraftCard card : this.cards) {
            buf.writeResourceLocation(card.getId());
            buf.writeResourceLocation(card.getCategoryId());
            
            // Encode result ItemStack
            buf.writeItem(card.getResult());
            
            // Encode ingredients count and items
            List<CraftIngredient> ingredients = card.getIngredients();
            buf.writeInt(ingredients.size());
            for (CraftIngredient ing : ingredients) {
                // Write ingredient as ItemStack (item + count)
                ItemStack ingStack = new ItemStack(ing.item(), ing.count());
                buf.writeItem(ingStack);
                buf.writeBoolean(ing.tag() != null);
                if (ing.tag() != null) {
                    buf.writeResourceLocation(ing.tag());
                }
            }
        }
    }

    public static S2CCraftSyncPacket decode(FriendlyByteBuf buf) {
        S2CCraftSyncPacket packet = new S2CCraftSyncPacket();

        // Decode categories
        int categoryCount = buf.readInt();
        for (int i = 0; i < categoryCount; i++) {
            ResourceLocation catId = buf.readResourceLocation();
            String displayName = buf.readUtf();
            int sortOrder = buf.readInt();
            packet.categories.add(new CraftCategory(catId, displayName, sortOrder));
        }

        // Decode cards
        int cardCount = buf.readInt();
        for (int i = 0; i < cardCount; i++) {
            ResourceLocation cardId = buf.readResourceLocation();
            ResourceLocation categoryId = buf.readResourceLocation();
            ItemStack result = buf.readItem();
            
            List<CraftIngredient> ingredients = new ArrayList<>();
            int ingredientCount = buf.readInt();
            for (int j = 0; j < ingredientCount; j++) {
                ItemStack ingStack = buf.readItem();
                ResourceLocation tag = null;
                if (buf.readBoolean()) {
                    tag = buf.readResourceLocation();
                }
                if (!ingStack.isEmpty()) {
                    ingredients.add(new CraftIngredient(ingStack.getItem(), ingStack.getCount(), tag));
                }
            }
            
            if (!ingredients.isEmpty() && !result.isEmpty()) {
                packet.cards.add(new CraftCard(cardId, categoryId, ingredients, result));
            }
        }

        return packet;
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                handleOnClient();
            }
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleOnClient() {
        LOGGER.debug("[S2CCraftSync] received {} categories and {} cards", 
                categories.size(), cards.size());
        CraftCardRegistry.replaceSnapshot(categories, cards);
    }
}

