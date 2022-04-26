package com.github.phylogeny.discernment;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public record PacketSpawnParticles(@Nullable Vec3 hit, int entityId) {
    private static final Random RAND = new Random();

    public static void encode(PacketSpawnParticles msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.hit != null);
        if (msg.hit != null) {
            buf.writeDouble(msg.hit.x);
            buf.writeDouble(msg.hit.y);
            buf.writeDouble(msg.hit.z);
        }
        buf.writeInt(msg.entityId);
    }

    public static PacketSpawnParticles decode(FriendlyByteBuf buf) {
        Vec3 hit = buf.readBoolean() ? new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()) : null;
        return new PacketSpawnParticles(hit, buf.readInt());
    }

    public static class Handler {
        public static void handle(PacketSpawnParticles msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> Optional.ofNullable(Minecraft.getInstance().level).ifPresent(world ->
                    Discernment.getRegistryValue(ForgeRegistries.PARTICLE_TYPES, Config.Server.PARTICLES.getNames(), RAND).ifPresent(type -> {
                        if (type instanceof ParticleOptions particle)
                            if (msg.hit == null)
                                Optional.ofNullable(world.getEntity(msg.entityId)).ifPresent(entity ->
                                        Minecraft.getInstance().particleEngine.createTrackingEmitter(entity, particle));
                            else
                                IntStream.range(0, Config.Server.PARTICLES.directCount.get()).forEach(i -> world.addParticle(particle, msg.hit.x, msg.hit.y, msg.hit.z,
                                        RAND.nextDouble() - 0.5, RAND.nextDouble() - 0.5, RAND.nextDouble() - 0.5));
                        else
                            Discernment.LOGGER.error(String.format("%s does not implement ParticleOptions", type.getRegistryName()));
                    })));
            ctx.get().setPacketHandled(true);
        }
    }
}