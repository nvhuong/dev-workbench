package com.laker.postman.panel.toolbox;

import com.laker.postman.util.MessageKeys;
/** YAML formatter, compressor, and syntax validator. */
public class YamlToolPanel extends StructuredTextToolPanel {
    public YamlToolPanel() {
        super("YAML", MessageKeys.TOOLBOX_YAML_INPUT, MessageKeys.TOOLBOX_YAML_VALID,
                MessageKeys.TOOLBOX_YAML_INVALID, "text/yaml");
    }

    @Override
    protected String formatText(String input, boolean compact) throws Exception {
        return StructuredTextFormatters.yaml(input, compact);
    }

    @Override
    protected void validateText(String input) throws Exception {
        StructuredTextFormatters.validateYaml(input);
    }
}
