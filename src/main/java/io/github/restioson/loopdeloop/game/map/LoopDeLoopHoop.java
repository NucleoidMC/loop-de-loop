package io.github.restioson.loopdeloop.game.map;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class LoopDeLoopHoop {
    public BlockPos centre;
    public int radius;

    public LoopDeLoopHoop(BlockPos centre, int radius) {
        this.centre = centre;
        this.radius = radius;
    }

    public boolean intersectsSegment(Vec3 begin, Vec3 end) {
        // If the hoop contains the end position, it intersects
        if (this.contains(end)) {
            return true;
        }

        // Find the intersection between the line and the hoop plane
        Vec3 intersection = lineIntersectsPlane(begin, end, centre.getZ() + 0.5);
        if (intersection == null) {
            // no intersection
            return false;
        }

        // Check if the intersection point is contained within the loop
        return this.contains(intersection.x, intersection.y);
    }

    public boolean contains(Vec3 pos) {
        double centerZ = this.centre.getZ() + 0.5;
        return Math.abs(pos.z() - centerZ) <= 0.8 && this.contains(pos.x(), pos.y());
    }

    private boolean contains(double x, double y) {
        int adjRadius = this.radius - 1; // radius - 1 is to avoid allowing people to go on top of corners
        int dx = Mth.floor(x) - this.centre.getX();
        int dy = Mth.floor(y) - this.centre.getY();
        return dx * dx + dy * dy <= adjRadius * adjRadius;
    }

    @Nullable
    private static Vec3 lineIntersectsPlane(Vec3 origin, Vec3 target, double planeZ) {
        Vec3 ray = target.subtract(origin);
        if (Math.abs(ray.z) <= 1e-5) {
            return null;
        }

        double distanceAlongRay = (planeZ - origin.z) / ray.z;
        double distanceAlongRay2 = distanceAlongRay * distanceAlongRay;
        double rayLength2 = ray.lengthSqr();

        if (distanceAlongRay < 0.0 || distanceAlongRay2 >= rayLength2) {
            return null;
        }

        return origin.add(ray.normalize().scale(distanceAlongRay));
    }
}
