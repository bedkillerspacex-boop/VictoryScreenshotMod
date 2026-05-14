package com.southside.victoryss;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ScoreboardCountdownTracker {

    private static final Pattern PATTERN_MINSEC = Pattern.compile(
            "\\u6e38\\u620f\\u5c06\\u5728[\\s\\p{Z}]*(\\d+):(\\d+)[\\s\\p{Z}]*\\u540e\\u5f00\\u59cb");
    private static final Pattern PATTERN_SEC    = Pattern.compile(
            "\\u6e38\\u620f\\u5c06\\u5728[\\s\\p{Z}]*(\\d+)[\\s\\p{Z}]*\\u79d2\\u540e\\u5f00\\u59cb");
    private static final Pattern PATTERN_BARE   = Pattern.compile(
            "^[\\s\\p{Z}]*(\\d+):(\\d+)[\\s\\p{Z}]*$");

    private ScoreboardCountdownTracker() {}

    public static int getCountdownSeconds(MinecraftClient client) {
        if (client.world == null) return -1;
        Scoreboard sb = client.world.getScoreboard();

        ScoreboardObjective sidebar = sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (sidebar != null) {
            int r = matchText(sidebar.getDisplayName().getString());
            if (r >= 0) return r;

            var holders = sb.getKnownScoreHolders();
            var holdersCopy = new java.util.ArrayList<>(holders);

            for (var holder : holdersCopy) {
                String hn = holder.getNameForScoreboard();
                r = matchText(hn);
                if (r >= 0) return r;

                var abstractTeam = sb.getScoreHolderTeam(hn);
                if (abstractTeam instanceof Team team) {
                    String prefix = team.getPrefix().getString();
                    String suffix = team.getSuffix().getString();
                    r = matchText(prefix + hn + suffix);
                    if (r >= 0) return r;
                    r = matchText(prefix);
                    if (r >= 0) return r;
                    r = matchText(suffix);
                    if (r >= 0) return r;
                }
            }
        }

        var holders = sb.getKnownScoreHolders();
        var holdersCopy = new java.util.ArrayList<>(holders);
        for (var holder : holdersCopy) {
            String hn = holder.getNameForScoreboard();
            int r = matchText(hn);
            if (r >= 0) return r;

            var abstractTeam = sb.getScoreHolderTeam(hn);
            if (abstractTeam instanceof Team team) {
                r = matchText(team.getPrefix().getString() + hn + team.getSuffix().getString());
                if (r >= 0) return r;
            }
        }

        return -1;
    }

    private static int matchText(String raw) {
        if (raw == null || raw.isEmpty()) return -1;
        String text = raw.replaceAll("\\u00a7.", "").trim();

        Matcher m = PATTERN_MINSEC.matcher(text);
        if (m.find()) return Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));

        m = PATTERN_SEC.matcher(text);
        if (m.find()) return Integer.parseInt(m.group(1));

        m = PATTERN_BARE.matcher(text);
        if (m.find()) return Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));

        return -1;
    }
}
