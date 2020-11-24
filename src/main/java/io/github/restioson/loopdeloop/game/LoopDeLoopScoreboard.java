package io.github.restioson.loopdeloop.game;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;
import xyz.nucleoid.plasmid.widget.SidebarWidget;

import java.util.List;

public class LoopDeLoopScoreboard {

    private final SidebarWidget sidebar;
    private final int totalHoops;

    public LoopDeLoopScoreboard(GlobalWidgets widgets, int totalHoops) {
        Text title = new LiteralText("Loop-de-loop").formatted(Formatting.BLUE, Formatting.BOLD);
        this.sidebar = widgets.addSidebar(title);
        this.totalHoops = totalHoops;
    }

    public void render(List<LoopDeLoopPlayer> leaderboard) {
        this.sidebar.set(content -> {
            String top = String.format(
                    "%sTotal hoops:%s %s",
                    Formatting.AQUA + Formatting.BOLD.toString(),
                    Formatting.RESET,
                    this.totalHoops
            );

            content.writeLine(top);

            for (LoopDeLoopPlayer entry : leaderboard) {
                String line = String.format(
                        "%s%s:%s %d hoops",
                        Formatting.AQUA,
                        entry.player.getEntityName(),
                        Formatting.RESET,
                        entry.lastHoop + 1
                );
                content.writeLine(line);
            }
        });
    }
}
