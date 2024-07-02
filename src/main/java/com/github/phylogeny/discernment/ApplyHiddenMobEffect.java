package com.github.phylogeny.discernment;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record ApplyHiddenMobEffect(
        HolderSet<MobEffect> toApply, LevelBasedValue minDuration, LevelBasedValue maxDuration, LevelBasedValue minAmplifier, LevelBasedValue maxAmplifier
) implements EnchantmentEntityEffect {
    public static final MapCodec<ApplyHiddenMobEffect> CODEC = RecordCodecBuilder.mapCodec(
            p_346379_ -> p_346379_.group(
                            RegistryCodecs.homogeneousList(Registries.MOB_EFFECT).fieldOf("to_apply").forGetter(ApplyHiddenMobEffect::toApply),
                            LevelBasedValue.CODEC.fieldOf("min_duration").forGetter(ApplyHiddenMobEffect::minDuration),
                            LevelBasedValue.CODEC.fieldOf("max_duration").forGetter(ApplyHiddenMobEffect::maxDuration),
                            LevelBasedValue.CODEC.fieldOf("min_amplifier").forGetter(ApplyHiddenMobEffect::minAmplifier),
                            LevelBasedValue.CODEC.fieldOf("max_amplifier").forGetter(ApplyHiddenMobEffect::maxAmplifier)
                    )
                    .apply(p_346379_, ApplyHiddenMobEffect::new)
    );

    @Override
    public void apply(ServerLevel level, int p_346112_, EnchantedItemInUse p_344766_, Entity target, Vec3 p_345315_) {
        if (target instanceof LivingEntity livingentity) {
            RandomSource randomsource = livingentity.getRandom();
            Optional<Holder<MobEffect>> optional = this.toApply.getRandomElement(randomsource);
            if (optional.isPresent()) {
                int i = Math.round(Mth.randomBetween(randomsource, this.minDuration.calculate(p_346112_), this.maxDuration.calculate(p_346112_)) * 20.0F);
                int j = Math.max(0, Math.round(Mth.randomBetween(randomsource, this.minAmplifier.calculate(p_346112_), this.maxAmplifier.calculate(p_346112_))));
                livingentity.addEffect(new MobEffectInstance(optional.get(), i, j, false, false));
            }
        }
    }

    @Override
    public MapCodec<ApplyHiddenMobEffect> codec() {
        return CODEC;
    }
}