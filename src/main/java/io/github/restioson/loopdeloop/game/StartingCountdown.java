package io.github.restioson.loopdeloop.game;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import xyz.nucleoid.plasmid.api.game.player.PlayerIterable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;

public final class StartingCountdown {
    private static final Set<Relative> TELEPORT_FLAGS = Set.of(
            Relative.X_ROT, Relative.Y_ROT
    );

    private final long startTime;

    private final Map<UUID, Vec3> frozenPositions = new Object2ObjectOpenHashMap<>();

    public StartingCountdown(long startTime) {
        this.startTime = startTime;
    }

    public boolean tick(PlayerIterable players, long time) {
        float sec_f = (this.startTime - time) / 20.0f;
        if (sec_f > 1) {
            for (ServerPlayer player : players) {
                var frozenPosition = this.frozenPositions.computeIfAbsent(player.getUUID(), uuid -> player.position());
                player.connection.teleport(new PositionMoveRotation(new Vec3(frozenPosition.x, frozenPosition.y, frozenPosition.z), Vec3.ZERO, 0, 0), TELEPORT_FLAGS);
            }
        }

        int sec = Mth.floor(sec_f) - 1;

        if ((this.startTime - time) % 20 == 0) {
            if (sec > 0) {
                players.showTitle(Component.literal(Integer.toString(sec)).withStyle(ChatFormatting.BOLD), 1, 5, 3);
                players.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            } else {
                players.showTitle(Component.literal("Go!").withStyle(ChatFormatting.BOLD), 1, 5, 3);
                players.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.PLAYERS, 1.0F, 2.0F);
                return true;
            }
        }

        return false;
    }
}
