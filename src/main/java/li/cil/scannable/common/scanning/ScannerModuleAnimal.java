package li.cil.scannable.common.scanning;

import li.cil.scannable.api.scanning.ScanFilterEntity;
import li.cil.scannable.api.scanning.ScanResultProvider;
import li.cil.scannable.api.scanning.ScannerModuleEntity;
import li.cil.scannable.client.scanning.ScanResultProviders;
import li.cil.scannable.client.scanning.filter.ScanFilterEntityAnimal;
import li.cil.scannable.common.config.Settings;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;

public enum ScannerModuleAnimal implements ScannerModuleEntity {
    INSTANCE;

    @Override
    public int getEnergyCost(final Player player, final ItemStack module) {
        return Settings.energyCostModuleAnimal;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public ScanResultProvider getResultProvider() {
        return ScanResultProviders.ENTITIES.get();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Optional<ScanFilterEntity> getFilter(final ItemStack module) {
        return Optional.of(ScanFilterEntityAnimal.INSTANCE);
    }
}
