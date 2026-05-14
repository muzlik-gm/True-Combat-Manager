package com.muzlik.pvpcombat.debug;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Map;

/**
 * Session-scoped NDJSON logger for debug instrumentation (writes to workspace log file).
 */
public final class AgentDebugLog {

    private static final String SESSION_ID = "c2637d";

    private AgentDebugLog() {
    }

    // #region agent log
    public static void log(String runId, String hypothesisId, String location, String message, Map<String, Object> data) {
        try {
            Path path = Paths.get(System.getProperty("pvpcombat.agentlog.path",
                    Paths.get(System.getProperty("user.dir"), "debug-c2637d.log").toString()));
            String dataJson = toJsonObject(data);
            long ts = System.currentTimeMillis();
            String line = String.format(Locale.US,
                    "{\"sessionId\":\"%s\",\"runId\":\"%s\",\"hypothesisId\":\"%s\",\"location\":\"%s\",\"message\":\"%s\",\"data\":%s,\"timestamp\":%d}%n",
                    esc(SESSION_ID), esc(runId), esc(hypothesisId), esc(location), esc(message), dataJson, ts);
            Files.writeString(path, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }

    private static String toJsonObject(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> e : data.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(esc(e.getKey())).append("\":");
            Object v = e.getValue();
            if (v == null) {
                sb.append("null");
            } else if (v instanceof Number || v instanceof Boolean) {
                sb.append(v);
            } else {
                sb.append('"').append(esc(String.valueOf(v))).append('"');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
    // #endregion
}
