package io.github.restioson.loopdeloop.game;

import io.github.restioson.loopdeloop.LoopDeLoop;
import xyz.nucleoid.plasmid.api.game.stats.StatisticMap;

import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class LoopDeLoopPlayer {
    public final ServerPlayer player;

    public int lastHoop = -1;
    public Vec3 lastPos;
    public long lastFailOrSuccess = -1;
    public int previousFails = -1;

    public int totalHoops;
    public int missedHoops;

    public int boostUsed;

    public LoopDeLoopPlayer(ServerPlayer player) {
        this.player = player;
        this.lastPos = player.position();
    }

    public void teleport(double x, double y, double z) {
        ServerLevel world = this.player.level();
        this.player.teleportTo(world, x, y, z, Set.of(), 0.0F, 0.0F, false);
        this.lastPos = new Vec3(x, y, z);
    }

    public void applyTo(StatisticMap statistics) {
        statistics.set(LoopDeLoop.TOTAL_HOOPS, this.totalHoops);
        statistics.set(LoopDeLoop.MISSED_HOOPS, this.missedHoops);
        statistics.set(LoopDeLoop.BOOSTS_USED, this.boostUsed);

    }
}
