package io.github.restioson.loopdeloop.game.map;

import io.github.restioson.loopdeloop.game.LoopDeLoopConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import xyz.nucleoid.plasmid.map.template.MapTemplate;

import java.util.Random;

public final class LoopDeLoopGenerator {
    public final LoopDeLoopConfig config;

    public LoopDeLoopGenerator(LoopDeLoopConfig config) {
        this.config = config;
    }

    public LoopDeLoopMap build() {
        MapTemplate template = MapTemplate.createEmpty();
        LoopDeLoopMap map = new LoopDeLoopMap(template);
        LoopDeLoopConfig cfg = this.config;

        this.spawnPlatform(template);

        BlockPos.Mutable circlePos = new BlockPos.Mutable();
        circlePos.set(0, 128, 32);
        Random random = new Random();

        // y = mx + c  -- these are gradient values
        double mZVarMax = (cfg.zVarMax.end - cfg.zVarMax.start) / (double) cfg.loops;
        double mZVarMin = (cfg.zVarMin.end - cfg.zVarMin.start) / (double) cfg.loops;

        for (int i = 0; i < cfg.loops; i++) {
            this.addCircle(template, cfg.loopRadius, circlePos.toImmutable(), map);

            // New circle
            int zVarMax = MathHelper.ceil(mZVarMax * i + cfg.zVarMax.start);
            int zVarMin = MathHelper.ceil(mZVarMin * i + cfg.zVarMin.start);
            int zMove = MathHelper.nextInt(random, zVarMax, zVarMin);
            int yVar = cfg.yVarMax / 2;
            int y = MathHelper.nextInt(random, 128 - yVar, 128 + yVar);
            int xMove = MathHelper.nextInt(random, -16, 16);
            circlePos.move(Direction.SOUTH, zMove);
            circlePos.move(Direction.EAST, xMove);
            circlePos.setY(y);
        }

        map.setSpawn(new BlockPos(0, 123, 0));

        return map;
    }

    private void spawnPlatform(MapTemplate template) {
        BlockPos min = new BlockPos(-5, 122, -5);
        BlockPos max = new BlockPos(5, 122, 5);

        for (BlockPos pos : BlockPos.iterate(min, max)) {
            template.setBlockState(pos, Blocks.RED_TERRACOTTA.getDefaultState());
        }
    }

    private void addCircle(MapTemplate template, int radius, BlockPos centre, LoopDeLoopMap map) {
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        map.addHoop(new LoopDeLoopHoop(centre, radius));

        int radius2 = radius * radius;
        int outlineRadius2 = (radius - 1) * (radius - 1);

        BlockState outline = Blocks.BLUE_TERRACOTTA.getDefaultState();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                int distance2 = x * x + y * y;
                if (distance2 >= radius2) {
                    continue;
                }

                if (distance2 >= outlineRadius2) {
                    mutablePos.set(centre.getX() + x, centre.getY() + y, centre.getZ());
                    template.setBlockState(mutablePos, outline);
                }
            }
        }
    }
}
