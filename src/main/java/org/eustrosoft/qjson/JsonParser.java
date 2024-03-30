package org.eustrosoft.qjson;

import org.eustrosoft.qjson.annotations.JsonIgnore;
import org.eustrosoft.qjson.annotations.JsonNotNull;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eustrosoft.qjson.Constants.*;

public class JsonParser {

    public Object processObjectRecursively(Object obj, String json) throws IOException, IllegalAccessException {
        Field[] classFields = getClassFields(obj.getClass());
        QJson qJson = new QJson();
        qJson.parseJSONReader(new StringReader(json));

        for (Field field : classFields) {
            field.setAccessible(true);
            Class<?> fieldType = field.getType();
            String fieldName = field.getName();
            try {
                JsonIgnore annotation = field.getAnnotation(JsonIgnore.class);
                if (annotation != null) {
                    continue;
                }
                if (Number.class.isAssignableFrom(fieldType)) {
                    field.set(obj, getRightNumber(fieldType, qJson.getItem(fieldName).toString()));
                } else if (Collection.class.isAssignableFrom(fieldType)) {
                    Collection<?> col = (Collection<?>) field.get(obj);
                    Iterator<?> iterator = col.iterator();
                    List list;
                    if (iterator.hasNext()) {
                        Object next = iterator.next();
                        Class<?> aClass = next.getClass();
                        if (String.class.isAssignableFrom(aClass)) {
                            list = new ArrayList<String>();
                            list.add(next.toString());
                            while (iterator.hasNext()) {
                                Object newObj = iterator.next();
                                list.add(newObj.toString());
                            }
                        } else if (Number.class.isAssignableFrom(aClass)) {
                            list = new ArrayList<Number>();
                            list.add(getRightNumber(aClass, next.toString()));
                            while (iterator.hasNext()) {
                                Object newObj = iterator.next();
                                list.add(getRightNumber(aClass, newObj.toString()));
                            }
                        } else {
                            list = new ArrayList<>();
                            int index = 0;
                            list.add(processObjectRecursively(next, qJson.getItemQJson(fieldName).getItem(index++).toString()));
                            while (iterator.hasNext()) {
                                Object newObj = iterator.next();
                                list.add(processObjectRecursively(newObj, qJson.getItemQJson(fieldName).getItem(index++).toString()));
                            }
                        }
                        field.set(obj, list);
                    } else {
                        if (Set.class.isAssignableFrom(fieldType)) {
                            field.set(obj, Collections.EMPTY_SET);
                        } else if (List.class.isAssignableFrom(fieldType)) {
                            field.set(obj, Collections.EMPTY_LIST);
                        } else if (Map.class.isAssignableFrom(fieldType)) {
                            field.set(obj, Collections.EMPTY_MAP);
                        }
                    }
                } else {
                    field.set(obj, fieldType.cast(qJson.getItem(fieldName)));
                }
            } catch (Exception ex) {
                processObjectRecursively(field.get(obj), qJson.getItem(fieldName).toString());
            }
        }
        return obj;
    }

    private Number getRightNumber(Class<?> fieldType, String val) {
        if (Short.class.isAssignableFrom(fieldType)) {
            return Short.parseShort(val);
        }
        if (Long.class.isAssignableFrom(fieldType)) {
            return Long.parseLong(val);
        }
        if (Integer.class.isAssignableFrom(fieldType)) {
            return Integer.parseInt(val);
        }
        if (Double.class.isAssignableFrom(fieldType)) {
            return Double.valueOf(val);
        }
        if (Float.class.isAssignableFrom(fieldType)) {
            return Float.valueOf(val);
        }
        if (BigDecimal.class.isAssignableFrom(fieldType)) {
            return BigDecimal.valueOf(Double.parseDouble(val));
        }
        if (BigInteger.class.isAssignableFrom(fieldType)) {
            return BigInteger.valueOf(Long.parseLong(val));
        }
        return 0;
    }

    public String parseObject(Object object) throws Exception {
        if (object == null) {
            return NULL;
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append(START_OBJ);
        Map<String, Object> classFields = getClassFields(new LinkedHashMap<>(), object);
        for (Map.Entry<String, Object> entry : classFields.entrySet()) {
            buffer.append(String.format("\"%s\":%s", entry.getKey(), entry.getValue()));
            buffer.append(COMMA);
        }
        String str = buffer.toString();
        if (str.lastIndexOf(COMMA) == str.length() - 1) {
            str = str.replaceAll("[,]$", END_OBJ);
        } else {
            str = str + END_OBJ;
        }
        return str;
    }

    public String parseCollection(Collection<?> collection) throws Exception {
        if (collection == null || collection.isEmpty()) {
            return EMPTY_ARR;
        }
        Object[] objects = collection.toArray();
        List<String> finalStrings = new ArrayList<>(6);
        for (Object ob : objects) {
            finalStrings.add(obj2str(ob));
        }
        return String.format("%s%s%s", START_ARR, String.join(COMMA, finalStrings), END_ARR);
    }

    private Map<String, Object> getClassFields(Map<String, Object> objMap, Object obj)
            throws Exception {
        Class<?> clazz = obj.getClass();
        JsonNotNull nonNull = clazz.getAnnotation(JsonNotNull.class);
        Field[] declaredFields = getClassFields(clazz);
        for (Field field : declaredFields) {
            JsonIgnore annotation = field.getAnnotation(JsonIgnore.class);
            if (annotation != null) {
                continue;
            }
            String fieldName = field.getName();
            field.setAccessible(true);

            Object value = null;

            if (field.getType().isEnum()) {
                try {
                    value = obj2str(field.get(obj).toString());
                } catch (Exception ignored) {}
            } else if (Collection.class.isAssignableFrom(field.getType())) {
                Collection o = (Collection) field.get(obj);
                value = parseCollection(o);
            } else if (field.getType().getSimpleName().equals(Object.class.getSimpleName())) {
                value = parseObject(field.get(obj));
            } else {
                value = obj2str(field.get(obj));
            }
            if (nonNull != null) {
                assert value != null;
                if (value.equals("null") || value.equals(EMPTY_ARR) || value.equals(EMPTY_OBJ)) {
                    continue;
                }
            }
            objMap.put(fieldName, value);
        }
        return objMap;
    }

    private String obj2str(Object obj) throws Exception {
        if (obj == null) {
            return NULL;
        }
        String finalString;
        if (obj instanceof Number) {
            finalString = getNumberValue((Number) obj);
        } else if (obj instanceof CharSequence) {
            finalString = String.format("\"%s\"", getString(obj.toString()));
        } else if (obj instanceof Boolean) {
            finalString = String.format("%s", obj.toString());
        } else {
            finalString = parseObject(obj);
        }
        return finalString;
    }

    private String getNumberValue(Number number) {
        if (number instanceof Long || number instanceof Integer || number instanceof Short) {
            return String.format("%d", number.longValue());
        }
        if (number instanceof Float || number instanceof Double) {
            return String.format("%f", number.doubleValue());
        }
        if (number instanceof Byte) {
            return String.format("%b", number.byteValue());
        }
        return String.format("%f", number.doubleValue());
    }

    private Field[] getClassFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        try {
            Class<?> cur = clazz;
            while (true) {
                fields.addAll(Arrays.asList(cur.getDeclaredFields()));
                Class<?> superclass = clazz.getSuperclass();
                if (superclass.getSimpleName().equals(cur.getSimpleName())) {
                    break;
                }
                cur = superclass;
            }
        } catch (NullPointerException ex) {
            // All classes searched
        }
        return fields.toArray(new Field[0]);
    }

    private String getString(String str) {
        if (str == null) {
            return "null";
        }
        StringBuilder buffer = new StringBuilder();
        int length = str.length();
        int i = 0;
        while (i < length) {
            char c = str.charAt(i);
            switch (c) {
                case '\n':
                    buffer.append("\\n");
                    break;
                case '\r':
                    buffer.append("\\r");
                    break;
                case '"':
                    buffer.append("\\\"");
                    break;
                case '\\':
                    buffer.append("\\\\");
                    break;
                default:
                    buffer.append(c);
            }
            i++;
        }
        return buffer.toString();
    }
}