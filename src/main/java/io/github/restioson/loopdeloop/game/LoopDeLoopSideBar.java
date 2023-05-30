package io.github.restioson.loopdeloop.game;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.widget.SidebarWidget;

import java.util.List;

public class LoopDeLoopSideBar {

    private final SidebarWidget sidebar;
    private final int totalHoops;

    public LoopDeLoopSideBar(GlobalWidgets widgets, int totalHoops) {
        Text title = Text.literal("Loop-de-loop").formatted(Formatting.BLUE, Formatting.BOLD);
        this.sidebar = widgets.addSidebar(title);
        this.totalHoops = totalHoops;
    }

    public void render(List<LoopDeLoopPlayer> leaderboard) {
        this.sidebar.set(content -> {
            var top = Text.literal("Total hoops: ")
                    .formatted(Formatting.AQUA, Formatting.BOLD)
                    .append(Text.literal(String.valueOf(this.totalHoops)).formatted(Formatting.WHITE));
            content.add(top);

            for (LoopDeLoopPlayer entry : leaderboard) {
                var line = entry.player.getName().copy().append(": ").formatted(Formatting.AQUA)
                        .append(Text.literal(String.valueOf(entry.lastHoop + 1)).formatted(Formatting.WHITE));
                content.add(line);
            }
        });
    }
}
