package com.github.phylogeny.discernment;

import com.google.common.base.Stopwatch;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Mod(Discernment.MOD_ID)
@Mod.EventBusSubscriber
public class Discernment
{
    public static final String MOD_ID = "discernment";
    public static final Logger LOGGER = LogManager.getLogger();

    private static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MOD_ID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MOD_ID);
    private static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, MOD_ID);
    private static final RegistryObject<Enchantment> DISCERNMENT_ENCHANT = ENCHANTMENTS.register("discernment", DiscernmentEnchantment::new);
    private static final RegistryObject<MobEffect> DISCERNMENT_EFFECT = EFFECTS.register("discernment", DiscernmentEffect::new);
    private static final RegistryObject<Potion> DISCERNMENT_POTION = registerDiscernmentPotion("discernment", 3600);
    private static final RegistryObject<Potion> DISCERNMENT_POTION_LONG = registerDiscernmentPotion("long_discernment", 9600);

    public Discernment()
    {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        Config.register(bus);
        ENCHANTMENTS.register(bus);
        EFFECTS.register(bus);
        POTIONS.register(bus);
        bus.addListener(this::setup);
    }

    public static ResourceLocation getResourceLoc(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    private void setup(FMLCommonSetupEvent event)
    {
        PacketNetwork.registerPackets();
        addBrewingRecipe(Potions.AWKWARD, Items.EMERALD, DISCERNMENT_POTION);
        addBrewingRecipe(DISCERNMENT_POTION.get(), Items.REDSTONE, DISCERNMENT_POTION_LONG);
    }

    private void addBrewingRecipe(Potion input, Item ingredient, RegistryObject<Potion> output)
    {
        BrewingRecipeRegistry.addRecipe(Ingredient.of(getPotionStack(input)), Ingredient.of(ingredient), getPotionStack(output.get()));
    }

    private ItemStack getPotionStack(Potion potion)
    {
        return PotionUtils.setPotion(new ItemStack(Items.POTION), potion);
    }

    private static RegistryObject<Potion> registerDiscernmentPotion(String name, int duration)
    {
        return POTIONS.register(name, () -> new Potion(new MobEffectInstance(DISCERNMENT_EFFECT.get(), duration)));
    }

    @SubscribeEvent
    public static void discern(LivingAttackEvent event)
    {
        LivingEntity target = event.getEntityLiving();
        if (!target.getType().getCategory().isFriendly())
            return;

        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity attacker) || attacker.isShiftKeyDown() || (!attacker.hasEffect(DISCERNMENT_EFFECT.get())
                && DISCERNMENT_ENCHANT.get().getSlotItems(attacker).values().stream()
                    .noneMatch(stack -> EnchantmentHelper.getItemEnchantmentLevel(DISCERNMENT_ENCHANT.get(), stack) > 0)))
            return;

        event.setCanceled(true);
        if (target.level.isClientSide())
            return;

        if (Config.Server.SOUNDS.enabled.get()) {
            getRegistryValue(ForgeRegistries.SOUND_EVENTS, Config.Server.SOUNDS.getNames(), target.getRandom()).ifPresent(sound ->
                    target.level.playSound(null, target.getX(), target.getY(), target.getZ(), sound, target.getSoundSource(),
                            Config.Server.SOUNDS.volume.randomInRange(), Config.Server.SOUNDS.pitch.randomInRange()));
        }
        if (!Config.Server.PARTICLES.enabled.get())
            return;

        if (attacker != event.getSource().getDirectEntity()) {
            PacketNetwork.sendToAllTrackingAndSelf(new PacketSpawnParticles(null, target.getId()), target);
            return;
        }
        Vec3 eyes = attacker.getEyePosition();
        float distance = attacker.distanceTo(target) + attacker.getBbWidth() * 2 + target.getBbWidth() * 2;
        Vec3 look = attacker.getLookAngle().scale(distance);
        Vec3 forward = eyes.add(look);
        AABB box = attacker.getBoundingBox().minmax(target.getBoundingBox()).inflate(target.getPickRadius());
        EntityHitResult result = ProjectileUtil.getEntityHitResult(attacker, eyes, forward, box,
                entity -> entity.getUUID().equals(target.getUUID()), distance * distance);
        PacketNetwork.sendToAllTrackingAndSelf(new PacketSpawnParticles(result == null ? null : result.getLocation(), target.getId()), target);
    }

    public static <T extends IForgeRegistryEntry<T>> Optional<T> getRegistryValue(IForgeRegistry<T> registry, List<ResourceLocation> names, Random rand) {
        return getRegistryValue(registry, names.get(rand.nextInt(names.size())));
    }

    public static <T extends IForgeRegistryEntry<T>> Optional<T> getRegistryValue(IForgeRegistry<T> registry, ResourceLocation name) {
        T value = registry.getValue(name);
        if (value == null) {
            LOGGER.error(String.format("%s is not a registered %s", name, registry.getRegistryName()));
            return Optional.empty();
        }
        return Optional.of(value);
    }

    private static class DiscernmentEnchantment extends Enchantment
    {
        protected DiscernmentEnchantment()
        {
            super(Rarity.UNCOMMON, EnchantmentCategory.VANISHABLE, EquipmentSlot.values());
        }
    }

    private static class DiscernmentEffect extends MobEffect
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
            super(MobEffectCategory.BENEFICIAL, 0);
            timer = Stopwatch.createStarted();
        }

        @Override
        public int getColor()
        {
            return COLORS[(int) (timer.elapsed(TimeUnit.MILLISECONDS) % CYCLE_TIME)];
        }
    }
}