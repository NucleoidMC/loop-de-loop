package io.github.restioson.loopdeloop.game;

import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.BossBarWidget;

public final class LoopDeLoopTimerBar {
    private final BossBarWidget bar;

    public LoopDeLoopTimerBar(GlobalWidgets widgets) {
        Component title = Component.literal("Waiting for the game to start...");
        this.bar = widgets.addBossBar(title, BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);
    }

    public void update(long ticksUntilEnd, long totalTicksUntilEnd) {
        if (ticksUntilEnd % 20 == 0) {
            this.bar.setTitle(this.getText(ticksUntilEnd));
            this.bar.setProgress((float) ticksUntilEnd / totalTicksUntilEnd);
        }
    }

    private Component getText(long ticksUntilEnd) {
        long secondsUntilEnd = ticksUntilEnd / 20;

        long minutes = secondsUntilEnd / 60;
        long seconds = secondsUntilEnd % 60;
        String time = String.format("%02d:%02d left", minutes, seconds);

        return Component.literal(time);
    }
}
