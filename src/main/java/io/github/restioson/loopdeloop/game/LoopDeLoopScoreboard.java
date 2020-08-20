package io.github.restioson.loopdeloop.game;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.widget.SidebarWidget;

import java.util.ArrayList;
import java.util.List;

public class LoopDeLoopScoreboard implements AutoCloseable {

    private final SidebarWidget sidebar;
    private final int totalHoops;

    public LoopDeLoopScoreboard(GameWorld gameWorld, int totalHoops) {
        Text title = new LiteralText("Loop-de-loop").formatted(Formatting.BLUE, Formatting.BOLD);
        this.sidebar = SidebarWidget.open(title, gameWorld.getPlayerSet());
        this.totalHoops = totalHoops;
    }

    public void render(List<LoopDeLoopPlayer> leaderboard) {
        String top = String.format(
                "%sTotal hoops:%s %s",
                Formatting.AQUA + Formatting.BOLD.toString(),
                Formatting.RESET,
                this.totalHoops
        );

        List<String> lines = new ArrayList<>();
        lines.add(top);

        for (LoopDeLoopPlayer entry : leaderboard) {
            String line = String.format(
                    "%s%s:%s %d hoops",
                    Formatting.AQUA,
                    entry.player.getEntityName(),
                    Formatting.RESET,
                    entry.lastHoop + 1
            );
            lines.add(line);
        }

        this.sidebar.set(lines.toArray(new String[0]));
    }

    @Override
    public void close() {
        this.sidebar.close();
    }
}
