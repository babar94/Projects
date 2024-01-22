package com.gateway.utils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@SuppressWarnings("restriction")
public class XMLBeautifier {

	private static final Logger LOG = LoggerFactory.getLogger(XMLBeautifier.class);

	public static String maskCCNumberCommons(String ccnum) {
		int total = ccnum.length();
		int startlen = 6, endlen = 4;
		int masklen = total - (startlen + endlen);
		String start = ccnum.substring(0, startlen);
		String end = ccnum.substring(startlen + masklen, total);
		String padded = StringUtils.rightPad(start, startlen + masklen, 'X');
		String masked = padded.concat(end);

		return masked;
	}

	public static String maskCCNumberCommons1(String ccnum) {
		int total = ccnum.length();
		int startlen = 0, endlen = 0;
		int masklen = total - (startlen + endlen);
		String start = ccnum.substring(0, startlen);
		String end = ccnum.substring(startlen + masklen, total);
		String padded = StringUtils.rightPad(start, startlen + masklen, 'X');
		String masked = padded.concat(end);

		return masked;
	}

	public static String format(String unformattedXml) {
		try {
			final Document document = parseXmlFile(unformattedXml);

			/*
			 * NodeList ns = document.getElementsByTagName("cnic"); if (ns != null) { if
			 * (ns.getLength() > 0) { Node node = ns.item(0);
			 * node.setTextContent(maskCCNumberCommons(node.getTextContent())); } }
			 */

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");

			Writer out = new StringWriter();
			transformer.transform(new DOMSource(document), new StreamResult(out));

			return out.toString();
		} catch (Exception e) {
			LOG.error("ERROR: {}", e.getMessage());
			LOG.error("OOPS", e);
			return unformattedXml;
		}
	}

	private static Document parseXmlFile(String in) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(in));
			return db.parse(is);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
