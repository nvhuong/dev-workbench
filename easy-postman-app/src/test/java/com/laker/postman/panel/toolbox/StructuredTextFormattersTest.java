package com.laker.postman.panel.toolbox;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class StructuredTextFormattersTest {
    @Test
    public void yamlFormatsCompressesAndValidatesDocuments() throws Exception {
        String input = "service:\n  name: api\n  instances: 2\n";
        String formatted = StructuredTextFormatters.yaml(input, false);
        String compact = StructuredTextFormatters.yaml(input, true);

        assertTrue(formatted.contains("service:"));
        assertTrue(formatted.contains("instances: 2"));
        assertFalse(compact.contains("\n"));
        StructuredTextFormatters.validateYaml(input);
        expectThrows(Exception.class, () -> StructuredTextFormatters.validateYaml("service: [unterminated"));
    }

    @Test
    public void xmlFormatsAndCompressesWithoutAllowingExternalEntities() throws Exception {
        String formatted = StructuredTextFormatters.xml("<root><item>value</item></root>", false);
        String compact = StructuredTextFormatters.xml("<root> <item>value</item> </root>", true);

        assertTrue(formatted.contains("\n"));
        assertTrue(formatted.contains("<item>value</item>"));
        assertFalse(compact.contains("\n"));
        StructuredTextFormatters.validateXml("<root><item/></root>");
        expectThrows(Exception.class, () -> StructuredTextFormatters.validateXml("<root>"));
        expectThrows(Exception.class, () -> StructuredTextFormatters.validateXml(
                "<!DOCTYPE root [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><root>&x;</root>"));
    }
}
