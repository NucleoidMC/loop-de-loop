package io.github.restioson.loopdeloop.game;

import com.google.common.collect.Sets;
import io.github.restioson.loopdeloop.LoopDeLoop;
import io.github.restioson.loopdeloop.game.map.LoopDeLoopHoop;
import io.github.restioson.loopdeloop.game.map.LoopDeLoopMap;
import io.github.restioson.loopdeloop.game.map.LoopDeLoopWinner;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.entity.EntityTypes;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamConfig;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.game.stats.StatisticKeys;
import xyz.nucleoid.plasmid.api.util.ItemStackBuilder;
import xyz.nucleoid.plasmid.api.util.PlayerUtil;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;

public final class LoopDeLoopActive {
    private static final GameTeam TEAM = new GameTeam(
            new GameTeamKey(LoopDeLoop.ID),
            GameTeamConfig.builder()
                    .setCollision(Team.CollisionRule.NEVER)
                    .setFriendlyFire(false)
                    .build()
    );

    private final ServerLevel world;
    private final GameSpace gameSpace;
    private final LoopDeLoopMap map;
    private final LoopDeLoopConfig config;
    private final List<LoopDeLoopWinner> finished;
    private final LoopDeLoopSpawnLogic spawnLogic;
    private final LoopDeLoopTimerBar timerBar;
    private final LoopDeLoopSideBar sidebar;

    private final TeamManager teamManager;

    // Only stores flying players, i.e non-completed players
    private final Object2ObjectMap<ServerPlayer, LoopDeLoopPlayer> playerStates;

    private StartingCountdown startingCountdown;

    @Nullable
    private ServerPlayer lastCompleter;
    private long closeTime = -1;
    private long finishTime = -1;
    private long startTime = -1;
    private long fallFlyingTime = -1;
    private static final int LEAP_INTERVAL_TICKS = 5;
    private static final double LEAP_VELOCITY = 3.0;

    private LoopDeLoopActive(
            ServerLevel world, GameSpace gameSpace,
            LoopDeLoopMap map, LoopDeLoopConfig config,
            Set<ServerPlayer> participants,
            GlobalWidgets widgets, TeamManager teamManager
    ) {
        this.world = world;
        this.gameSpace = gameSpace;
        this.map = map;
        this.config = config;
        this.finished = new ArrayList<>();
        this.spawnLogic = new LoopDeLoopSpawnLogic(world, map);
        this.timerBar = new LoopDeLoopTimerBar(widgets);
        this.playerStates = new Object2ObjectOpenHashMap<>();
        this.teamManager = teamManager;

        for (ServerPlayer player : participants) {
            this.playerStates.put(player, new LoopDeLoopPlayer(player));
        }

        this.sidebar = new LoopDeLoopSideBar(widgets, this.config.loops());
    }

    public static void open(ServerLevel world, GameSpace gameSpace, LoopDeLoopMap map, LoopDeLoopConfig config) {
        gameSpace.setActivity(activity -> {
            GlobalWidgets widgets = GlobalWidgets.addTo(activity);
            TeamManager teamManager = TeamManager.addTo(activity);
            teamManager.addTeam(TEAM);

            Set<ServerPlayer> participants = Sets.newHashSet(gameSpace.getPlayers());
            LoopDeLoopActive active = new LoopDeLoopActive(world, gameSpace, map, config, participants, widgets, teamManager);

            activity.deny(GameRuleType.CRAFTING);
            activity.deny(GameRuleType.PORTALS);
            activity.deny(GameRuleType.PVP);
            activity.deny(GameRuleType.BLOCK_DROPS);
            activity.deny(GameRuleType.FALL_DAMAGE);
            activity.deny(GameRuleType.HUNGER);
            activity.deny(GameRuleType.THROW_ITEMS);

            activity.listen(GameActivityEvents.ENABLE, active::onOpen);

            activity.listen(GamePlayerEvents.OFFER, JoinOffer::acceptSpectators);
            activity.listen(GamePlayerEvents.ACCEPT, active::acceptPlayer);
            activity.listen(GamePlayerEvents.REMOVE, active::removePlayer);

            activity.listen(GameActivityEvents.TICK, active::tick);

            activity.listen(PlayerDamageEvent.EVENT, active::onPlayerDamage);
            activity.listen(PlayerDeathEvent.EVENT, active::onPlayerDeath);

            activity.listen(ItemUseEvent.EVENT, active::onUseItem);
        });
    }

    private InteractionResult onUseItem(ServerPlayer player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);

        LoopDeLoopPlayer state = this.playerStates.get(player);
        if (state != null) {
            if (heldStack.getItem() == Items.FEATHER) {
                ItemCooldowns cooldown = player.getCooldowns();
                if (!cooldown.isOnCooldown(heldStack)) {
                    Vec3 rotationVec = player.getViewVector(1.0F);
                    player.setDeltaMovement(rotationVec.scale(LEAP_VELOCITY));
                    Vec3 oldVel = player.getDeltaMovement();
                    player.setDeltaMovement(oldVel.x, oldVel.y + 0.5f, oldVel.z);
                    player.connection.send(new ClientboundSetEntityMotionPacket(player));

                    PlayerUtil.playSoundToPlayer(player, SoundEvents.HORSE_SADDLE.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    cooldown.addCooldown(heldStack, LEAP_INTERVAL_TICKS);

                    state.boostUsed++;
                    return InteractionResult.SUCCESS_SERVER;
                }
            } else if (heldStack.getItem() == Items.FIREWORK_ROCKET) {
                ItemCooldowns cooldown = player.getCooldowns();
                if (!cooldown.isOnCooldown(heldStack)) {
                    state.boostUsed++;
                }
            }
        }

        return InteractionResult.PASS;
    }

    private void onOpen() {
        for (ServerPlayer player : this.playerStates.keySet()) {
            this.teamManager.addPlayerTo(player, TEAM.key());

            String[] lines;
            if (this.config.flappyMode()) {
                lines = new String[] {
                        "Loop-de-loop - fly through all the hoops with your feather. Whoever does it first wins!",
                        "Right-click with your feather to fly forwards."
                };
            } else {
                lines = new String[] {
                        "Loop-de-loop - fly through all the hoops with your elytra. Whoever does it first wins!",
                        "You start with some rockets and can get more by flying through hoops, or when you fail a hoop."
                };
            }

            for (String line : lines) {
                Component text = Component.literal(line).withStyle(ChatFormatting.GOLD);
                player.sendSystemMessage(text, false);
            }

            this.spawnParticipant(player);
        }

        long time = this.world.getGameTime();
        this.startTime = time - (time % 20) + (4 * 20) + 19;
        this.finishTime = this.startTime + (this.config.timeLimit() * 20L);
        this.fallFlyingTime = this.startTime + 20L;
        this.startingCountdown = new StartingCountdown(this.startTime);

        this.sidebar.render(this.buildLeaderboard());
    }

    private JoinAcceptorResult acceptPlayer(JoinAcceptor offer) {
        return this.spawnLogic.acceptPlayer(offer, GameType.SPECTATOR);
    }

    private void removePlayer(ServerPlayer player) {
        this.playerStates.remove(player);
    }

    // thx https://stackoverflow.com/a/6810409/4871468
    private static String ordinal(int i) {
        String[] suffixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
        return switch (i % 100) {
            case 11, 12, 13 -> i + "th";
            default -> i + suffixes[i % 10];
        };
    }

    private void tick() {
        long time = this.world.getGameTime();

        if (this.closeTime > 0) {
            if (time >= this.closeTime) {
                this.gameSpace.close(GameCloseReason.FINISHED);
            }
            return;
        }

        if (this.startingCountdown != null) {
            if (this.startingCountdown.tick(this.playerStates.keySet()::iterator, time)) {
                this.startingCountdown = null;

                if (this.map.getSpawnPlatform() != null && !config.debugMode()) {
                    for (BlockPos pos : this.map.getSpawnPlatform()) {
                         this.world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                }
            }

            return;
        }

        if (time > this.finishTime || this.playerStates.isEmpty()) {
            this.tickEndWaiting(time);
            return;
        }

        this.timerBar.update(this.finishTime - time, this.config.timeLimit() * 20L);
        this.tickPlayers(time);
    }

    private void tickEndWaiting(long time) {
        for (ServerPlayer player : this.playerStates.keySet()) {
            player.setGameMode(GameType.SPECTATOR);
        }

        this.closeTime = time + (5 * 20);
        this.broadcastWin();
    }

    private void tickPlayers(long time) {
        Iterator<Map.Entry<ServerPlayer, LoopDeLoopPlayer>> iterator = this.playerStates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ServerPlayer, LoopDeLoopPlayer> entry = iterator.next();
            LoopDeLoopPlayer state = entry.getValue();
            ServerPlayer player = entry.getKey();

            if (this.tickPlayer(player, state, time)) {
                iterator.remove();
            }
        }
    }

    private boolean tickPlayer(ServerPlayer player, LoopDeLoopPlayer state, long time) {
        if (time < this.fallFlyingTime) {
            player.startFallFlying();
        }

        int nextHoopIdx = state.lastHoop + 1;
        if (nextHoopIdx >= this.map.hoops.size()) {
            this.onPlayerFinish(player, state, time);
            return true;
        }

        if (state.lastHoop != -1 && player.onGround()) {
            if (!config.debugMode()) this.failHoop(player, state, time);
            return false;
        }

        LoopDeLoopHoop nextHoop = this.map.hoops.get(nextHoopIdx);

        Vec3 lastPos = state.lastPos;
        Vec3 currentPos = player.position();

        state.lastPos = currentPos;

        if (nextHoop.intersectsSegment(lastPos, currentPos)) {
            PlayerUtil.playSoundToPlayer(player, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);

            if (!this.config.flappyMode()) {
                giveRocket(player, 1, this.config.rocketPower());
            }

            state.totalHoops++;
            state.lastHoop = nextHoopIdx;
            state.previousFails = 0;
            state.lastFailOrSuccess = time;

            this.sidebar.render(this.buildLeaderboard());

            return false;
        }

        if (this.testFailure(player, state, nextHoop)) {
            this.failHoop(player, state, time);
            return false;
        }

        return false;
    }

    private boolean testFailure(ServerPlayer player, LoopDeLoopPlayer state, LoopDeLoopHoop nextHoop) {
        // player has traveled past the next hoop
        if (Math.floor(player.getZ()) > nextHoop.centre.getZ() + 1) {
            return true;
        }

        // player has travelled outside of the map
        double yMax = (this.config.yVarMax() / 2.0) + 30;
        if (player.getZ() < -5 || player.getY() < 128 - yMax || player.getY() > 128 + yMax) {
            return true;
        }

        int minX = nextHoop.centre.getX() - nextHoop.radius;
        int maxX = nextHoop.centre.getX() + nextHoop.radius;

        if (state.lastHoop >= 0) {
            LoopDeLoopHoop last = this.map.hoops.get(state.lastHoop);

            // we've gone behind the last hoop
            if (player.getZ() < last.centre.getZ() - 10) {
                return true;
            }

            minX = Math.min(minX, last.centre.getX() - last.radius);
            maxX = Math.max(maxX, last.centre.getX() + last.radius);
        }

        return player.getX() > maxX + 30 || player.getX() < minX - 30;
    }

    private List<LoopDeLoopPlayer> buildLeaderboard() {
        return this.playerStates.values().stream()
                .sorted(Comparator.comparingInt(player -> this.config.loops() - player.lastHoop))
                .limit(5)
                .collect(Collectors.toList());
    }

    private void onPlayerFinish(ServerPlayer player, LoopDeLoopPlayer state, long time) {
        this.finished.add(new LoopDeLoopWinner(player.getScoreboardName(), time));
        this.lastCompleter = player;
        boolean isFirst = false;

        String ordinal = ordinal(this.finished.size());

        var message = Component.literal("You finished in ")
                .append(Component.literal(ordinal).withStyle(ChatFormatting.AQUA))
                .append(" place!");

        player.sendSystemMessage(message, true);
        PlayerUtil.playSoundToPlayer(player, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.setGameMode(GameType.SPECTATOR);
        if (this.finished.size() == 1 && (long) playerStates.entrySet().size() > 1) isFirst = true;
        this.publishPlayerStatistics(player, state, time, isFirst);
    }

    private void publishPlayerStatistics(ServerPlayer player, LoopDeLoopPlayer state, long finishTime, boolean isFirst) {
        int playerTime = (int) (finishTime - this.startTime);

        var statistics = this.gameSpace.getStatistics().bundle(this.config.statisticsBundle());

        var playerStatistics = statistics.forPlayer(player);
        playerStatistics.set(StatisticKeys.QUICKEST_TIME, playerTime);
        if (isFirst) playerStatistics.set(StatisticKeys.GAMES_WON, playerStatistics.get(StatisticKeys.GAMES_WON, 0) + 1);
        state.applyTo(playerStatistics);
    }

    private void failHoop(ServerPlayer player, LoopDeLoopPlayer state, long time) {
        if (time - state.lastFailOrSuccess < 10) {
            return;
        }

        state.previousFails += 1;
        state.lastFailOrSuccess = time;
        state.missedHoops++;

        if (!this.config.flappyMode()) {
            giveRocket(player, Math.min(state.previousFails, 3), this.config.rocketPower());

            List<FireworkRocketEntity> rockets = this.world.getEntities(
                    EntityTypes.FIREWORK_ROCKET,
                    player.getBoundingBox(),
                    firework -> firework.getOwner() == player
            );
            for (FireworkRocketEntity rocket : rockets) {
                rocket.remove(Entity.RemovalReason.DISCARDED);
            }
        }

        if (state.lastHoop == -1) {
            this.spawnLogic.spawnPlayer(player);
        } else {
            LoopDeLoopHoop lastHoop = this.map.hoops.get(state.lastHoop);
            Vec3 centre = Vec3.atCenterOf(lastHoop.centre);

            RandomSource random = player.getRandom();
            float radius = 2;

            state.teleport(
                    centre.x + Mth.nextDouble(random, -radius, radius),
                    centre.y + Mth.nextDouble(random, -radius, radius),
                    centre.z + 2
            );
        }

        PlayerUtil.playSoundToPlayer(player, SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private void broadcastWin() {
        MutableComponent message;

        if (this.finished.isEmpty()) {
            message = Component.literal("The game ended, but nobody won!").withStyle(ChatFormatting.GOLD);
        } else {
            message = Component.literal("The game has ended!\n").withStyle(ChatFormatting.GOLD);

            for (int i = 0; i < 5 && i < this.finished.size(); i++) {
                LoopDeLoopWinner player = this.finished.get(i);

                Component ordinal = Component.literal(ordinal(i + 1)).withStyle(ChatFormatting.AQUA);
                Component playerName = Component.literal(player.name()).withStyle(ChatFormatting.AQUA);
                Component time = Component.literal(String.format("%.2fs", (player.time() - this.startTime) / 20.0f)).withStyle(ChatFormatting.GREEN);

                MutableComponent line = Component.literal("   ").append(ordinal)
                        .append(" place - ").append(playerName)
                        .append(" in ").append(time);
                message.append(line.append("\n"));
            }
        }

        this.gameSpace.getPlayers().sendMessage(message);
        this.broadcastSound(SoundEvents.VILLAGER_YES);
    }

    private EventResult onPlayerDamage(ServerPlayer player, DamageSource source, float amount) {
        long time = this.world.getGameTime();
        this.failHoop(player, this.playerStates.get(player), time);
        return EventResult.DENY;
    }

    private EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
        long time = this.world.getGameTime();
        this.failHoop(player, this.playerStates.get(player), time);
        return EventResult.DENY;
    }

    private void spawnParticipant(ServerPlayer player) {
        this.spawnLogic.resetPlayer(player, GameType.ADVENTURE);
        this.spawnLogic.spawnPlayer(player);

        if (this.config.flappyMode()) {
            ItemStack feather = ItemStackBuilder.of(Items.FEATHER)
                    .addLore(Component.literal("Flap flap"))
                    .build();
            player.setItemSlot(EquipmentSlot.OFFHAND, feather);
        } else {
            ItemStack elytra = ItemStackBuilder.of(Items.ELYTRA)
                    .setUnbreakable()
                    .build();
            player.setItemSlot(EquipmentSlot.CHEST, elytra);

            giveRocket(player, this.config.startRockets(), this.config.rocketPower());
        }
    }

    private static void giveRocket(ServerPlayer player, int n, int flightPower) {
        ItemStack rockets = new ItemStack(Items.FIREWORK_ROCKET, n);
        rockets.set(DataComponents.FIREWORKS, new Fireworks(flightPower, List.of()));
        player.getInventory().add(rockets);
    }

    private void broadcastSound(SoundEvent sound, float pitch) {
        for (ServerPlayer player : this.gameSpace.getPlayers()) {
            if (player.equals(this.lastCompleter)) {
                continue;
            }
            PlayerUtil.playSoundToPlayer(player, sound, SoundSource.PLAYERS, 1.0F, pitch);
        }
    }

    private void broadcastSound(SoundEvent sound) {
        this.broadcastSound(sound, 1.0F);
    }
}
