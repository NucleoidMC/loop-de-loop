package io.github.restioson.loopdeloop.game;

import net.minecraft.util.math.Vec3d;

public class LoopDeLoopPlayer {
    public int lastHoop = -1;
    public Vec3d lastPos;
    public long lastFailOrSuccess = -1;
    public int previousFails = -1;
}
