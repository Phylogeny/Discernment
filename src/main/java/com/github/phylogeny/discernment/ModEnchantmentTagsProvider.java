package com.github.phylogeny.discernment;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.tags.EnchantmentTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ModEnchantmentTagsProvider extends EnchantmentTagsProvider {
    public ModEnchantmentTagsProvider(PackOutput output,
                                      CompletableFuture<HolderLookup.Provider> lookupProvider,
                                      @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Discernment.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider lookupProvider) {
        tag(EnchantmentTags.NON_TREASURE)
                .add(Discernment.DISCERNMENT_ENCHANT);
    }

    @SubscribeEvent
    public static void gatherData(final GatherDataEvent event) {
        DataGenerator dataGen = event.getGenerator();
        dataGen.addProvider(event.includeServer(),
                new ModEnchantmentTagsProvider(
                        dataGen.getPackOutput(),
                        event.getLookupProvider(),
                        event.getExistingFileHelper()));
    }
}
