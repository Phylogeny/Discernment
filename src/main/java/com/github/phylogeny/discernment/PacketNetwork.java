package com.github.phylogeny.discernment;

import io.netty.buffer.ByteBuf;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.LogicalSidedProvider;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import javax.annotation.Nullable;
import java.util.Optional;

@EventBusSubscriber(modid = Discernment.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class PacketNetwork {
    @MethodsReturnNonnullByDefault
    public static final StreamCodec<ByteBuf, Optional<Vec3>> OPTIONAL_VEC3_STREAM_CODEC = new StreamCodec<>() {
        @Override
        public Optional<Vec3> decode(ByteBuf buffer) {
            return buffer.readBoolean()
                    ? Optional.of(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()))
                    : Optional.empty();
        }

        @Override
        public void encode(ByteBuf buffer, Optional<Vec3> optionalVec) {
            buffer.writeBoolean(optionalVec.isPresent());
            optionalVec.ifPresent(vec -> {
                buffer.writeDouble(vec.x);
                buffer.writeDouble(vec.y);
                buffer.writeDouble(vec.z);
            });
        }
    };

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Discernment.MOD_ID);

        // Client side
        registrar.playToClient(PacketSpawnParticles.TYPE, PacketSpawnParticles.STREAM_CODEC, PacketSpawnParticles.HANDLER);
    }

    public static void sendToAllAround(CustomPacketPayload msg, ServerLevel level, BlockPos pos) {
        sendToAllAround(msg, level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public static void sendToAllAround(CustomPacketPayload msg, ServerLevel level, Vec3 vec) {
        sendToAllAround(msg, level, vec.x, vec.y, vec.z);
    }

    public static void sendToAllAround(CustomPacketPayload msg, ServerLevel level, double x, double y, double z) {
        sendToAllAround(msg, level, null, x, y, z);
    }

    public static void sendToAllAround(CustomPacketPayload msg, ServerLevel level, @Nullable ServerPlayer excludedPlayer, double x, double y, double z) {
        PacketDistributor.sendToPlayersNear(level, excludedPlayer, x, y, z, 128, msg);
    }

    public static void enqueueServerWork(Runnable runnable) {
        LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER).executeBlocking(runnable);
    }
}
