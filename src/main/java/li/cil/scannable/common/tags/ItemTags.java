package li.cil.scannable.common.tags;

import li.cil.scannable.api.API;
import net.minecraft.world.item.Item;
import net.minecraft.tags.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.Tags;

public final class ItemTags {
    public static final Tag.Named<Item> MODULES = tag("modules");

    // --------------------------------------------------------------------- //

    public static void initialize() {
    }

    // --------------------------------------------------------------------- //

    private static Tags.IOptionalNamedTag<Item> tag(final String name) {
        return net.minecraft.tags.ItemTags.createOptional(new ResourceLocation(API.MOD_ID, name));
    }
}
