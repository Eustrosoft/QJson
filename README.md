## QJson project to process and parse json

### There are 2 main classes to interact with:

- QJson - to create json and get elements from json
- JsonParser - parser for json, easier to understand and interact with

*There are 2 methods to process json*
- parseObject(Object) to process json with reflection java, supported annotations from `annotations` package (same for parseCollection(List<?>) to parse collection of objects)
- processObjectRecursively(Object obj, String json) to parse json to object, the first parameter is .class for object
