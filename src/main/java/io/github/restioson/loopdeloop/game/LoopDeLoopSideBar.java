package io.github.restioson.loopdeloop.game;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.widget.SidebarWidget;

import java.util.List;

public class LoopDeLoopSideBar {

    private final SidebarWidget sidebar;
    private final int totalHoops;

    public LoopDeLoopSideBar(GlobalWidgets widgets, int totalHoops) {
        Text title = new LiteralText("Loop-de-loop").formatted(Formatting.BLUE, Formatting.BOLD);
        this.sidebar = widgets.addSidebar(title);
        this.totalHoops = totalHoops;
    }

    public void render(List<LoopDeLoopPlayer> leaderboard) {
        this.sidebar.set(content -> {
            var top = new LiteralText("Total hoops: ")
                    .formatted(Formatting.AQUA, Formatting.BOLD)
                    .append(new LiteralText(String.valueOf(this.totalHoops)).formatted(Formatting.WHITE));
            content.add(top);

            for (LoopDeLoopPlayer entry : leaderboard) {
                var line = entry.player.getName().shallowCopy().append(": ").formatted(Formatting.AQUA)
                        .append(new LiteralText(String.valueOf(entry.lastHoop + 1)).formatted(Formatting.WHITE));
                content.add(line);
            }
        });
    }
}
