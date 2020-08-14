package io.github.restioson.loopdeloop.game;


import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LoopDeLoopScoreboard {

    ServerScoreboard scoreboard;
    ScoreboardObjective hoops;
    int totalHoops;

    public LoopDeLoopScoreboard(ServerScoreboard scoreboard, int totalHoops) {
        ScoreboardObjective scoreboardObjective = new ScoreboardObjective(
                scoreboard,
                "loopdeloop_hoops",
                ScoreboardCriterion.DUMMY,
                new LiteralText("Loop-de-loop").formatted(Formatting.BLUE, Formatting.BOLD),
                ScoreboardCriterion.RenderType.INTEGER
        );
        scoreboard.addScoreboardObjective(scoreboardObjective);
        scoreboard.setObjectiveSlot(1, scoreboardObjective);
        this.hoops = scoreboardObjective;
        this.scoreboard = scoreboard;
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

        this.renderObjective(this.hoops, lines);
    }

    private void renderObjective(ScoreboardObjective objective, List<String> lines) {
        this.clear(objective);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            scoreboard.getPlayerScore(line, objective).setScore(lines.size() - i);
        }
    }

    private void clear(ScoreboardObjective objective) {
        Collection<ScoreboardPlayerScore> existing = this.scoreboard.getAllPlayerScores(objective);
        for (ScoreboardPlayerScore score : existing) {
            this.scoreboard.resetPlayerScore(score.getPlayerName(), objective);
        }
    }

    public void close() {
        scoreboard.removeObjective(this.hoops);
    }
}
