package io.github.restioson.loopdeloop.game;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.plasmid.game.player.PlayerIterable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StartingCountdown {
    private static final Set<PlayerPositionLookS2CPacket.Flag> TELEPORT_FLAGS = Set.of(
            PlayerPositionLookS2CPacket.Flag.X_ROT, PlayerPositionLookS2CPacket.Flag.Y_ROT
    );

    private final long startTime;

    private final Map<UUID, Vec3d> frozenPositions = new Object2ObjectOpenHashMap<>();

    public StartingCountdown(long startTime) {
        this.startTime = startTime;
    }

    public boolean tick(PlayerIterable players, long time) {
        float sec_f = (this.startTime - time) / 20.0f;
        if (sec_f > 1) {
            for (ServerPlayerEntity player : players) {
                var frozenPosition = this.frozenPositions.computeIfAbsent(player.getUuid(), uuid -> player.getPos());
                player.networkHandler.requestTeleport(frozenPosition.x, frozenPosition.y, frozenPosition.z, player.getYaw(), player.getPitch(), TELEPORT_FLAGS);
            }
        }

        int sec = MathHelper.floor(sec_f) - 1;

        if ((this.startTime - time) % 20 == 0) {
            if (sec > 0) {
                players.showTitle(Text.literal(Integer.toString(sec)).formatted(Formatting.BOLD), 1, 5, 3);
                players.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1.0F, 1.0F);
            } else {
                players.showTitle(Text.literal("Go!").formatted(Formatting.BOLD), 1, 5, 3);
                players.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1.0F, 2.0F);
                return true;
            }
        }

        return false;
    }
}
