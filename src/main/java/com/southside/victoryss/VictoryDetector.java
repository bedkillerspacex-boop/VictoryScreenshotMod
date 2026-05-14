package com.southside.victoryss;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class VictoryDetector {
    private static final String SERVER_READY_TITLE_KEYWORD = "\u6218\u6597";
    private static final String ROUND_START_KEYWORD = "\u5f00\u59cb";
    private static final String SERVER_RECONFIGURING_KEYWORD = "\u91cd\u65b0\u914d\u7f6e\u4e2d";
    private static final String[] VICTORY_TITLE_KEYWORDS = {"\u80dc\u5229", "\u83b7\u80dc", "victory", "win"};

    private static long sessionStartTime = 0L;
    private static boolean wasInSession = false;
    private static boolean screenshotCombatUnlocked = false;
    private static boolean screenshotStartUnlocked = false;
    private static boolean nativeVictoryLatched = false;
    private static boolean nativeVictoryActive = false;
    private static boolean manualVictoryTrigger = false;
    private static long lastCountdownEndTime = 0L;
    private static String lastVictoryDebugPlain = "";
    private static Field inGameHudTitleField;
    private static Field inGameHudSubtitleField;
    private static List<Field> inGameHudTextFields;
    private static boolean titleFieldLookupAttempted = false;

    private VictoryDetector() {}

    public static void updateSession(MinecraftClient client) {
        boolean inSession = client != null && client.getNetworkHandler() != null;
        if (!wasInSession && inSession) {
            sessionStartTime = System.currentTimeMillis();
            resetScreenshotUnlocks();
            resetVictoryState();
        } else if (wasInSession && !inSession) {
            sessionStartTime = 0L;
            resetScreenshotUnlocks();
            resetVictoryState();
        }
        wasInSession = inSession;

        if (!inSession) {
            return;
        }

        if (isReconfiguringScreen(client)) {
            resetScreenshotUnlocks();
            resetVictoryState();
        }

        String hudText = stripFormatting(readCombinedHudText(client));
        if (!screenshotCombatUnlocked && hudText.contains(SERVER_READY_TITLE_KEYWORD)) {
            screenshotCombatUnlocked = true;
        }
        if (!screenshotStartUnlocked && hudText.contains(ROUND_START_KEYWORD)) {
            screenshotStartUnlocked = true;
        }
    }

    public static void onBattleStartMessage(String rawMessage) {
        if (rawMessage == null) {
            return;
        }
        String plain = stripFormatting(rawMessage);
        if (plain.contains("\u6218\u6597\u5f00\u59cb") || plain.contains("\u51b3\u51fa\u80dc\u8005")) {
            screenshotCombatUnlocked = true;
            screenshotStartUnlocked = true;
        }
    }

    public static void triggerVictory() {
        if (!isSuppressed() && canTriggerVictoryScreenshot()) {
            manualVictoryTrigger = true;
        }
    }

    public static boolean isVictory() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return false;
        }

        if (manualVictoryTrigger) {
            manualVictoryTrigger = false;
            nativeVictoryActive = true;
            nativeVictoryLatched = true;
            return true;
        }

        if (client.world == null || isSuppressed()) {
            return false;
        }

        String raw = readCombinedHudText(client);
        String plain = stripFormatting(raw).toLowerCase(Locale.ROOT);
        Set<String> candidates = buildDecodeCandidates(plain);
        boolean nowVictory = false;
        String hitReason = "NONE";
        for (String candidate : candidates) {
            if (containsCnVictory(candidate)) {
                nowVictory = true;
                hitReason = "CN_MATCH";
                break;
            }
            for (String key : VICTORY_TITLE_KEYWORDS) {
                if (candidate.contains(key)) {
                    nowVictory = true;
                    hitReason = "KEY:" + key;
                    break;
                }
            }
            if (nowVictory) {
                break;
            }
        }

        boolean fire = nowVictory && !nativeVictoryLatched;
        if (!plain.equals(lastVictoryDebugPlain)) {
            System.out.println("[VictorySS][VictoryDebug] raw=\"" + raw + "\" plain=\"" + plain + "\" nowVictory=" + nowVictory + " fire=" + fire + " reason=" + hitReason + " candidates=" + candidates);
            lastVictoryDebugPlain = plain;
        }

        nativeVictoryActive = nowVictory;
        nativeVictoryLatched = nowVictory;
        return fire;
    }

    public static boolean canTriggerVictoryScreenshot() {
        return sessionStartTime > 0 && screenshotCombatUnlocked && screenshotStartUnlocked;
    }

    public static void onVictoryScreenshotTaken() {
        resetScreenshotUnlocks();
        nativeVictoryActive = false;
        nativeVictoryLatched = true;
    }

    public static boolean match(String raw) {
        if (raw == null || raw.isEmpty()) {
            return false;
        }
        String plain = stripFormatting(raw).toLowerCase(Locale.ROOT);
        Set<String> candidates = buildDecodeCandidates(plain);
        for (String candidate : candidates) {
            if (containsCnVictory(candidate)) {
                return true;
            }
            for (String key : VICTORY_TITLE_KEYWORDS) {
                if (candidate.contains(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSuppressed() {
        MinecraftClient client = MinecraftClient.getInstance();
        int countdown = ScoreboardCountdownTracker.getCountdownSeconds(client);
        if (countdown >= 0) {
            lastCountdownEndTime = System.currentTimeMillis();
            return true;
        }

        long now = System.currentTimeMillis();
        boolean justJoined = sessionStartTime > 0 && (now - sessionStartTime < 5000L);
        boolean recentlyEnded = lastCountdownEndTime > 0 && (now - lastCountdownEndTime < 10000L);
        return justJoined || recentlyEnded;
    }

    private static boolean isReconfiguringScreen(MinecraftClient client) {
        if (client == null || client.currentScreen == null) {
            return false;
        }
        String screenTitle = stripFormatting(client.currentScreen.getTitle().getString()).toLowerCase(Locale.ROOT);
        return screenTitle.contains(SERVER_RECONFIGURING_KEYWORD) || screenTitle.contains("reconfiguring");
    }

    private static void resetScreenshotUnlocks() {
        screenshotCombatUnlocked = false;
        screenshotStartUnlocked = false;
    }

    private static void resetVictoryState() {
        manualVictoryTrigger = false;
        nativeVictoryLatched = false;
        nativeVictoryActive = false;
    }

    private static boolean containsCnVictory(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        if (text.contains("\u80dc\u5229")) {
            return true;
        }
        return text.indexOf('\u80dc') >= 0 && text.indexOf('\u5229') >= 0;
    }

    private static Set<String> buildDecodeCandidates(String plain) {
        Set<String> out = new LinkedHashSet<>();
        if (plain == null) {
            return out;
        }
        out.add(plain);
        out.add(redecode(plain, Charset.forName("GBK"), StandardCharsets.UTF_8));
        out.add(redecode(plain, Charset.forName("GB18030"), StandardCharsets.UTF_8));
        out.add(redecode(plain, StandardCharsets.ISO_8859_1, StandardCharsets.UTF_8));
        out.add(redecode(plain, StandardCharsets.UTF_8, Charset.forName("GBK")));
        out.remove("");
        return out;
    }

    private static String redecode(String text, Charset from, Charset to) {
        try {
            return new String(text.getBytes(from), to).toLowerCase(Locale.ROOT);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String readCombinedHudText(MinecraftClient client) {
        StringBuilder sb = new StringBuilder();
        String title = textToString(readTitleText(client));
        String subtitle = textToString(readSubtitleText(client));
        if (!title.isEmpty()) {
            sb.append(title).append(' ');
        }
        if (!subtitle.isEmpty()) {
            sb.append(subtitle).append(' ');
        }
        for (Text text : readAllHudTexts(client)) {
            String s = textToString(text);
            if (!s.isEmpty()) {
                sb.append(s).append(' ');
            }
        }
        return sb.toString();
    }

    private static String textToString(Text text) {
        return text == null ? "" : text.getString();
    }

    private static String stripFormatting(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replaceAll("\u00A7[0-9A-FK-ORa-fk-or]", "");
    }

    private static Text readTitleText(MinecraftClient client) {
        return readInGameHudText(client, true);
    }

    private static Text readSubtitleText(MinecraftClient client) {
        return readInGameHudText(client, false);
    }

    private static Text readInGameHudText(MinecraftClient client, boolean title) {
        if (client == null || client.inGameHud == null) {
            return null;
        }
        Field field = resolveInGameHudTextField(client, title);
        if (field == null) {
            return null;
        }
        try {
            Object value = field.get(client.inGameHud);
            return value instanceof Text text ? text : null;
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private static Field resolveInGameHudTextField(MinecraftClient client, boolean title) {
        Field cached = title ? inGameHudTitleField : inGameHudSubtitleField;
        if (cached != null) {
            return cached;
        }
        if (titleFieldLookupAttempted || client == null || client.inGameHud == null) {
            return null;
        }
        titleFieldLookupAttempted = true;

        String[] fieldNames = title
                ? new String[]{"title", "overlayTitle", "field_2018"}
                : new String[]{"subtitle", "overlaySubtitle", "field_2020"};
        for (String fieldName : fieldNames) {
            try {
                Field field = client.inGameHud.getClass().getDeclaredField(fieldName);
                if (Text.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    if (title) {
                        inGameHudTitleField = field;
                    } else {
                        inGameHudSubtitleField = field;
                    }
                    return field;
                }
            } catch (NoSuchFieldException ignored) {
            }
        }

        for (Field field : client.inGameHud.getClass().getDeclaredFields()) {
            if (!Text.class.isAssignableFrom(field.getType())) {
                continue;
            }
            String name = field.getName().toLowerCase(Locale.ROOT);
            if (title && name.contains("subtitle")) {
                continue;
            }
            if (!title && !name.contains("subtitle")) {
                continue;
            }
            field.setAccessible(true);
            if (title) {
                inGameHudTitleField = field;
            } else {
                inGameHudSubtitleField = field;
            }
            return field;
        }
        return null;
    }

    private static List<Text> readAllHudTexts(MinecraftClient client) {
        List<Text> out = new ArrayList<>();
        if (client == null || client.inGameHud == null) {
            return out;
        }
        if (inGameHudTextFields == null) {
            inGameHudTextFields = new ArrayList<>();
            for (Field field : client.inGameHud.getClass().getDeclaredFields()) {
                if (!Text.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                inGameHudTextFields.add(field);
            }
        }
        for (Field field : inGameHudTextFields) {
            try {
                Object value = field.get(client.inGameHud);
                if (value instanceof Text text) {
                    out.add(text);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return out;
    }
}
