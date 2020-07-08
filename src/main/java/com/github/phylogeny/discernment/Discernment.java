package com.github.phylogeny.discernment;

import com.google.common.base.Stopwatch;
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

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Mod(Discernment.MOD_ID)
@EventBusSubscriber
public class Discernment
{
    public static final String MOD_ID = "discernment";
    private static final DeferredRegister<Enchantment> ENCHANTMENTS = new DeferredRegister<>(ForgeRegistries.ENCHANTMENTS, MOD_ID);
    private static final DeferredRegister<Effect> EFFECTS = new DeferredRegister<>(ForgeRegistries.POTIONS, MOD_ID);
    private static final DeferredRegister<Potion> POTIONS = new DeferredRegister<>(ForgeRegistries.POTION_TYPES, MOD_ID);
    private static final RegistryObject<Enchantment> DISCERNMENT_ENCHANT = ENCHANTMENTS.register("discernment", DiscernmentEnchantment::new);
    private static final RegistryObject<Effect> DISCERNMENT_EFFECT = EFFECTS.register("discernment", DiscernmentEffect::new);
    static
    {
        registerDiscernmentPotion("discernment", 3600);
        registerDiscernmentPotion("long_discernment", 9600);
    }

    public Discernment()
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
            super(Rarity.UNCOMMON, EnchantmentType.ALL, EquipmentSlotType.values());
        }
    }

    private static class DiscernmentEffect extends Effect
    {
        private final Stopwatch timer;
        private static final int CYCLE_TIME = 2000;
        private static final int[] COLORS = IntStream.range(0, CYCLE_TIME).map(i ->
        {
            int value = Math.max(1, (int)((Math.sin((i % CYCLE_TIME) / (double) CYCLE_TIME * 2 * Math.PI) / 2 + 0.5) * 255 + 0.5));
            return (value << 16) | (value << 8) | value;
        }).toArray();

        protected DiscernmentEffect()
        {
            super(EffectType.BENEFICIAL, 0);
            timer = Stopwatch.createStarted();
        }

        @Override
        public int getLiquidColor()
        {
            return COLORS[(int) (timer.elapsed(TimeUnit.MILLISECONDS) % CYCLE_TIME)];
        }
    }
}