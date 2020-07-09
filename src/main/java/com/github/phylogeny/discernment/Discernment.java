package com.github.phylogeny.discernment;

import com.google.common.base.Stopwatch;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.potion.*;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
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
    private static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MOD_ID);
    private static final DeferredRegister<Effect> EFFECTS = DeferredRegister.create(ForgeRegistries.POTIONS, MOD_ID);
    private static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTION_TYPES, MOD_ID);
    private static final RegistryObject<Enchantment> DISCERNMENT_ENCHANT = ENCHANTMENTS.register("discernment", DiscernmentEnchantment::new);
    private static final RegistryObject<Effect> DISCERNMENT_EFFECT = EFFECTS.register("discernment", DiscernmentEffect::new);
    private static final RegistryObject<Potion> DISCERNMENT_POTION = registerDiscernmentPotion("discernment", 3600);
    private static final RegistryObject<Potion> DISCERNMENT_POTION_LONG = registerDiscernmentPotion("long_discernment", 9600);

    public Discernment()
    {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENCHANTMENTS.register(bus);
        EFFECTS.register(bus);
        POTIONS.register(bus);
        bus.addListener(this::setup);
    }

    private void setup(FMLCommonSetupEvent event)
    {
        DeferredWorkQueue.runLater(() ->
        {
            addBrewingRecipe(Potions.AWKWARD, Items.EMERALD, DISCERNMENT_POTION);
            addBrewingRecipe(DISCERNMENT_POTION.get(), Items.REDSTONE, DISCERNMENT_POTION_LONG);
        });
    }

    private void addBrewingRecipe(Potion input, Item ingredient, RegistryObject<Potion> output)
    {
        BrewingRecipeRegistry.addRecipe(Ingredient.fromStacks(getPotionStack(input)), Ingredient.fromItems(ingredient), getPotionStack(output.get()));
    }

    private ItemStack getPotionStack(Potion potion)
    {
        return PotionUtils.addPotionToItemStack(new ItemStack(Items.POTION), potion);
    }

    private static RegistryObject<Potion> registerDiscernmentPotion(String name, int duration)
    {
        return POTIONS.register(name, () -> new Potion(new EffectInstance(DISCERNMENT_EFFECT.get(), duration)));
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