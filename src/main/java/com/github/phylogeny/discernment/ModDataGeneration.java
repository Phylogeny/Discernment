package com.github.phylogeny.discernment;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.ApplyMobEffect;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ModDataGeneration {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator dataGen = event.getGenerator();
        PackOutput packOutput = dataGen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        ModEnchantmentsProvider enchantments = new ModEnchantmentsProvider(
                packOutput,
                lookupProvider);
        dataGen.addProvider(event.includeServer(), enchantments);
        dataGen.addProvider(event.includeServer(),
                new ModEnchantmentTagsProvider(
                        packOutput,
                        enchantments.getRegistryProvider(),
                        event.getExistingFileHelper()));
    }

    private static class ModEnchantmentsProvider extends DatapackBuiltinEntriesProvider {
        public ModEnchantmentsProvider(PackOutput output,
                                       CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(output, lookupProvider, new RegistrySetBuilder()
                    .add(Registries.ENCHANTMENT, context -> {
                        context.register(Discernment.DISCERNMENT_ENCHANT,
                                Enchantment.enchantment(
                                                Enchantment.definition(
                                                        context.lookup(Registries.ITEM).getOrThrow(Tags.Items.ENCHANTABLES),
                                                        2,
                                                        1,
                                                        Enchantment.constantCost(1),
                                                        Enchantment.constantCost(41),
                                                        4,
                                                        EquipmentSlotGroup.ANY)
                                        )
                                        .withEffect(
                                                EnchantmentEffectComponents.TICK,
                                                new ApplyMobEffect(
                                                        HolderSet.direct(Discernment.DISCERNMENT_EFFECT),
                                                        LevelBasedValue.constant(1.5F),
                                                        LevelBasedValue.constant(1.5F),
                                                        LevelBasedValue.constant(0.0F),
                                                        LevelBasedValue.constant(0.0F)
                                                )
                                        )
                                        .build(Discernment.DISCERNMENT_ENCHANT.location()));

                    }),
                    Set.of(Discernment.MOD_ID));
        }
    }

    @ParametersAreNonnullByDefault
    private static class ModEnchantmentTagsProvider extends EnchantmentTagsProvider {
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
    }
}
