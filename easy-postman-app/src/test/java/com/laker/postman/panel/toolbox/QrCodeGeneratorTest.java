package com.laker.postman.panel.toolbox;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class QrCodeGeneratorTest {
    @Test
    public void generatesColoredPngReadyQrCode() throws Exception {
        Color foreground = QrCodeGenerator.parseColor("#123456ff");
        Color background = QrCodeGenerator.parseColor("#fefdfcff");
        var image = QrCodeGenerator.generate("https://it-tools.tech", foreground, background,
                ErrorCorrectionLevel.M, 360);

        assertEquals(image.getWidth(), 360);
        assertEquals(image.getHeight(), 360);
        assertEquals(image.getRGB(0, 0), background.getRGB());
        boolean hasForeground = false;
        for (int y = 0; y < image.getHeight() && !hasForeground; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) == foreground.getRGB()) {
                    hasForeground = true;
                    break;
                }
            }
        }
        assertTrue(hasForeground);

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        var decodedImage = ImageIO.read(new ByteArrayInputStream(png.toByteArray()));
        assertEquals(decodedImage.getWidth(), 360);
    }

    @Test
    public void parsesRgbAndRgbaColorsAndRejectsInvalidInputs() {
        assertEquals(QrCodeGenerator.parseColor("#abcdef"), new Color(0xab, 0xcd, 0xef));
        assertEquals(QrCodeGenerator.parseColor("#10203080"), new Color(0x10, 0x20, 0x30, 0x80));
        assertEquals(QrCodeGenerator.toRgbaHex(new Color(0x10, 0x20, 0x30, 0x80)), "#10203080");
        expectThrows(IllegalArgumentException.class, () -> QrCodeGenerator.parseColor("not-a-color"));
        expectThrows(IllegalArgumentException.class, () -> QrCodeGenerator.generate(" ", Color.BLACK,
                Color.WHITE, ErrorCorrectionLevel.M, 300));
    }
}
