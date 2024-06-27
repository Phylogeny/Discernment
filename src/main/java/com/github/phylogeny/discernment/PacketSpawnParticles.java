package com.github.phylogeny.discernment;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.stream.IntStream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record PacketSpawnParticles(Optional<Vec3> optionalHit, int entityId) implements CustomPacketPayload {
    public static final Type<PacketSpawnParticles> TYPE = new Type<>(Discernment.getResourceLoc("fade_screen"));
    public static final Handler HANDLER = new Handler();
    private static final RandomSource RAND = RandomSource.create();

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSpawnParticles> STREAM_CODEC = StreamCodec.composite(
            PacketNetwork.OPTIONAL_VEC3_STREAM_CODEC,
            PacketSpawnParticles::optionalHit,
            ByteBufCodecs.INT,
            PacketSpawnParticles::entityId,
            PacketSpawnParticles::new);

    @Override
    public Type<PacketSpawnParticles> type() {
        return TYPE;
    }

    @ParametersAreNonnullByDefault
    public static class Handler implements IPayloadHandler<PacketSpawnParticles> {
        public void handle(PacketSpawnParticles data, IPayloadContext context) {
            context.enqueueWork(() -> Optional.ofNullable(Minecraft.getInstance().level).ifPresent(world ->
                    Discernment.getRegistryValue(BuiltInRegistries.PARTICLE_TYPE, Config.Server.PARTICLES.getNames(), RAND).ifPresent(type -> {
                        if (type instanceof ParticleOptions particle)
                            data.optionalHit.ifPresentOrElse(hit -> IntStream.range(0, Config.Server.PARTICLES.directCount.get())
                                            .forEach(i -> world.addParticle(particle, hit.x, hit.y, hit.z,
                                                    RAND.nextDouble() - 0.5,
                                                    RAND.nextDouble() - 0.5,
                                                    RAND.nextDouble() - 0.5)),
                                    () -> Optional.ofNullable(world.getEntity(data.entityId)).ifPresent(entity ->
                                            Minecraft.getInstance().particleEngine.createTrackingEmitter(entity, particle)));
                        else
                            Discernment.LOGGER.error(String.format("%s does not implement ParticleOptions", BuiltInRegistries.PARTICLE_TYPE.getKey(type)));
                    })));
        }
    }
}
