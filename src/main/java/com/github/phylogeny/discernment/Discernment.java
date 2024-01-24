package com.github.phylogeny.discernment;

import com.google.common.base.Stopwatch;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.brewing.BrewingRecipeRegistry;
import net.neoforged.neoforge.event.entity.living.LivingAttackEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.IntStream;

@Mod(Discernment.MOD_ID)
@Mod.EventBusSubscriber
public class Discernment {
    public static final String MOD_ID = "discernment";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(Registries.ENCHANTMENT, MOD_ID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MOD_ID);
    private static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, MOD_ID);
    private static final Supplier<Enchantment> DISCERNMENT_ENCHANT = ENCHANTMENTS.register("discernment", DiscernmentEnchantment::new);
    private static final Supplier<MobEffect> DISCERNMENT_EFFECT = EFFECTS.register("discernment", DiscernmentEffect::new);
    private static final Supplier<Potion> DISCERNMENT_POTION = registerDiscernmentPotion("discernment", 3600);
    private static final Supplier<Potion> DISCERNMENT_POTION_LONG = registerDiscernmentPotion("long_discernment", 9600);

    public Discernment(IEventBus bus) {
        Config.register(bus);
        ENCHANTMENTS.register(bus);
        EFFECTS.register(bus);
        POTIONS.register(bus);
        bus.addListener(this::setup);
    }

    public static ResourceLocation getResourceLoc(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    private void setup(FMLCommonSetupEvent event) {
        addBrewingRecipe(Potions.AWKWARD, Items.EMERALD, DISCERNMENT_POTION);
        addBrewingRecipe(DISCERNMENT_POTION.get(), Items.REDSTONE, DISCERNMENT_POTION_LONG);
    }

    private void addBrewingRecipe(Potion input, Item ingredient, Supplier<Potion> output) {
        BrewingRecipeRegistry.addRecipe(Ingredient.of(getPotionStack(input)), Ingredient.of(ingredient), getPotionStack(output.get()));
    }

    private ItemStack getPotionStack(Potion potion) {
        return PotionUtils.setPotion(new ItemStack(Items.POTION), potion);
    }

    private static Supplier<Potion> registerDiscernmentPotion(String name, int duration) {
        return POTIONS.register(name, () -> new Potion(new MobEffectInstance(DISCERNMENT_EFFECT.get(), duration)));
    }

    @SubscribeEvent
    public static void discern(LivingAttackEvent event) {
        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity attacker)
                || attacker.isShiftKeyDown()
                || (!attacker.hasEffect(DISCERNMENT_EFFECT.get())
                        && DISCERNMENT_ENCHANT.get().getSlotItems(attacker).values().stream()
                            .noneMatch(stack -> stack.getEnchantmentLevel(DISCERNMENT_ENCHANT.get()) > 0)))
            return;

        LivingEntity target = event.getEntity();
        if ((Config.Server.FUNCTIONALITY.protection.namedEntityBlacklist.useNamePattern()
                && Optional.ofNullable(target.getCustomName())
                    .filter(Config.Server.FUNCTIONALITY.protection.namedEntityBlacklist::namePatternMatches)
                    .isPresent())
                || ((target instanceof Player
                        ? !Config.Server.FUNCTIONALITY.protection.protectPlayers.get()
                        : (!Config.Server.FUNCTIONALITY.protection.protectPeacefulMobs.get() || !target.getType().getCategory().isFriendly()))
                    && (!(target instanceof OwnableEntity ownable) || !attacker.getUUID().equals(ownable.getOwnerUUID()))
                    && (!Config.Server.FUNCTIONALITY.protection.namedEntityWhitelist.useNamePattern()
                        || Optional.ofNullable(target.getCustomName())
                            .filter(Config.Server.FUNCTIONALITY.protection.namedEntityWhitelist::namePatternMatches)
                            .isEmpty())))
            return;

        event.setCanceled(true);
        if (target.level().isClientSide())
            return;

        if (Config.Server.SOUNDS.enabled.get()) {
            getRegistryValue(BuiltInRegistries.SOUND_EVENT, Config.Server.SOUNDS.getNames(), target.getRandom()).ifPresent(sound ->
                    target.level().playSound(null, target.getX(), target.getY(), target.getZ(), sound, target.getSoundSource(),
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

    @NotNull
    public static <T> Optional<T> getRegistryValue(Registry<T> registry, List<ResourceLocation> names, RandomSource rand) {
        return getRegistryValue(registry, names.get(rand.nextInt(names.size())));
    }

    @NotNull
    public static <T> Optional<T> getRegistryValue(Registry<T> registry, ResourceLocation name) {
        T value = registry.get(name);
        if (value == null) {
            LOGGER.error(String.format("%s is not a registered %s", name, registry.asLookup().key().location()));
            return Optional.empty();
        }
        return Optional.of(value);
    }

    @ParametersAreNonnullByDefault
    private static class DiscernmentEnchantment extends Enchantment {
        protected DiscernmentEnchantment() {
            super(Rarity.UNCOMMON, EnchantmentCategory.VANISHABLE, EquipmentSlot.values());
        }

        @Override
        public boolean canApplyAtEnchantingTable(ItemStack stack) {
            return Config.Server.FUNCTIONALITY.availableInEnchantingTable.get()
                    && super.canApplyAtEnchantingTable(stack);
        }
    }

    private static class DiscernmentEffect extends MobEffect {
        private final Stopwatch timer;
        private static final int CYCLE_TIME = 2000;
        private static final int[] COLORS = IntStream.range(0, CYCLE_TIME).map(i -> {
            int value = Math.max(1, (int)((Math.sin((i % CYCLE_TIME) / (double) CYCLE_TIME * 2 * Math.PI) / 2 + 0.5) * 255 + 0.5));
            return (value << 16) | (value << 8) | value;
        }).toArray();

        protected DiscernmentEffect() {
            super(MobEffectCategory.BENEFICIAL, 0);
            timer = Stopwatch.createStarted();
        }

        @Override
        public int getColor() {
            return COLORS[(int) (timer.elapsed(TimeUnit.MILLISECONDS) % CYCLE_TIME)];
        }
    }
}