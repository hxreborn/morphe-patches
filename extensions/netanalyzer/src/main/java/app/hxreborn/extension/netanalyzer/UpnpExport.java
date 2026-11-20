/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
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
