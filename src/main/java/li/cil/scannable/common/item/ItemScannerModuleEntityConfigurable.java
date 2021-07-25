package li.cil.scannable.common.item;

import li.cil.scannable.common.config.Constants;
import li.cil.scannable.common.container.EntityModuleContainer;
import li.cil.scannable.common.scanning.ScannerModuleEntityConfigurable;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fmllegacy.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class ItemScannerModuleEntityConfigurable extends AbstractItemScannerModule {
    private static final String TAG_ENTITY_DEPRECATED = "entity";
    private static final String TAG_ENTITIES = "entities";
    private static final String TAG_IS_LOCKED = "isLocked";

    public static boolean isLocked(final ItemStack stack) {
        final CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.getBoolean(TAG_IS_LOCKED);
    }

    public static List<EntityType<?>> getEntityTypes(final ItemStack stack) {
        final CompoundTag nbt = stack.getTag();
        if (nbt == null || !(nbt.contains(TAG_ENTITY_DEPRECATED, NBT.TAG_STRING) || nbt.contains(TAG_ENTITIES, NBT.TAG_LIST))) {
            return Collections.emptyList();
        }

        upgradeData(nbt);

        final ListTag list = nbt.getList(TAG_ENTITIES, NBT.TAG_STRING);
        final List<EntityType<?>> result = new ArrayList<>();
        list.forEach(tag -> {
            final Optional<EntityType<?>> entityType = EntityType.byString(tag.getAsString());
            entityType.ifPresent(result::add);
        });

        return result;
    }

    private static boolean addEntityType(final ItemStack stack, final EntityType<?> entityType) {
        final ResourceLocation registryName = entityType.getRegistryName();
        if (registryName == null) {
            return false;
        }

        final CompoundTag nbt = stack.getOrCreateTag();
        if (nbt.getBoolean(TAG_IS_LOCKED)) {
            return false;
        }

        final StringTag itemNbt = StringTag.valueOf(registryName.toString());

        final ListTag list = nbt.getList(TAG_ENTITIES, NBT.TAG_STRING);
        if (list.contains(itemNbt)) {
            return true;
        }
        if (list.size() >= Constants.CONFIGURABLE_MODULE_SLOTS) {
            return false;
        }

        // getList may have just created a new empty list.
        nbt.put(TAG_ENTITIES, list);

        list.add(itemNbt);
        return true;
    }

    public static void setEntityTypeAt(final ItemStack stack, final int index, final EntityType<?> entityType) {
        if (index < 0 || index >= Constants.CONFIGURABLE_MODULE_SLOTS) {
            return;
        }

        final ResourceLocation registryName = entityType.getRegistryName();
        if (registryName == null) {
            return;
        }

        final CompoundTag nbt = stack.getOrCreateTag();
        if (nbt.getBoolean(TAG_IS_LOCKED)) {
            return;
        }

        final StringTag itemNbt = StringTag.valueOf(registryName.toString());

        final ListTag list = nbt.getList(TAG_ENTITIES, NBT.TAG_STRING);
        final int oldIndex = list.indexOf(itemNbt);
        if (oldIndex == index) {
            return;
        }

        if (index >= list.size()) {
            list.add(itemNbt);
        } else {
            list.set(index, itemNbt);
        }

        if (oldIndex >= 0) {
            list.remove(oldIndex);
        }

    }

    public static void removeEntityTypeAt(final ItemStack stack, final int index) {
        if (index < 0 || index >= Constants.CONFIGURABLE_MODULE_SLOTS) {
            return;
        }

        final CompoundTag nbt = stack.getOrCreateTag();
        if (nbt.getBoolean(TAG_IS_LOCKED)) {
            return;
        }

        final ListTag list = nbt.getList(TAG_ENTITIES, NBT.TAG_STRING);
        if (index < list.size()) {
            list.remove(index);
        }
    }

    private static void upgradeData(final CompoundTag nbt) {
        if (nbt.contains(TAG_ENTITY_DEPRECATED, NBT.TAG_STRING)) {
            final ListTag list = new ListTag();
            list.add(nbt.get(TAG_ENTITY_DEPRECATED));
            nbt.put(TAG_ENTITIES, list);
            nbt.remove(TAG_ENTITY_DEPRECATED);
        }
    }

    // --------------------------------------------------------------------- //

    public ItemScannerModuleEntityConfigurable() {
        super(ScannerModuleEntityConfigurable.INSTANCE);
    }

    // --------------------------------------------------------------------- //
    // Item

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level world, final List<Component> tooltip, final TooltipFlag flag) {
        final List<EntityType<?>> entities = getEntityTypes(stack);
        if (entities.size() == 0) {
            tooltip.add(new TranslatableComponent(Constants.TOOLTIP_MODULE_ENTITY));
        } else {
            tooltip.add(new TranslatableComponent(Constants.TOOLTIP_MODULE_ENTITY_LIST));
            entities.forEach(e -> tooltip.add(new TranslatableComponent(Constants.TOOLTIP_LIST_ITEM_FORMAT, e.getDescription())));
        }
        super.appendHoverText(stack, world, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level world, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openGui(serverPlayer, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return stack.getHoverName();
                    }

                    @Override
                    public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player player) {
                        return new EntityModuleContainer(id, inventory, hand);
                    }
                }, buffer -> buffer.writeEnum(hand));
            }
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(final ItemStack stack, final Player player, final LivingEntity target, final InteractionHand hand) {
        // NOT adding to `stack` parameter, because that's a copy in creative mode.
        if (addEntityType(player.getItemInHand(hand), target.getType())) {
            player.swing(hand);
            player.getInventory().setChanged();
            return InteractionResult.SUCCESS;
        } else {
            if (player.getCommandSenderWorld().isClientSide && !ItemScannerModuleEntityConfigurable.isLocked(stack)) {
                Minecraft.getInstance().gui.getChat().addMessage(new TranslatableComponent(Constants.MESSAGE_NO_FREE_SLOTS), Constants.CHAT_LINE_ID);
            }
            return InteractionResult.SUCCESS; // Prevent opening item UI.
        }
    }
}
