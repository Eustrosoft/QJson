package org.eustrosoft.qjson.test;

import org.eustrosoft.qjson.JsonParser;

import java.io.IOException;

public class Example {

    public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException {
        JsonParser parser = new JsonParser();
        String json = "{\"stringVal\":\"\\\"\\\"\\\\example String 12312\", \"intVal\": 123, \"intVal2\":232, " +
                "\"aFloat\":2342.23, \"strings\":[\"123\", \"434hello1231\", \"\"], \"integers\":[123, 444, 2]," +
                "\"map\": {\"str123\": 23, \"str321\":43}," +
                "\"exampleClasses\":[{\"stringVal\":\"123123Hhehehe\"}], \"example2Class\":{\"strName\":\"123123\", \"intName\": 1231, \"integers\":[23,435,54]}}";
        System.out.println(json);
        long l = System.currentTimeMillis();
        ExampleClass classFromJson = parser.parseJson(ExampleClass.class, json);
        System.out.println("Parse time: " + (System.currentTimeMillis() - l));
        System.out.println(classFromJson.toString());
        System.out.println("Parsed!");
    }
}
