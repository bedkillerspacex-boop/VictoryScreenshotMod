package com.southside.victoryss;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;

import java.util.regex.Pattern;

public class VictoryDetector {
    private static final Pattern VICTORY_PATTERN = Pattern.compile(".*(\u80dc\u5229|VICTORY|WINNER|WIN).*");
    private static boolean manualVictoryTrigger = false;

    public static void triggerVictory() {
        manualVictoryTrigger = true;
    }

    public static boolean isVictory() {
        if (manualVictoryTrigger) {
            manualVictoryTrigger = false;
            return true;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;

        Scoreboard sb = client.world.getScoreboard();
        
        ScoreboardObjective sidebar = sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (sidebar != null) {
            if (match(sidebar.getDisplayName().getString())) return true;

            var holders = sb.getKnownScoreHolders();
            for (var holder : holders) {
                String hn = holder.getNameForScoreboard();
                if (match(hn)) return true;

                var abstractTeam = sb.getScoreHolderTeam(hn);
                if (abstractTeam instanceof Team team) {
                    if (match(team.getPrefix().getString() + hn + team.getSuffix().getString())) return true;
                }
            }
        }

        return false;
    }

    public static boolean match(String raw) {
        if (raw == null || raw.isEmpty()) return false;
        // Strip color codes (§ = U+00A7)
        String text = raw.replaceAll("\u00a7.", "").toUpperCase().trim();
        return VICTORY_PATTERN.matcher(text).matches();
    }
}
