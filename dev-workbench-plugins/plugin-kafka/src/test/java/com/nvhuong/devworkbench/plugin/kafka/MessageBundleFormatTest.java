package com.nvhuong.devworkbench.plugin.kafka;

import org.testng.annotations.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.testng.Assert.assertTrue;

/**
 * Messages with placeholders go through MessageFormat, where a lone single quote starts a quoted section: a
 * translation such as "Cấu hình '{0}' đã bị xóa" was shown without the value and with the quotes stripped.
 */
public class MessageBundleFormatTest {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)[^}]*}");
    private static final Pattern LONE_QUOTE = Pattern.compile("(?<!')'(?!')");

    @Test
    public void placeholdersAreSubstitutedAndQuotesEscaped() throws IOException {
        List<String> problems = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/resources"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".properties")).toList()) {
                Properties bundle = new Properties();
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    bundle.load(reader);
                }
                for (String key : bundle.stringPropertyNames()) {
                    check(file.getFileName() + ": " + key, bundle.getProperty(key), problems);
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    private static void check(String where, String pattern, List<String> problems) {
        Matcher matcher = PLACEHOLDER.matcher(pattern);
        int maxIndex = -1;
        while (matcher.find()) {
            maxIndex = Math.max(maxIndex, Integer.parseInt(matcher.group(1)));
        }
        if (maxIndex < 0) {
            return;
        }
        if (LONE_QUOTE.matcher(pattern).find()) {
            problems.add(where + " has an unescaped single quote (use '' or a different quote character)");
        }
        Object[] args = new Object[maxIndex + 1];
        for (int i = 0; i <= maxIndex; i++) {
            args[i] = "ARG" + i + "VALUE";
        }
        String formatted = MessageFormat.format(pattern, args);
        Matcher used = PLACEHOLDER.matcher(pattern);
        while (used.find()) {
            String expected = "ARG" + used.group(1) + "VALUE";
            if (!formatted.contains(expected)) {
                problems.add(where + " does not substitute {" + used.group(1) + "}: " + formatted);
            }
        }
    }
}
