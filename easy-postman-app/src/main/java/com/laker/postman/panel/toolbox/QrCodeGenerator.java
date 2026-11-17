package com.laker.postman.panel.toolbox;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/** Generates standalone QR images without network access. */
final class QrCodeGenerator {
    private static final int QUIET_ZONE = 4;

    private QrCodeGenerator() { }

    static BufferedImage generate(String text, Color foreground, Color background,
                                  ErrorCorrectionLevel errorCorrection, int size) throws Exception {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("QR text is empty");
        if (size < 64) throw new IllegalArgumentException("Image size must be at least 64 pixels");
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION,
                errorCorrection == null ? ErrorCorrectionLevel.M : errorCorrection);
        hints.put(EncodeHintType.MARGIN, QUIET_ZONE);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);
        BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int foregroundArgb = foreground.getRGB();
        int backgroundArgb = background.getRGB();
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                image.setRGB(x, y, matrix.get(x, y) ? foregroundArgb : backgroundArgb);
            }
        }
        return image;
    }

    static Color parseColor(String value) {
        if (value == null) throw new IllegalArgumentException("Color is empty");
        String hex = value.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() != 6 && hex.length() != 8) {
            throw new IllegalArgumentException("Expected #RRGGBB or #RRGGBBAA");
        }
        try {
            long rgba = Long.parseLong(hex, 16);
            if (hex.length() == 6) return new Color((int) rgba);
            return new Color((int) ((rgba >> 24) & 0xff), (int) ((rgba >> 16) & 0xff),
                    (int) ((rgba >> 8) & 0xff), (int) (rgba & 0xff));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Color must contain hexadecimal digits", exception);
        }
    }

    static String toRgbaHex(Color color) {
        return String.format(java.util.Locale.ROOT, "#%02x%02x%02x%02x",
                color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }
}
