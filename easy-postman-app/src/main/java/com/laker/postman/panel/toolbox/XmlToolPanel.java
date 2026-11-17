package com.laker.postman.panel.toolbox;

import com.laker.postman.util.MessageKeys;
/** XML formatter, compressor, and syntax validator with external entities disabled. */
public class XmlToolPanel extends StructuredTextToolPanel {
    public XmlToolPanel() {
        super("XML", MessageKeys.TOOLBOX_XML_INPUT, MessageKeys.TOOLBOX_XML_VALID,
                MessageKeys.TOOLBOX_XML_INVALID, "text/xml");
    }

    @Override
    protected String formatText(String input, boolean compact) throws Exception {
        return StructuredTextFormatters.xml(input, compact);
    }

    @Override
    protected void validateText(String input) throws Exception {
        StructuredTextFormatters.validateXml(input);
    }
}
