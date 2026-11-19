package com.nvhuong.devworkbench.util;

import org.testng.annotations.Test;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class UiFontCatalogTest {

    @Test(description = "Danh sách phông chữ sẽ bỏ qua các mục trống và sắp xếp theo tên sau khi loại bỏ trùng lặp")
    public void testNormalizeFamilies_DeduplicatesAndSorts() {
        List<String> normalized = UiFontCatalog.normalizeFamilies(List.of(
                "Consolas",
                "Microsoft YaHei UI",
                "consolas",
                " ",
                "",
                "Arial"
        ));

        assertEquals(normalized, List.of("Arial", "Consolas", "Microsoft YaHei UI"));
    }

    @Test(description = "Thư mục phông chữ phải giữ lại kết quả kiểm tra khả năng sử dụng để nhắc nhở giao diện và sử dụng lại xác minh")
    public void testBuildFontOptions_PreservesSupportStatus() {
        List<UiFontCatalog.FontOption> options = UiFontCatalog.buildFontOptions(
                List.of("Font B", "font b", "Font A"),
                family -> switch (family) {
                    case "Font A" -> UiFontCatalog.FontSupport.FULL;
                    case "Font B" -> UiFontCatalog.FontSupport.NO_CJK;
                    default -> UiFontCatalog.FontSupport.NO_EMOJI;
                }
        );

        assertEquals(options.size(), 2);
        assertEquals(options.get(0).family(), "Font A");
        assertEquals(options.get(0).support(), UiFontCatalog.FontSupport.FULL);
        assertEquals(options.get(1).family(), "Font B");
        assertEquals(options.get(1).support(), UiFontCatalog.FontSupport.NO_CJK);
    }

    @Test(description = "Các phông chữ không hỗ trợ tiếng Trung sẽ không được coi là an toàn để sử dụng làm phông chữ giao diện")
    public void testFontSupport_IsUiSafeRequiresCjkSupport() {
        assertTrue(UiFontCatalog.FontSupport.FULL.isUiSafe());
        assertTrue(UiFontCatalog.FontSupport.NO_EMOJI.isUiSafe());
        assertFalse(UiFontCatalog.FontSupport.NO_CJK.isUiSafe());
    }

    @Test(description = "Giao diện tiếng Trung không được phép thiếu chữ tiếng Trung glyph của UI Font chữ, giao diện tiếng Anh chỉ nhắc nhở rủi ro")
    public void testFontSupport_ChineseLocaleRequiresCjkSafeUiFont() {
        assertFalse(UiFontCatalog.isUiFontAllowedForLocale(UiFontCatalog.FontSupport.NO_CJK, Locale.forLanguageTag("vi")));
        assertTrue(UiFontCatalog.isUiFontAllowedForLocale(UiFontCatalog.FontSupport.NO_CJK, Locale.ENGLISH));
        assertTrue(UiFontCatalog.isUiFontAllowedForLocale(UiFontCatalog.FontSupport.NO_EMOJI, Locale.forLanguageTag("vi")));
        assertTrue(UiFontCatalog.isUiFontAllowedForLocale(UiFontCatalog.FontSupport.FULL, Locale.forLanguageTag("vi")));
    }

    @Test(description = "Không nên sử dụng phông chữ vật lý không tồn tại Java của Dialog Dự phòng bị đánh giá sai là hỗ trợ tiếng Trung")
    public void testInspectFamily_MissingPhysicalFontShouldNotBeCjkSafe() {
        String missingFamily = "Definitely Missing DevWorkbench Font " + System.nanoTime();

        assertEquals(UiFontCatalog.inspectFamily(missingFamily), UiFontCatalog.FontSupport.NO_CJK);
    }

    @Test(description = "Khi tải danh sách phông chữ một cách lười biếng, giá trị hiện tại phải được giữ lại và hợp nhất với danh sách mới để loại bỏ việc sắp xếp lại")
    public void testMergeFamiliesForCombo_PreservesCurrentValue() {
        List<String> merged = UiFontCatalog.mergeFamiliesForCombo(
                "Microsoft YaHei Mono",
                List.of("Consolas", "microsoft yahei mono", "JetBrains Mono")
        );

        assertEquals(merged, List.of("Consolas", "JetBrains Mono", "Microsoft YaHei Mono"));
    }

    @Test(description = "Giao diện tiếng Trung UI Hộp thả xuống phông chữ sẽ lọc ra các phông chữ không hỗ trợ tiếng Trung")
    public void testMergeUiFamiliesForCombo_ChineseLocaleKeepsOnlyCjkSafeFonts() {
        List<String> merged = UiFontCatalog.mergeUiFamiliesForCombo(
                "",
                List.of(
                        new UiFontCatalog.FontOption("Consolas", UiFontCatalog.FontSupport.NO_CJK),
                        new UiFontCatalog.FontOption("Microsoft YaHei UI", UiFontCatalog.FontSupport.NO_EMOJI),
                        new UiFontCatalog.FontOption("Noto Color Emoji", UiFontCatalog.FontSupport.NO_CJK)
                ),
                Locale.forLanguageTag("vi")
        );

        assertEquals(merged, List.of("Microsoft YaHei UI"));
    }

    @Test(description = "Hộp thả xuống phông chữ chính của trình soạn thảo nên ưu tiên giữ lại các phông chữ có chiều rộng cố định phù hợp hơn cho việc chỉnh sửa mã")
    public void testMergeEditorPrimaryFamiliesForCombo_KeepsLikelyCodeFonts() {
        List<String> merged = UiFontCatalog.mergeEditorPrimaryFamiliesForCombo(
                "",
                List.of(
                        new UiFontCatalog.FontOption("Arial", UiFontCatalog.FontSupport.NO_EMOJI),
                        new UiFontCatalog.FontOption("Consolas", UiFontCatalog.FontSupport.NO_CJK),
                        new UiFontCatalog.FontOption("JetBrains Mono", UiFontCatalog.FontSupport.NO_CJK),
                        new UiFontCatalog.FontOption("PingFang SC", UiFontCatalog.FontSupport.NO_EMOJI)
                )
        );

        assertEquals(merged, List.of("Consolas", "JetBrains Mono"));
    }

    @Test(description = "Hộp thả xuống phông chữ dự phòng của trình soạn thảo sẽ ưu tiên giữ lại các phông chữ có thể hiển thị tiếng Trung")
    public void testMergeEditorFallbackFamiliesForCombo_KeepsCjkSafeFonts() {
        List<String> merged = UiFontCatalog.mergeEditorFallbackFamiliesForCombo(
                "",
                List.of(
                        new UiFontCatalog.FontOption("Consolas", UiFontCatalog.FontSupport.NO_CJK),
                        new UiFontCatalog.FontOption("Microsoft YaHei UI", UiFontCatalog.FontSupport.NO_EMOJI),
                        new UiFontCatalog.FontOption("PingFang SC", UiFontCatalog.FontSupport.FULL)
                )
        );

        assertEquals(merged, List.of("Microsoft YaHei UI", "PingFang SC"));
    }

    @Test(description = "Bộ nhớ đệm phông chữ chỉ nên kích hoạt tải nền một lần và sử dụng lại kết quả")
    public void testFontOptionCache_LoadsOnlyOnce() {
        UiFontCatalog.FontOptionCache cache = new UiFontCatalog.FontOptionCache();
        AtomicInteger loadCount = new AtomicInteger();
        List<UiFontCatalog.FontOption> loaded = new ArrayList<>(List.of(
                new UiFontCatalog.FontOption("Font A", UiFontCatalog.FontSupport.FULL)
        ));

        List<UiFontCatalog.FontOption> first = cache.getOrLoadAsync(() -> {
            loadCount.incrementAndGet();
            return loaded;
        }).join();
        List<UiFontCatalog.FontOption> second = cache.getOrLoadAsync(() -> {
            loadCount.incrementAndGet();
            return List.of(new UiFontCatalog.FontOption("Font B", UiFontCatalog.FontSupport.NO_EMOJI));
        }).join();

        assertEquals(loadCount.get(), 1);
        assertSame(first, second);
        assertSame(cache.getCachedOptions(), loaded);
    }

    /**
     * The UI is English/Vietnamese. A font without Vietnamese letters (ư, ơ, đ, ạ, ệ...) renders the Vietnamese UI as
     * boxes, so it must not be reported as UI-safe; the gate used to check only the no longer shipped Chinese locale.
     */
    @Test
    public void fontWithoutVietnameseGlyphsIsNotUiSafeForVietnameseUi() {
        java.awt.Font basicLatinOnly = fontCoveringUpTo(0x024F, false);
        java.awt.Font vietnameseCapable = fontCoveringUpTo(0x1EFF, false);

        assertEquals(UiFontCatalog.inspectFont(basicLatinOnly), UiFontCatalog.FontSupport.NO_CJK);
        assertEquals(UiFontCatalog.inspectFont(vietnameseCapable), UiFontCatalog.FontSupport.NO_EMOJI);
        assertFalse(UiFontCatalog.isUiFontAllowedForLocale(
                UiFontCatalog.inspectFont(basicLatinOnly), Locale.forLanguageTag("vi")));
    }

    /** Font stub that can display every code point up to {@code maxCodePoint} (excluding Latin Extended Additional unless reached). */
    private static java.awt.Font fontCoveringUpTo(int maxCodePoint, boolean emoji) {
        return new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 13) {
            @Override
            public int canDisplayUpTo(String text) {
                for (int i = 0; i < text.length(); ) {
                    int codePoint = text.codePointAt(i);
                    boolean supported = codePoint <= maxCodePoint || (emoji && codePoint >= 0x1F000);
                    if (!supported && !(codePoint >= 0x0300 && codePoint <= 0x036F && maxCodePoint >= 0x036F)) {
                        return i;
                    }
                    i += Character.charCount(codePoint);
                }
                return -1;
            }
        };
    }
}
