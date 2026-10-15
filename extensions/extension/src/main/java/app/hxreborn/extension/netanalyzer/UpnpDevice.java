/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

final class UpnpDevice {

    private static final String DOCUMENT_TYPE_DECLARATION = "<!DOCTYPE";

    private static final String ELEMENT_DEVICE = "device";

    private static final String ELEMENT_SERVICE_LIST = "serviceList";

    private static final String ELEMENT_DEVICE_LIST = "deviceList";

    private static final String KEY_UDN = "UDN";

    private static final String KEY_FRIENDLY_NAME = "friendlyName";

    private static final String KEY_DEVICE_TYPE = "deviceType";

    private static final String KEY_SERVICE_TYPE = "serviceType";

    private static final List<String> DEVICE_ORDER = Arrays.asList(KEY_DEVICE_TYPE, KEY_FRIENDLY_NAME, "manufacturer",
            "manufacturerURL", "modelDescription", "modelName", "modelNumber", "modelURL", "serialNumber", "UDN", "UPC",
            "presentationURL");

    private static final List<String> SERVICE_ORDER = Arrays.asList("serviceId", "SCPDURL", "controlURL",
            "eventSubURL");

    final String friendlyName;

    final String deviceType;

    final InetAddress address;

    final Map<String, String> props;

    final List<UpnpService> services;

    final List<UpnpDevice> devices;

    private UpnpDevice(String friendlyName, String deviceType, InetAddress address, Map<String, String> props,
            List<UpnpService> services, List<UpnpDevice> devices) {
        this.friendlyName = friendlyName;
        this.deviceType = deviceType;
        this.address = address;
        this.props = props;
        this.services = services;
        this.devices = devices;
    }

    static UpnpDevice parse(String xml, InetAddress address) {
        if (xml.contains(DOCUMENT_TYPE_DECLARATION)) {
            return null;
        }
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setExpandEntityReferences(false);
            final Element root = factory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)))
                .getDocumentElement();
            for (Element child : elements(root)) {
                if (ELEMENT_DEVICE.equals(child.getNodeName())) {
                    return parseDevice(child, address);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            return null;
        }
        return null;
    }

    private static UpnpDevice parseDevice(Element element, InetAddress address) {
        final Map<String, String> values = new HashMap<>();
        final List<UpnpService> services = new ArrayList<>();
        final List<UpnpDevice> devices = new ArrayList<>();
        for (Element child : elements(element)) {
            final String name = child.getNodeName();
            if (ELEMENT_SERVICE_LIST.equals(name)) {
                for (Element serviceElement : elements(child)) {
                    final UpnpService service = parseService(serviceElement, address);
                    if (service != null) {
                        services.add(service);
                    }
                }
            } else if (ELEMENT_DEVICE_LIST.equals(name)) {
                for (Element deviceElement : elements(child)) {
                    final UpnpDevice device = parseDevice(deviceElement, address);
                    if (device != null) {
                        devices.add(device);
                    }
                }
            } else {
                final String text = singleText(child);
                if (text != null) {
                    values.put(name, text);
                }
            }
        }
        final String friendlyName = values.get(KEY_FRIENDLY_NAME);
        final String deviceType = values.get(KEY_DEVICE_TYPE);
        if (values.get(KEY_UDN) == null || friendlyName == null || deviceType == null) {
            return null;
        }
        return new UpnpDevice(friendlyName, deviceType, address, ordered(values, DEVICE_ORDER), services, devices);
    }

    private static UpnpService parseService(Element element, InetAddress address) {
        final Map<String, String> values = new HashMap<>();
        for (Element child : elements(element)) {
            final String text = singleText(child);
            if (text != null) {
                values.put(child.getNodeName(), text);
            }
        }
        final String serviceType = values.remove(KEY_SERVICE_TYPE);
        if (serviceType == null) {
            return null;
        }
        return new UpnpService(serviceType, address, ordered(values, SERVICE_ORDER));
    }

    private static Map<String, String> ordered(Map<String, String> values, List<String> priority) {
        final Map<String, String> result = new LinkedHashMap<>();
        for (String key : priority) {
            if (values.containsKey(key)) {
                result.put(key, values.get(key));
            }
        }
        result.putAll(new TreeMap<>(values));
        return result;
    }

    private static List<Element> elements(Element parent) {
        final List<Element> result = new ArrayList<>();
        final NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                result.add((Element) nodes.item(i));
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static String singleText(Element element) {
        String text = null;
        final NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            final Node node = nodes.item(i);
            if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                if (text != null) {
                    return null;
                }
                text = node.getNodeValue();
            }
        }
        if (text == null || text.trim().length() == 0) {
            return null;
        }
        return text.trim();
    }

}
