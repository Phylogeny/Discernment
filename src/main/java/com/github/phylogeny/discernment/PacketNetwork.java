package com.github.phylogeny.discernment;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.util.LogicalSidedProvider;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

@Mod.EventBusSubscriber(modid = Discernment.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PacketNetwork {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlerEvent event) {
        IPayloadRegistrar registrar = event.registrar(Discernment.MOD_ID);

        // Client side
        registrar.play(PacketSpawnParticles.ID, PacketSpawnParticles::new, handler -> handler.client(PacketSpawnParticles::handle));
    }

    public static void reply(CustomPacketPayload msg, PlayPayloadContext context) {
        context.replyHandler().send(msg);
    }

    public static void sendToAllTrackingAndSelf(CustomPacketPayload msg, Entity entity) {
        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(entity).send(msg);
    }

    public static void sendToAllTracking(CustomPacketPayload msg, Entity entity) {
        PacketDistributor.TRACKING_ENTITY.with(entity).send(msg);
    }

    public static void sendTo(CustomPacketPayload msg, ServerPlayer player) {
        PacketDistributor.PLAYER.with(player).send(msg);
    }

    public static void sendToAll(CustomPacketPayload msg)
    {
        PacketDistributor.ALL.noArg().send(msg);
    }

    public static void sendToAllAround(CustomPacketPayload msg, Level world, BlockPos pos) {
        sendToAllAround(msg, world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public static void sendToAllAround(CustomPacketPayload msg, Level world, Vec3 vec) {
        sendToAllAround(msg, world, vec.x, vec.y, vec.z);
    }

    public static void sendToAllAround(CustomPacketPayload msg, Level world, double x, double y, double z) {
        PacketDistributor.NEAR.with(new PacketDistributor.TargetPoint(x, y, z, 128, world.dimension())).send(msg);
    }

    public static void sendToDimension(CustomPacketPayload msg, ResourceKey<Level> dimension) {
        PacketDistributor.DIMENSION.with(dimension).send(msg);
    }

    public static void sendToServer(CustomPacketPayload msg)
    {
        PacketDistributor.SERVER.noArg().send(msg);
    }

    public static void enqueueServerWork(Runnable runnable) {
        BlockableEventLoop<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);
        if (!executor.isSameThread())
            executor.submitAsync(runnable);
        else
            runnable.run();
    }
}
