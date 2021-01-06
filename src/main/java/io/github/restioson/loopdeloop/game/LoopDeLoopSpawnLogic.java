package io.github.restioson.loopdeloop.game;

import io.github.restioson.loopdeloop.LoopDeLoop;
import io.github.restioson.loopdeloop.game.map.LoopDeLoopMap;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameSpace;

public final class LoopDeLoopSpawnLogic {
    private final GameSpace gameSpace;
    private final LoopDeLoopMap map;

    public LoopDeLoopSpawnLogic(GameSpace gameSpace, LoopDeLoopMap map) {
        this.gameSpace = gameSpace;
        this.map = map;
    }

    public void resetPlayer(ServerPlayerEntity player, GameMode gameMode) {
        player.setGameMode(gameMode);

        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.NIGHT_VISION,
                20 * 60 * 60,
                1,
                true,
                false
        ));
    }

    public void spawnPlayer(ServerPlayerEntity player) {
        ServerWorld world = this.gameSpace.getWorld();

        BlockPos pos = this.map.getSpawn();
        if (pos == null) {
            LoopDeLoop.LOGGER.warn("Cannot spawn player! No spawn is defined in the map!");
            return;
        }

        float radius = 4.5f;
        double x = pos.getX() + MathHelper.nextDouble(player.getRandom(), -radius, radius);
        double z = pos.getZ() + MathHelper.nextFloat(player.getRandom(), -radius, radius);

        player.teleport(world, x, pos.getY(), z, 0.0F, 0.0F);
    }
}
