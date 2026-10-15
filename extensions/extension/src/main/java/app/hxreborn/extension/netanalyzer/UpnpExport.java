/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.util.List;
import java.util.Map;

import android.content.Context;

final class UpnpExport implements ExportSource {

    private final List<UpnpDevice> devices;

    UpnpExport(List<UpnpDevice> devices) {
        this.devices = devices;
    }

    @Override
    public String subject(Context context) {
        return "UPnP Services of '" + ConnectedNetwork.ssid(context) + "'";
    }

    @Override
    public String text(Context context) {
        final ExportOutline outline = new ExportOutline();
        for (UpnpDevice device : this.devices) {
            writeDevice(outline, device);
        }
        return outline.toString();
    }

    private static void writeDevice(ExportOutline outline, UpnpDevice device) {
        outline.title(device.friendlyName);
        outline.indent(ExportOutline.INDENT_STEP);
        outline.line("Address", device.address);
        writeValues(outline, device.props);
        if (!device.services.isEmpty()) {
            outline.title("Services");
            for (UpnpService service : device.services) {
                outline.indent(ExportOutline.INDENT_STEP);
                outline.title(service.serviceType);
                outline.indent(ExportOutline.INDENT_STEP);
                outline.line("Address", service.address);
                writeValues(outline, service.props);
                outline.indent(-2 * ExportOutline.INDENT_STEP);
            }
        }
        if (!device.devices.isEmpty()) {
            outline.title("Devices");
            for (UpnpDevice child : device.devices) {
                outline.indent(ExportOutline.INDENT_STEP);
                writeDevice(outline, child);
                outline.indent(-ExportOutline.INDENT_STEP);
            }
        }
        outline.indent(-ExportOutline.INDENT_STEP);
    }

    private static void writeValues(ExportOutline outline, Map<String, String> values) {
        boolean first = true;
        for (Map.Entry<String, String> value : values.entrySet()) {
            outline.line(value.getKey(), value.getValue(), first);
            first = false;
        }
    }

}
