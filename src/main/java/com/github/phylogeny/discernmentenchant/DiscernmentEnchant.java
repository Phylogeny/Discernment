package com.github.phylogeny.discernmentenchant;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(DiscernmentEnchant.MOD_ID)
@EventBusSubscriber
public class DiscernmentEnchant
{
    public static final String MOD_ID = "discernmentenchant";
    private static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MOD_ID);
    private static final RegistryObject<Enchantment> DISCERNMENT = ENCHANTMENTS.register("discernment", DiscernmentEnchantment::new);

    public DiscernmentEnchant()
    {
        ENCHANTMENTS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    @SubscribeEvent
    public static void discern(LivingAttackEvent event)
    {
        if (!event.getEntityLiving().getType().getClassification().getPeacefulCreature())
            return;

        Entity source = event.getSource().getTrueSource();
        if (!(source instanceof LivingEntity))
            return;

        if (source != null && !source.isSneaking()
                && DISCERNMENT.get().getEntityEquipment((LivingEntity) source).values().stream()
                    .anyMatch(stack -> EnchantmentHelper.getEnchantmentLevel(DISCERNMENT.get(), stack) > 0))
            event.setCanceled(true);
    }

    private static class DiscernmentEnchantment extends Enchantment
    {
        protected DiscernmentEnchantment()
        {
            super(Rarity.UNCOMMON, EnchantmentType.VANISHABLE, EquipmentSlotType.values());
        }
    }
}