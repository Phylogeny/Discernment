package com.github.phylogeny.discernment;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.IntStream;

@MethodsReturnNonnullByDefault
public record PacketSpawnParticles(@Nullable Vec3 hit, int entityId) implements CustomPacketPayload {
    public static final ResourceLocation ID = Discernment.getResourceLoc("fade_screen");
    private static final RandomSource RAND = RandomSource.create();

    public PacketSpawnParticles(FriendlyByteBuf buffer) {
        this(buffer.readBoolean()
                ? new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())
                : null,
                buffer.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(hit != null);
        if (hit != null) {
            buffer.writeDouble(hit.x);
            buffer.writeDouble(hit.y);
            buffer.writeDouble(hit.z);
        }
        buffer.writeInt(entityId);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public static void handle(PacketSpawnParticles data, PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> Optional.ofNullable(Minecraft.getInstance().level).ifPresent(world ->
                Discernment.getRegistryValue(BuiltInRegistries.PARTICLE_TYPE, Config.Server.PARTICLES.getNames(), RAND).ifPresent(type -> {
                    if (type instanceof ParticleOptions particle)
                        if (data.hit == null)
                            Optional.ofNullable(world.getEntity(data.entityId)).ifPresent(entity ->
                                    Minecraft.getInstance().particleEngine.createTrackingEmitter(entity, particle));
                        else
                            IntStream.range(0, Config.Server.PARTICLES.directCount.get()).forEach(i -> world.addParticle(particle, data.hit.x, data.hit.y, data.hit.z,
                                    RAND.nextDouble() - 0.5, RAND.nextDouble() - 0.5, RAND.nextDouble() - 0.5));
                    else
                        Discernment.LOGGER.error(String.format("%s does not implement ParticleOptions", BuiltInRegistries.PARTICLE_TYPE.getKey(type)));
                })));
    }
}
