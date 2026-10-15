/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ExternalIp {

    private static final String STATE_VALUE_METHOD = "getValue";

    private static final String[] LOCATION_TEXT_FIELDS = { "country_code", "country_name", "region", "city",
            "region_name", "time_zone", "as_num", "as_name" };

    private static final String[] LOCATION_COORDINATE_FIELDS = { "latitude", "longitude" };

    final InetAddress address;

    final String hostname;

    final String countryCode;

    final String countryName;

    final String region;

    final String city;

    final String timeZone;

    final String asNumber;

    final String asName;

    final Double latitude;

    final Double longitude;

    private ExternalIp(InetAddress address, String hostname, List<Object> texts, List<Object> coordinates) {
        this.address = address;
        this.hostname = hostname;
        this.countryCode = text(texts, "country_code");
        this.countryName = text(texts, "country_name");
        this.region = text(texts, "region");
        this.city = text(texts, "city");
        this.timeZone = text(texts, "time_zone");
        this.asNumber = text(texts, "as_num");
        this.asName = text(texts, "as_name");
        this.latitude = coordinate(coordinates, "latitude");
        this.longitude = coordinate(coordinates, "longitude");
    }

    static ExternalIp read(Object stateHolder) throws ReflectiveOperationException {
        final Object state = stateHolder.getClass().getMethod(STATE_VALUE_METHOD).invoke(stateHolder);
        if (state == null) {
            return null;
        }
        InetAddress address = null;
        String hostname = null;
        Object location = null;
        for (Field field : instanceFields(state)) {
            final Object value = field.get(state);
            if (field.getType() == InetAddress.class) {
                address = (InetAddress) value;
            } else if (field.getType() == String.class) {
                hostname = (String) value;
            } else if (!field.getType().isPrimitive()) {
                location = value;
            }
        }
        return of(address, hostname, location);
    }

    static ExternalIp of(InetAddress address, String hostname, Object location) throws IllegalAccessException {
        final List<Object> texts = new ArrayList<>();
        final List<Object> coordinates = new ArrayList<>();
        if (location != null) {
            for (Field field : instanceFields(location)) {
                if (field.getType() == String.class) {
                    texts.add(field.get(location));
                } else if (field.getType() == double.class) {
                    coordinates.add(field.get(location));
                }
            }
        }
        return new ExternalIp(address, hostname, texts, coordinates);
    }

    private static List<Field> instanceFields(Object object) {
        final List<Field> fields = new ArrayList<>();
        for (Field field : object.getClass().getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                fields.add(field);
            }
        }
        Collections.sort(fields, (left, right) -> left.getName().compareTo(right.getName()));
        return fields;
    }

    private static String text(List<Object> values, String name) {
        final int index = indexOf(LOCATION_TEXT_FIELDS, name);
        return (index < values.size()) ? (String) values.get(index) : null;
    }

    private static Double coordinate(List<Object> values, String name) {
        final int index = indexOf(LOCATION_COORDINATE_FIELDS, name);
        return (index < values.size()) ? (Double) values.get(index) : null;
    }

    private static int indexOf(String[] names, String name) {
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(name)) {
                return i;
            }
        }
        throw new IllegalArgumentException(name);
    }

}
