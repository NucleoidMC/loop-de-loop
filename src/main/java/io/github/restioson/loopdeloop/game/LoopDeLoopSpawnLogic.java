package io.github.restioson.loopdeloop.game;

import io.github.restioson.loopdeloop.game.map.LoopDeLoopMap;
import xyz.nucleoid.plasmid.api.game.GameOpenException;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;


public final class LoopDeLoopSpawnLogic {
    private final ServerLevel world;
    private final LoopDeLoopMap map;

    public LoopDeLoopSpawnLogic(ServerLevel world, LoopDeLoopMap map) {
        this.world = world;
        this.map = map;
    }

    public JoinAcceptorResult acceptPlayer(JoinAcceptor offer, GameType gameMode) {
        return offer.teleport(this.world, this.generateSpawn(this.world.getRandom()))
                .thenRunForEach(player -> this.resetPlayer(player, gameMode));
    }

    public void resetPlayer(ServerPlayer player, GameType gameMode) {
        player.setGameMode(gameMode);

        player.addEffect(new MobEffectInstance(
                MobEffects.NIGHT_VISION,
                MobEffectInstance.INFINITE_DURATION,
                1,
                true,
                false
        ));
    }

    public void spawnPlayer(ServerPlayer player) {
        var spawn = this.generateSpawn(player.getRandom());

        player.teleportTo(this.world, spawn.x, spawn.y, spawn.z, Set.of(), 0.0F, 0.0F, false);
    }

    private Vec3 generateSpawn(RandomSource random) {
        BlockPos spawn = this.map.getSpawn();
        if (spawn == null) {
            throw new GameOpenException(Component.literal("Cannot spawn player! No spawn defined in map!"));
        }

        float radius = 2.5f;
        double x = spawn.getX() + Mth.nextDouble(random, -radius, radius);
        double z = spawn.getZ() + 1 + Mth.nextDouble(random, -radius, radius);
        return new Vec3(x, spawn.getY(), z);
    }
}
