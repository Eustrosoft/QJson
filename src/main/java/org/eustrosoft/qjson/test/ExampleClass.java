package org.eustrosoft.qjson.test;

import java.util.List;
import java.util.Map;

public class ExampleClass {
    private String stringVal;
    private Integer intVal;
    private int intVal2;
    private Float aFloat;
    private List<String> strings;
    private List<Integer> integers;
    private Example2Class example2Class;
    private Map<String, Integer> map;
    private List<ExampleClass> exampleClasses;


    @Override
    public String toString() {
        return "{\"stringVal\" : " + (stringVal == null ? null : "\"" + stringVal + "\"") + ",\"intVal\" : "
                + intVal + ",\"intVal2\" : " + intVal2 + ",\"aFloat\" : " + aFloat + ",\"strings\" : "
                + (strings == null ? null : strings) + ",\"integers\" : " + (integers == null ? null : integers)
                + ",\"example2Class\" : " + (example2Class == null ? null : example2Class) +  "}";
    }


    public static class Example2Class {
        private String strName;
        private Integer intName;
        private List<Integer> integers;


        @Override
        public String toString() {
            return "{\"strName\" : " + (strName == null ? null : "\"" + strName + "\"") + ",\"intName\" : " + intName + ",\"integers\" : " + (integers == null ? null : integers) + "}";
        }
    }
}
