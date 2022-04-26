package com.github.phylogeny.discernment;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PacketNetwork {
    private static int packetId = 0;
    private static final SimpleChannel INSTANCE = NetworkRegistry.ChannelBuilder
            .named(Discernment.getResourceLoc("channel_main"))
            .clientAcceptedVersions(version -> true)
            .serverAcceptedVersions(version -> true)
            .networkProtocolVersion(() -> "1.0")
            .simpleChannel();

    public static void registerPackets() {
        // Client side
        registerMessage(PacketSpawnParticles.class, PacketSpawnParticles::encode, PacketSpawnParticles::decode, PacketSpawnParticles.Handler::handle);
    }

    public static <MSG> void sendToAllTrackingAndSelf(MSG msg, Entity entity) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), msg);
    }

    public static <MSG> void sendToAllTracking(MSG msg, Entity entity) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), msg);
    }

    public static <MSG> void sendTo(MSG msg, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static <MSG> void sendToAll(MSG msg)
    {
        INSTANCE.send(PacketDistributor.ALL.noArg(), msg);
    }

    public static <MSG> void sendToAllAround(MSG msg, Level world, BlockPos pos) {
        sendToAllAround(msg, world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public static <MSG> void sendToAllAround(MSG msg, Level world, Vec3 vec) {
        sendToAllAround(msg, world, vec.x, vec.y, vec.z);
    }

    public static <MSG> void sendToAllAround(MSG msg, Level world, double x, double y, double z) {
        INSTANCE.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(x, y, z, 128, world.dimension())), msg);
    }

    public static <MSG> void sendToDimension(MSG msg, ResourceKey<Level> dimension) {
        INSTANCE.send(PacketDistributor.DIMENSION.with(() -> dimension), msg);
    }

    public static <MSG> void sendToServer(MSG msg)
    {
        INSTANCE.send(PacketDistributor.SERVER.noArg(), msg);
    }

    private static <MSG> void registerMessage(Class<MSG> messageType, BiConsumer<MSG, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer) {
        INSTANCE.registerMessage(packetId++, messageType, encoder, decoder, messageConsumer);
    }
}