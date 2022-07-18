package io.github.restioson.loopdeloop.game;

import io.github.restioson.loopdeloop.game.map.LoopDeLoopMap;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameOpenException;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;

public final class LoopDeLoopSpawnLogic {
    private final ServerWorld world;
    private final LoopDeLoopMap map;

    public LoopDeLoopSpawnLogic(ServerWorld world, LoopDeLoopMap map) {
        this.world = world;
        this.map = map;
    }

    public PlayerOfferResult acceptPlayer(PlayerOffer offer, GameMode gameMode) {
        var player = offer.player();
        var spawn = this.generateSpawn(player);
        return offer.accept(this.world, spawn)
                .and(() -> this.resetPlayer(player, gameMode));
    }

    public void resetPlayer(ServerPlayerEntity player, GameMode gameMode) {
        player.changeGameMode(gameMode);

        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.NIGHT_VISION,
                20 * 60 * 60,
                1,
                true,
                false
        ));
    }

    public void spawnPlayer(ServerPlayerEntity player) {
        var spawn = this.generateSpawn(player);

        player.teleport(this.world, spawn.x, spawn.y, spawn.z, 0.0F, 0.0F);
    }

    private Vec3d generateSpawn(ServerPlayerEntity player) {
        BlockPos spawn = this.map.getSpawn();
        if (spawn == null) {
            throw new GameOpenException(Text.literal("Cannot spawn player! No spawn defined in map!"));
        }

        float radius = 4.5f;
        double x = spawn.getX() + MathHelper.nextDouble(player.getRandom(), -radius, radius);
        double z = spawn.getZ() + MathHelper.nextFloat(player.getRandom(), -radius, radius);
        return new Vec3d(x, spawn.getY(), z);
    }
}
