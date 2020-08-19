package io.github.restioson.loopdeloop.game;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.widget.BossBarWidget;

public final class LoopDeLoopTimerBar implements AutoCloseable {
    private final BossBarWidget bar;

    public LoopDeLoopTimerBar(GameWorld gameWorld) {
        LiteralText title = new LiteralText("Waiting for the game to start...");

        this.bar = BossBarWidget.open(gameWorld.getPlayerSet(), title, BossBar.Color.GREEN, BossBar.Style.NOTCHED_10);
    }

    public void update(long ticksUntilEnd, long totalTicksUntilEnd) {
        if (ticksUntilEnd % 20 == 0) {
            this.bar.setTitle(this.getText(ticksUntilEnd));
            this.bar.setProgress((float) ticksUntilEnd / totalTicksUntilEnd);
        }
    }

    private Text getText(long ticksUntilEnd) {
        long secondsUntilEnd = ticksUntilEnd / 20;

        long minutes = secondsUntilEnd / 60;
        long seconds = secondsUntilEnd % 60;
        String time = String.format("%02d:%02d left", minutes, seconds);

        return new LiteralText(time);
    }

    @Override
    public void close() {
        this.bar.close();
    }
}
