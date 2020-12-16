/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2020 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.rpc;

import com.axelor.common.StringUtils;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.i18n.I18n;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

final class Translator {

  private Translator() {}

  private static String getTranslation(String value) {
    if (StringUtils.notBlank(value)) {
      String key = "value:" + value;
      String val = I18n.get(key);
      if (val != key) {
        return val;
      }
    }
    return value;
  }

  private static String toKey(String name) {
    return String.format("$t:%s", name);
  }

  private static boolean isTranslatable(Mapper mapper, String field) {
    Property property = null;
    Iterator<String> names = Arrays.stream(field.split("\\.")).iterator();
    while (names.hasNext()) {
      property = mapper.getProperty(names.next());
      if (property == null) return false;
      if (names.hasNext()) {
        if (property.getTarget() == null) return false;
        mapper = Mapper.of(property.getTarget());
      }
    }
    return property != null && property.isTranslatable();
  }

  static Map<String, Object> translate(Map<String, Object> values, Property property) {
    return translate(values, property.getName());
  }

  static Map<String, Object> translate(Map<String, Object> values, String name) {
    Object value = values.get(name);
    if (value instanceof String) {
      Object val = getTranslation((String) value);
      if (val != value) {
        values.put(toKey(name), val);
      }
    }
    return values;
  }

  @SuppressWarnings("all")
  static void applyTranslatables(Map<String, Object> values, Class<?> model) {
    if (values == null || values.isEmpty()) return;
    final Mapper mapper = Mapper.of(model);
    for (Property property : mapper.getProperties()) {
      final String name = property.getName();
      final Object value = values.get(name);
      if (property.isTranslatable() && value instanceof String) {
        translate(values, property.getName());
      }
      if (property.getTarget() != null && value instanceof Map) {
        applyTranslatables((Map) value, property.getTarget());
      }
      if (property.getTarget() != null && value instanceof Collection) {
        for (Object item : (Collection) value) {
          if (item instanceof Map) {
            applyTranslatables((Map) item, property.getTarget());
          }
        }
      }
    }

    // translate dotted fields
    values.keySet().stream()
        .filter(name -> name.contains("."))
        .filter(name -> isTranslatable(mapper, name))
        .collect(Collectors.toList())
        .forEach(name -> translate(values, name));
  }
}
