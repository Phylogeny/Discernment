package com.github.phylogeny.discernmentenchant;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.potion.Potion;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.IEventBus;
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
    private static final DeferredRegister<Effect> EFFECTS = DeferredRegister.create(ForgeRegistries.POTIONS, MOD_ID);
    private static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTION_TYPES, MOD_ID);
    private static final RegistryObject<Enchantment> DISCERNMENT_ENCHANT = ENCHANTMENTS.register("discernment", DiscernmentEnchantment::new);
    private static final RegistryObject<Effect> DISCERNMENT_EFFECT = EFFECTS.register("discernment", DiscernmentEffect::new);
    static
    {
        registerDiscernmentPotion("discernment", 3600);
        registerDiscernmentPotion("long_discernment", 9600);
    }

    public DiscernmentEnchant()
    {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENCHANTMENTS.register(bus);
        EFFECTS.register(bus);
        POTIONS.register(bus);
    }

    private static void registerDiscernmentPotion(String name, int duration)
    {
        POTIONS.register(name, () -> new Potion(new EffectInstance(DISCERNMENT_EFFECT.get(), duration)));
    }

    @SubscribeEvent
    public static void discern(LivingAttackEvent event)
    {
        if (!event.getEntityLiving().getType().getClassification().getPeacefulCreature())
            return;

        Entity source = event.getSource().getTrueSource();
        if (!(source instanceof LivingEntity))
            return;

        LivingEntity entity = (LivingEntity) source;
        if (source != null && !source.isSneaking() && (entity.isPotionActive(DISCERNMENT_EFFECT.get())
                || DISCERNMENT_ENCHANT.get().getEntityEquipment(entity).values().stream()
                    .anyMatch(stack -> EnchantmentHelper.getEnchantmentLevel(DISCERNMENT_ENCHANT.get(), stack) > 0)))
            event.setCanceled(true);
    }

    private static class DiscernmentEnchantment extends Enchantment
    {
        protected DiscernmentEnchantment()
        {
            super(Rarity.UNCOMMON, EnchantmentType.VANISHABLE, EquipmentSlotType.values());
        }
    }

    private static class DiscernmentEffect extends Effect
    {
        protected DiscernmentEffect()
        {
            super(EffectType.BENEFICIAL, 0);//TODO color
        }
    }
}