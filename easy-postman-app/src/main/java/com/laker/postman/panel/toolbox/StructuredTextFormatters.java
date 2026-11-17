package com.laker.postman.panel.toolbox;

import com.laker.postman.util.JsonUtil;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;

/** Pure format operations shared by the YAML and XML toolbox panels. */
final class StructuredTextFormatters {
    private static final YAMLMapper YAML = YAMLMapper.builder().build();

    private StructuredTextFormatters() { }

    static String yaml(String input, boolean compact) throws Exception {
        JsonNode document = YAML.readTree(input);
        if (document == null) throw new IllegalArgumentException("Empty YAML document");
        // JSON is a valid YAML flow document and keeps nested YAML compact on one line.
        return compact ? JsonUtil.toJsonStr(document)
                : YAML.writerWithDefaultPrettyPrinter().writeValueAsString(document);
    }

    static void validateYaml(String input) throws Exception {
        if (YAML.readTree(input) == null) throw new IllegalArgumentException("Empty YAML document");
    }

    static String xml(String input, boolean compact) throws Exception {
        Document document = parseXml(input);
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        var transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, compact ? "no" : "yes");
        if (!compact) transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter output = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(output));
        return output.toString();
    }

    static void validateXml(String input) throws Exception {
        parseXml(input);
    }

    private static Document parseXml(String input) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override public void warning(org.xml.sax.SAXParseException exception) throws org.xml.sax.SAXException {
                throw exception;
            }
            @Override public void error(org.xml.sax.SAXParseException exception) throws org.xml.sax.SAXException {
                throw exception;
            }
            @Override public void fatalError(org.xml.sax.SAXParseException exception) throws org.xml.sax.SAXException {
                throw exception;
            }
        });
        builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
        return builder.parse(new InputSource(new StringReader(input)));
    }
}
