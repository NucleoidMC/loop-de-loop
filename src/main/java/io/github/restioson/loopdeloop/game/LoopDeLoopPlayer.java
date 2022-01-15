package io.github.restioson.loopdeloop.game;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class LoopDeLoopPlayer {
    public final ServerPlayerEntity player;

    public int lastHoop = -1;
    public Vec3d lastPos;
    public long lastFailOrSuccess = -1;
    public int previousFails = -1;

    public int totalHoops;
    public int missedHoops;

    public LoopDeLoopPlayer(ServerPlayerEntity player) {
        this.player = player;
    }

    public void teleport(double x, double y, double z) {
        ServerWorld world = (ServerWorld) this.player.world;
        this.player.teleport(world, x, y, z, 0.0F, 0.0F);
        this.lastPos = new Vec3d(x, y, z);
    }
}
