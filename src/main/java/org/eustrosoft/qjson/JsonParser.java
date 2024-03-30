package org.eustrosoft.qjson;

import org.eustrosoft.qjson.annotations.JsonIgnore;
import org.eustrosoft.qjson.annotations.JsonNotNull;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.eustrosoft.qjson.Constants.COMMA;
import static org.eustrosoft.qjson.Constants.EMPTY_ARR;
import static org.eustrosoft.qjson.Constants.EMPTY_OBJ;
import static org.eustrosoft.qjson.Constants.END_ARR;
import static org.eustrosoft.qjson.Constants.END_OBJ;
import static org.eustrosoft.qjson.Constants.NULL;
import static org.eustrosoft.qjson.Constants.START_ARR;
import static org.eustrosoft.qjson.Constants.START_OBJ;

public class JsonParser {

    public <T> T parseJson(Class<T> obj, String json) throws IllegalAccessException, IOException, InstantiationException {
        T object = obj.newInstance();
        return processJsonRecursively(object, json);
    }

    public <T> T processJsonRecursively(T obj, String json) throws IOException, IllegalAccessException, InstantiationException {
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
                processField(obj, qJson, field, fieldType, fieldName);
            } catch (Exception ex) {
                parseJson(field.get(obj).getClass(), qJson.getItem(fieldName).toString());
            }
        }
        return (T) obj;
    }

    private <T> void processField(T obj, QJson qJson, Field field,
                                  Class<?> fieldType, String fieldName
    ) throws IllegalAccessException {
        if (field == null || qJson == null) {
            return;
        }
        if (Number.class.isAssignableFrom(fieldType)) {
            field.set(obj, getRightNumber(fieldType, toStr(getQJsonValueOrNull(qJson, fieldName))));
        } else if (Collection.class.isAssignableFrom(fieldType)) {
            Collection collection = null;
            if (Set.class.isAssignableFrom(fieldType)) {
                collection = new HashSet();
            } else if (List.class.isAssignableFrom(fieldType)) {
                collection = new ArrayList();
            }
            QJson colJson = qJson.getItemQJson(fieldName);
            int index = 0;
            try {
                while (true) {
                    Object item = colJson.getItem(index);
                    if (item instanceof CharSequence) {
                        item = "\"" + item + "\"";
                    }
                    if (!isSimpleType(item)) {
                        try {
                            if (!isSimpleType(field.getGenericType())) {
                                item = processJsonRecursively(
                                        ((Class<?>) (((ParameterizedTypeImpl) field.getGenericType()).getActualTypeArguments()[0])).newInstance(),
                                        item.toString()
                                );
                            }
                        } catch (Exception ex) {
                            item = processJsonRecursively(collection, item.toString());
                        }
                    }
                    collection.add(item);
                    index++;
                }
            } catch (Exception exception) {
                // ignore
            }
            field.set(obj, collection);
        } else if (Map.class.isAssignableFrom(fieldType)) {
            Map map = null;
            if (LinkedHashMap.class.isAssignableFrom(fieldType)) {
                map = new LinkedHashMap();
            } else {
                map = new HashMap();
            }
            int index = 0;
            QJson mapJson = qJson.getItemQJson(fieldName);
            try {
                while (true) {
                    Object item = mapJson.getItem(index);
                    if (item instanceof CharSequence) {
                        item = "\"" + item + "\"";
                    }
                    if (!isSimpleType(item)) {
                        item = processJsonRecursively(fieldType.newInstance(), mapJson.getItem(index).toString());
                    }
                    map.putIfAbsent(mapJson.getItemName(index), item);
                    index++;
                }
            } catch (Exception exception) {
                // ignore
            }
            field.set(obj, map);
        } else {
            try {
                Object item = null;
                try {
                    item = qJson.getItem(fieldName);
                    if (isSimpleType(item)) {
                        field.set(obj, fieldType.cast(item));
                    } else {
                        field.set(obj, processJsonRecursively(fieldType.newInstance(), qJson.getItem(fieldName).toString()));
                    }
                } catch (Exception ex) {
                    // value is null or empty
                }
            } catch (Exception ex) {
                // skip
            }
        }
    }

    private String toStr(Object value) {
        if (Objects.isNull(value)) {
            return null;
        }
        return value.toString();
    }

    private Object getQJsonValueOrNull(QJson qJson, String paramName) {
        try {
            return qJson.getItem(paramName);
        } catch (Exception ex) {
            return null;
        }
    }

    private Number getRightNumber(Class<?> fieldType, String val) {
        if (val == null) {
            return null;
        }
        if (Short.class.isAssignableFrom(fieldType) || short.class.isAssignableFrom(fieldType)) {
            return Short.parseShort(val);
        }
        if (Long.class.isAssignableFrom(fieldType) || long.class.isAssignableFrom(fieldType)) {
            return Long.parseLong(val);
        }
        if (Integer.class.isAssignableFrom(fieldType) || int.class.isAssignableFrom(fieldType)) {
            return Integer.parseInt(val);
        }
        if (Double.class.isAssignableFrom(fieldType) || double.class.isAssignableFrom(fieldType)) {
            return Double.valueOf(val);
        }
        if (Float.class.isAssignableFrom(fieldType) || float.class.isAssignableFrom(fieldType)) {
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

    private boolean isSimpleType(Object obj) {
        Class<?> aClass = obj.getClass();
        return Number.class.isAssignableFrom(aClass) ||
                CharSequence.class.isAssignableFrom(aClass) ||
                Boolean.class.isAssignableFrom(aClass);
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