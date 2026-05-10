package io.github.restioson.loopdeloop.game;

import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.FixedFormat;

public class LoopDeLoopSideBar {

    private final SidebarWidget sidebar;
    private final int totalHoops;

    public LoopDeLoopSideBar(GlobalWidgets widgets, int totalHoops) {
        Component title = Component.literal("Loop-de-loop").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD);
        this.sidebar = widgets.addSidebar(title);
        this.totalHoops = totalHoops;
    }

    public void render(List<LoopDeLoopPlayer> leaderboard) {
        this.sidebar.set(content -> {
            var top = Component.literal("Total hoops: ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);

            var topNumber = Component.literal(String.valueOf(this.totalHoops)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD);
            var topFormat = new FixedFormat(topNumber);

            content.add(top, topFormat);

            for (LoopDeLoopPlayer entry : leaderboard) {
                var line = entry.player.getName().copy().withStyle(ChatFormatting.AQUA);

                var number = Component.literal(String.valueOf(entry.lastHoop + 1)).withStyle(ChatFormatting.WHITE);
                var format = new FixedFormat(number);

                content.add(line, format);
            }
        });
    }
}
