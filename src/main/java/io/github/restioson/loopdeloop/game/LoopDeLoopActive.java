package io.github.restioson.loopdeloop.game;

import io.github.restioson.loopdeloop.game.map.LoopDeLoopHoop;
import io.github.restioson.loopdeloop.game.map.LoopDeLoopMap;
import io.github.restioson.loopdeloop.game.map.LoopDeLoopWinner;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.event.GameCloseListener;
import xyz.nucleoid.plasmid.game.event.GameOpenListener;
import xyz.nucleoid.plasmid.game.event.GameTickListener;
import xyz.nucleoid.plasmid.game.event.OfferPlayerListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDamageListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.PlayerRemoveListener;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LoopDeLoopActive {
    private final GameWorld gameWorld;
    private final LoopDeLoopMap map;
    private final LoopDeLoopConfig config;
    private final List<LoopDeLoopWinner> finished;
    private final LoopDeLoopSpawnLogic spawnLogic;
    private final LoopDeLoopTimerBar timerBar = new LoopDeLoopTimerBar();
    private final Object2ObjectMap<ServerPlayerEntity, LoopDeLoopPlayer> player_states;
    @Nullable
    private ServerPlayerEntity lastCompleter;
    @Nullable
    Team team;
    private long closeTime = -1;
    private long finishTime = -1;
    private long startTime = -1;

    private LoopDeLoopActive(GameWorld gameWorld, LoopDeLoopMap map, LoopDeLoopConfig config, Set<ServerPlayerEntity> participants) {
        this.gameWorld = gameWorld;
        this.map = map;
        this.config = config;
        this.finished = new ArrayList<>();
        this.spawnLogic = new LoopDeLoopSpawnLogic(gameWorld, map);
        this.player_states = new Object2ObjectOpenHashMap<>();

        for (ServerPlayerEntity player : participants) {
            this.player_states.put(player, new LoopDeLoopPlayer());
        }
    }

    public static void open(GameWorld gameWorld, LoopDeLoopMap map, LoopDeLoopConfig config) {
        Set<ServerPlayerEntity> participants = gameWorld.getPlayers();
        LoopDeLoopActive active = new LoopDeLoopActive(gameWorld, map, config, participants);

        gameWorld.openGame(game -> {
            game.setRule(GameRule.CRAFTING, RuleResult.DENY);
            game.setRule(GameRule.PORTALS, RuleResult.DENY);
            game.setRule(GameRule.PVP, RuleResult.DENY);
            game.setRule(GameRule.BLOCK_DROPS, RuleResult.DENY);
            game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
            game.setRule(GameRule.HUNGER, RuleResult.DENY);
            game.setRule(GameRule.THROW_ITEMS, RuleResult.DENY);

            game.on(GameOpenListener.EVENT, active::onOpen);
            game.on(GameCloseListener.EVENT, active::onClose);

            game.on(OfferPlayerListener.EVENT, player -> JoinResult.ok());
            game.on(PlayerAddListener.EVENT, active::addPlayer);
            game.on(PlayerRemoveListener.EVENT, active::removePlayer);

            game.on(GameTickListener.EVENT, active::tick);

            game.on(PlayerDamageListener.EVENT, active::onPlayerDamage);
            game.on(PlayerDeathListener.EVENT, active::onPlayerDeath);
        });
    }

    private void onOpen() {
        ServerWorld world = this.gameWorld.getWorld();
        ServerScoreboard scoreboard = world.getScoreboard();

        Team oldTeam = scoreboard.getTeam("loopdeloop");
        if (oldTeam != null) {
            scoreboard.removeTeam(oldTeam);
        }

        this.team = scoreboard.addTeam("loopdeloop");
        this.team.setCollisionRule(AbstractTeam.CollisionRule.NEVER);
        for (ServerPlayerEntity player : this.player_states.keySet()) {
            scoreboard.addPlayerToTeam(player.getEntityName(), this.team);
            this.spawnParticipant(player);
        }

        long time = world.getTime();
        this.startTime = time - (time % 20) + (4 * 20) + 19;
        this.finishTime = this.startTime + (this.config.timeLimit * 20);
    }

    private void onClose() {
        this.gameWorld.getWorld().getScoreboard().removeTeam(this.team);
        this.timerBar.close();
    }

    private void addPlayer(ServerPlayerEntity player) {
        if (!this.player_states.containsKey(player)) {
            this.spawnSpectator(player);
        }
        this.timerBar.addPlayer(player);
    }

    private void removePlayer(ServerPlayerEntity player) {
        this.player_states.remove(player);
    }

    // thx https://stackoverflow.com/a/6810409/4871468
    private static String ordinal(int i) {
        String[] suffixes = new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";
            default:
                return i + suffixes[i % 10];
        }
    }

    private void tick() {
        ServerWorld world = this.gameWorld.getWorld();
        long time = world.getTime();

        if (this.closeTime > 0) {
            if (time >= this.closeTime) {
                this.gameWorld.close();
            }
            return;
        }

        if (this.startTime > time) {
            this.tickStartWaiting(time);
            return;
        }

        if (time > this.finishTime || this.player_states.isEmpty()) {
            this.tickEndWaiting(time);
            return;
        }

        this.timerBar.update(this.finishTime - time, config.timeLimit * 20);
        this.tickPlayers(time);
    }

    private void tickStartWaiting(long time) {
        float sec_f = (this.startTime - time) / 20.0f;

        if (sec_f > 1) {
            for (Map.Entry<ServerPlayerEntity, LoopDeLoopPlayer> entry : this.player_states.entrySet()) {
                LoopDeLoopPlayer state = entry.getValue();
                ServerPlayerEntity player = entry.getKey();

                if (state.lastPos == null) {
                    state.lastPos = player.getPos();
                    continue;
                }

                player.teleport(state.lastPos.x, state.lastPos.y, state.lastPos.z);
            }
        }

        int sec = (int) Math.floor(sec_f) - 1;

        if ((this.startTime - time) % 20 == 0) {
            if (sec > 0) {
                this.broadcastTitle(new LiteralText(Integer.toString(sec)).formatted(Formatting.BOLD));
                this.broadcastSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP, 1.0F);
            } else {
                this.broadcastTitle(new LiteralText("Go!").formatted(Formatting.BOLD));
                this.broadcastSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP, 2.0F);
            }
        }
    }

    private void tickEndWaiting(long time) {
        for (ServerPlayerEntity player : this.player_states.keySet()) {
            player.setGameMode(GameMode.SPECTATOR);
        }

        this.closeTime = time + (5 * 20);
        this.broadcastWin();
    }

    private void tickPlayers(long time) {
        Iterator<Map.Entry<ServerPlayerEntity, LoopDeLoopPlayer>> iterator = this.player_states.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ServerPlayerEntity, LoopDeLoopPlayer> entry = iterator.next();
            LoopDeLoopPlayer state = entry.getValue();
            ServerPlayerEntity player = entry.getKey();

            int nextHoop = state.lastHoop + 1;

            if (nextHoop >= this.map.hoops.size()) {
                iterator.remove();
                this.finished.add(new LoopDeLoopWinner(player.getEntityName(), time));
                int idx = this.finished.size();
                player.sendMessage(new LiteralText("You finished in " + ordinal(idx) + " place!"), true);
                player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
                player.setGameMode(GameMode.SPECTATOR);
                this.lastCompleter = player;
                continue;
            }

            LoopDeLoopHoop hoop = this.map.hoops.get(nextHoop);

            int lastHoopZ;
            int lastHoopX;
            if (state.lastHoop == -1) {
                lastHoopZ = -5;
                lastHoopX = 0;
            } else {
                BlockPos last = this.map.hoops.get(state.lastHoop).centre;
                lastHoopZ = last.getZ();
                lastHoopX = last.getX();
            }

            BlockPos centre = hoop.centre;

            double yMax = ((double) this.config.maxYVariation / 2) + 30;
            boolean outOfBounds = Math.floor(player.getZ()) > centre.getZ() ||
                    player.getZ() < lastHoopZ - 10 ||
                    player.getY() < 75 - yMax ||
                    player.getY() > 75 + yMax ||
                    player.getY() < 0 ||
                    player.getX() > Math.max(centre.getX(), lastHoopX) + 30 ||
                    player.getX() < Math.min(centre.getX(), lastHoopX) - 30;

            // The two parts of the `or` here represent the two paths: the player is moving fast, or they are moving slow
            //
            // If they are moving fast, the line segment connecting their past and present movement will intersect the
            // hoop's circle and the first part will be true.
            //
            // If they are moving slow, there may not be enough precision to detect this, so the slow path is fallen back
            // to, simply checking if the end coordinate is inside of the hoop.
            if (hoop.intersectsSegment(state.lastPos, player.getPos())) {
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
                giveRocket(player, 1);
                state.lastHoop += 1;
            } else if ((time - state.lastFailTp > 5) && outOfBounds) {
                this.failHoop(player, state, time);
            }

            state.lastPos = player.getPos();
        }
    }

    private void failHoop(ServerPlayerEntity player, LoopDeLoopPlayer state, long time) {
        if (time - state.lastFailTp < 5) {
            return;
        }

        state.lastFailTp = time;
        giveRocket(player, 1);

        ServerWorld world = this.gameWorld.getWorld();
        List<FireworkRocketEntity> rockets = world.getEntitiesByType(
                EntityType.FIREWORK_ROCKET,
                player.getBoundingBox(),
                firework -> firework.getOwner() == player
        );
        rockets.forEach(Entity::remove);

        if (state.lastHoop == -1) {
            this.spawnLogic.spawnPlayer(player);
        } else {
            LoopDeLoopHoop lastHoop = this.map.hoops.get(state.lastHoop);
            Vec3d centre = Vec3d.ofCenter(lastHoop.centre);
            float radius = 2;
            double x = centre.x + MathHelper.nextDouble(player.getRandom(), -radius, radius);
            double y = centre.y + MathHelper.nextFloat(player.getRandom(), -radius, radius);
            double z = centre.z + 2;
            player.teleport(this.gameWorld.getWorld(), x, y, z, 0.0f, 0.0f);
        }

        player.playSound(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    private void broadcastWin() {
        Text message;

        if (this.finished.isEmpty()) {
            message = new LiteralText("The game ended, but nobody won!").formatted(Formatting.GOLD);
        } else {
            StringBuilder message_builder = new StringBuilder();
            message_builder.append("The game has ended!\n");

            for (int i = 0; i < 3 && i < this.finished.size(); i++) {
                LoopDeLoopWinner player = this.finished.get(i);
                message_builder.append(String.format(
                        "    %s place - %s in %.2fs\n",
                        ordinal(i + 1),
                        player.name,
                        (player.time - this.startTime) / 20.0f
                ));
            }

            String message_string = message_builder.toString();
            message = new LiteralText(message_string).formatted(Formatting.GOLD);
        }

        this.broadcastMessage(message);
        this.broadcastSound(SoundEvents.ENTITY_VILLAGER_YES);
    }

    private boolean onPlayerDamage(ServerPlayerEntity player, DamageSource source, float amount) {
        long time = this.gameWorld.getWorld().getTime();
        this.failHoop(player, this.player_states.get(player), time);
        return true;
    }

    private boolean onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        long time = this.gameWorld.getWorld().getTime();
        this.failHoop(player, this.player_states.get(player), time);
        return true;
    }

    private void spawnParticipant(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);
        this.spawnLogic.spawnPlayer(player);

        ItemStack elytra = ItemStackBuilder.of(Items.ELYTRA)
                .setUnbreakable()
                .build();
        player.equipStack(EquipmentSlot.CHEST, elytra);

        giveRocket(player, this.config.startRockets);
    }

    private static void giveRocket(ServerPlayerEntity player, int n) {
        ItemStack rockets = new ItemStack(Items.FIREWORK_ROCKET, n);
        player.inventory.insertStack(rockets);
    }

    private void spawnSpectator(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
        this.spawnLogic.spawnPlayer(player);
    }

    // TODO: extract common broadcast utils into plasmid
    private void broadcastMessage(Text message) {
        for (ServerPlayerEntity player : this.gameWorld.getPlayers()) {
            player.sendMessage(message, false);
        }
    }

    private void broadcastTitle(Text message) {
        for (ServerPlayerEntity player : this.gameWorld.getPlayers()) {
            TitleS2CPacket packet = new TitleS2CPacket(TitleS2CPacket.Action.TITLE, message, 1, 5,  3);
            player.networkHandler.sendPacket(packet);
        }
    }

    private void broadcastSound(SoundEvent sound, float pitch) {
        for (ServerPlayerEntity player : this.gameWorld.getPlayers()) {
            if (player.equals(this.lastCompleter)) {
                continue;
            }
            player.playSound(sound, SoundCategory.PLAYERS, 1.0F, pitch);
        }
    }

    private void broadcastSound(SoundEvent sound) {
        this.broadcastSound(sound, 1.0F);
    }
}
