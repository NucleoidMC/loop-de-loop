package io.github.restioson.loopdeloop.game.map;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;

public class LoopDeLoopHoop {
    public BlockPos centre;
    public int radius;

    public LoopDeLoopHoop(BlockPos centre, int radius) {
        this.centre = centre;
        this.radius = radius;
    }

    public boolean intersectsSegment(Vec3d begin, Vec3d end) {
        // If the hoop contains the end position, it intersects
        if (this.contains(end)) {
            return true;
        }

        // Find the intersection between the line and the hoop plane
        Vec3d intersection = lineIntersectsPlane(begin, end, centre.getZ() + 0.5);
        if (intersection == null) {
            // no intersection
            return false;
        }

        // Check if the intersection point is contained within the loop
        return this.contains(intersection.x, intersection.y);
    }

    public boolean contains(Vec3d pos) {
        double centerZ = this.centre.getZ() + 0.5;
        return Math.abs(pos.getZ() - centerZ) <= 0.8 && this.contains(pos.getX(), pos.getY());
    }

    private boolean contains(double x, double y) {
        int adjRadius = this.radius - 1; // radius - 1 is to avoid allowing people to go on top of corners
        int dx = MathHelper.floor(x) - this.centre.getX();
        int dy = MathHelper.floor(y) - this.centre.getY();
        return dx * dx + dy * dy <= adjRadius * adjRadius;
    }

    @Nullable
    private static Vec3d lineIntersectsPlane(Vec3d origin, Vec3d target, double planeZ) {
        Vec3d ray = target.subtract(origin);
        if (Math.abs(ray.z) <= 1e-5) {
            return null;
        }

        double distanceAlongRay = (planeZ - origin.z) / ray.z;
        double distanceAlongRay2 = distanceAlongRay * distanceAlongRay;
        double rayLength2 = ray.lengthSquared();

        if (distanceAlongRay < 0.0 || distanceAlongRay2 >= rayLength2) {
            return null;
        }

        return origin.add(ray.normalize().multiply(distanceAlongRay));
    }
}
