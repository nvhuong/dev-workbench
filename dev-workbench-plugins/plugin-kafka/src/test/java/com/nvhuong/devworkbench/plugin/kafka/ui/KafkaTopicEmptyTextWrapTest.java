package com.nvhuong.devworkbench.plugin.kafka.ui;

import org.testng.annotations.Test;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class KafkaTopicEmptyTextWrapTest {

    /** The empty-topic hint was wrapped per character ("ở tr / ên", "địa c / hỉ"); words must stay whole. */
    @Test
    public void wrapsAtWordBoundaries() {
        FontMetrics fm = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics()
                .getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        String text = "Chưa kết nối. Bấm nút kết nối ở trên hoặc nhấn Enter trong ô địa chỉ.";
        Set<String> words = Arrays.stream(text.split(" ")).collect(Collectors.toSet());

        List<String> lines = KafkaTopicPanel.wrapLines(text, fm, 180);

        assertTrue(lines.size() > 1, lines.toString());
        for (String line : lines) {
            assertTrue(fm.stringWidth(line) <= 180, "line too wide: " + line);
            for (String word : line.split(" ")) {
                assertTrue(words.contains(word), "word split across lines: '" + word + "' in " + lines);
            }
        }
        assertEquals(String.join(" ", lines), text);
    }

    @Test
    public void keepsExplicitLineBreaksAndSplitsOverlongWords() {
        FontMetrics fm = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics()
                .getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        List<String> lines = KafkaTopicPanel.wrapLines("Not connected\nverylongtopicnamewithoutanyspaces", fm, 90);
        int firstLongLine = lines.indexOf(lines.stream().filter(line -> line.startsWith("verylong")).findFirst().orElseThrow());
        assertEquals(String.join(" ", lines.subList(0, firstLongLine)), "Not connected", "explicit break kept: " + lines);
        assertTrue(lines.size() - firstLongLine > 1, "an over-long word is still broken to fit: " + lines);
        assertEquals(String.join("", lines.subList(firstLongLine, lines.size())), "verylongtopicnamewithoutanyspaces");
        lines.forEach(line -> assertTrue(fm.stringWidth(line) <= 90 || line.length() == 1, "line too wide: " + line));
    }
}
