package com.nvhuong.devworkbench.util;

import lombok.experimental.UtilityClass;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import org.xml.sax.InputSource;

@UtilityClass
public class XmlUtil {
    /**
     * Xác định xem chuỗi có hợp pháp hay không XML
     */
    public static boolean isXml(String str) {
        if (str == null || str.trim().isEmpty()) return false;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new InputSource(new StringReader(str)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Định dạng XML Chuỗi để tránh chèn nhiều lần các dòng trống
     */
    public static String formatXml(String xml) {
        if (!isXml(xml)) return xml;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            // Loại bỏ các dòng trống thừa
            String result = writer.toString().replaceAll("(?m)^\s*$", "");
            return result.trim();
        } catch (Exception e) {
            return xml;
        }
    }
}

