package org.bstats.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonObjectBuilderTest {

    @Test
    void buildComplexObjects() {
        String actual = new JsonObjectBuilder()
                .appendField("str", "Hello world")
                .appendField("int", 42)
                .appendField("strArray", new String[]{"a", "bb", "ccc", ""})
                .appendField("intArray", new int[]{1, 2, 3, 4})
                .appendField("objArray", new JsonObjectBuilder.JsonObject[]{
                        new JsonObjectBuilder().appendField("hello", "world").build(),
                        new JsonObjectBuilder().appendField("answer", 42).build()
                })
                .appendNull("null")
                .appendField("obj", new JsonObjectBuilder().appendField("hello", "world").build())
                .build()
                .toString();
        String expected = "{" +
                "\"str\":\"Hello world\"," +
                "\"int\":42," +
                "\"strArray\":[\"a\",\"bb\",\"ccc\",\"\"]," +
                "\"intArray\":[1,2,3,4]," +
                "\"objArray\":[{\"hello\":\"world\"},{\"answer\":42}]," +
                "\"null\":null," +
                "\"obj\":{\"hello\":\"world\"}" +
                "}";
        assertEquals(expected, actual);
    }

    @Test
    void notEscapeKeyIfNotNecessary() {
        String actual = new JsonObjectBuilder()
                .appendNull("ok")
                .build()
                .toString();
        String expected = "{\"ok\":null}";
        assertEquals(expected, actual);
    }

    @Test
    void escapeKeyIfNecessary() {
        String actual = new JsonObjectBuilder()
                .appendNull("ab/cd\"\n\r\\\u001B\u0011")
                .build()
                .toString();
        String expected = "{\"ab/cd\\\"\\u000a\\u000d\\\\\\u001b\\u0011\":null}";
        assertEquals(expected, actual);
    }

    @Test
    void escapeStringValue() {
        String result = new JsonObjectBuilder()
                .appendField("str", "ab/cd\"\n\r\\\u001B\u0011")
                .build()
                .toString();
        String expected = "{\"str\":\"ab/cd\\\"\\u000a\\u000d\\\\\\u001b\\u0011\"}";
        assertEquals(expected, result);
    }

    @Test
    void preventBuilderReuse() {
        JsonObjectBuilder builder = new JsonObjectBuilder()
                .appendField("key", "value");
        assertDoesNotThrow(() -> builder.appendField("answer", 42));
        assertDoesNotThrow(builder::build);
        assertThrows(IllegalStateException.class, () -> builder.appendField("answer", 42));
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void appendNullWorks() {
        String actual = new JsonObjectBuilder()
                .appendNull("key")
                .build()
                .toString();
        String expected = "{\"key\":null}";
        assertEquals(expected, actual);
    }

    @Test
    void appendNullValidatesArg() {
        assertThrows(IllegalArgumentException.class, () -> new JsonObjectBuilder().appendNull(null));
    }

    @Test
    void appendStringFieldWorks() {
        String actual = new JsonObjectBuilder()
                .appendField("key", "value")
                .build()
                .toString();
        String expected = "{\"key\":\"value\"}";
        assertEquals(expected, actual);
    }

    @Test
    void appendStringFieldValidatesArgs() {
        assertThrows(IllegalArgumentException.class, () -> new JsonObjectBuilder().appendField(null, "value"));
        assertThrows(IllegalArgumentException.class, () -> new JsonObjectBuilder().appendField("key", (String) null));
        assertThrows(IllegalArgumentException.class, () -> new JsonObjectBuilder().appendField(null, (String) null));
    }

    @Test
    void appendIntFieldWorks() {
        String actual = new JsonObjectBuilder()
                .appendField("key", 42)
                .build()
                .toString();
        String expected = "{\"key\":42}";
        assertEquals(expected, actual);
    }

    @Test
    void appendIntFieldValidatesArgs() {
        assertThrows(IllegalArgumentException.class, () -> new JsonObjectBuilder().appendField(null, 42));
    }

    @Test
    void appendObjectFieldWorks() {
        JsonObjectBuilder.JsonObject simpleObject = new JsonObjectBuilder()
                .appendField("answer", 42)
                .build();

        String actual = new JsonObjectBuilder()
                .appendField("obj", simpleObject)
                .build()
                .toString();
        String expected = "{\"obj\":{\"answer\":42}}";
        assertEquals(expected, actual);
    }

    @Test
    void appendObjectFieldValidatesArgs() {
        JsonObjectBuilder.JsonObject simpleObject = new JsonObjectBuilder()
                .appendField("answer", 42)
                .build();

        assertThrows(IllegalArgumentException.class, () -> new JsonObjectBuilder().appendField(null, simpleObject));
        assertThrows(IllegalArgumentException.class,
                () -> new JsonObjectBuilder().appendField("key", (JsonObjectBuilder.JsonObject) null));
        assertThrows(IllegalArgumentException.class,
                () -> new JsonObjectBuilder().appendField(null, (JsonObjectBuilder.JsonObject) null));
    }

    @Test
    void appendStringArrayFieldWorks() {
        String actual = new JsonObjectBuilder()
                .appendField("key", new String[]{"a", "b", "c"})
                .build()
                .toString();
        String expected = "{\"key\":[\"a\",\"b\",\"c\"]}";
        assertEquals(expected, actual);
    }

    @Test
    void appendStringArrayFieldValidatesArgs() {
        assertThrows(IllegalArgumentException.class,
                () -> new JsonObjectBuilder().appendField(null, new String[]{"a", "b", "c"}));
        assertThrows(IllegalArgumentException.class,
                () -> new JsonObjectBuilder().appendField("key", (String[]) null));
        assertThrows(IllegalArgumentException.class,
                () -> new JsonObjectBuilder().appendField(null, (String[]) null));
    }

    @Test
    void appendIntArrayFieldWorks() {
        String actual = new JsonObjectBuilder()
                .appendField("key", new int[]{1, 2, 3})
                .build()
                .toString();
        String expected = "{\"key\":[1,2,3]}";
        assertEquals(expected, actual);
    }

    @Test
    void appendIntArrayFieldValidatesArgs() {
        assertThrows(IllegalArgumentException.class,
                () -> new JsonObjectBuilder().appendField(null, new int[]{1, 2, 3}));
        assertThrows(IllegalArgumentException.class,
                () -> new JsonObjectBuilder().appendField("key", (int[]) null));
        assertThrows(IllegalArgumentException.class,
                () -> new JsonObjectBuilder().appendField(null, (int[]) null));
    }


    @Test
    void appendObjectArrayFieldWorks() {
        String actual = new JsonObjectBuilder()
                .appendField("key", new JsonObjectBuilder.JsonObject[]{
                        new JsonObjectBuilder()
                                .build(),
                        new JsonObjectBuilder()
                                .appendField("answer", 42)
                                .build()
                })
                .build()
                .toString();
        String expected = "{\"key\":[{},{\"answer\":42}]}";
        assertEquals(expected, actual);
    }

    @Test
    void appendObjectArrayFieldValidatesArgs() {
        assertThrows(IllegalArgumentException.class,
                () -> new JsonObjectBuilder().appendField(null, new JsonObjectBuilder.JsonObject[]{
                        new JsonObjectBuilder()
                                .build(),
                        new JsonObjectBuilder()
                                .appendField("answer", 42)
                                .build()
                }));
        assertThrows(IllegalArgumentException.class,
                () -> new JsonObjectBuilder().appendField("key", (JsonObjectBuilder.JsonObject[]) null));
        assertThrows(IllegalArgumentException.class,
                () -> new JsonObjectBuilder().appendField(null, (JsonObjectBuilder.JsonObject[]) null));
    }
}