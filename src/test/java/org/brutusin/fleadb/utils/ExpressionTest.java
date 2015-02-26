/*
 * Copyright 2015 Ignacio del Valle Alles idelvall@brutusin.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.brutusin.fleadb.utils;

import org.brutusin.commons.json.ParseException;
import org.brutusin.commons.json.spi.JsonCodec;
import org.brutusin.commons.json.spi.JsonNode;
import org.brutusin.commons.json.spi.JsonSchema;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class ExpressionTest {

    private static JsonNode node;
    private static JsonSchema schemaNode;

    @BeforeClass
    public static void setUpClass() throws ParseException {
        node = JsonCodec.getInstance().parse("{\"map\":{\"aa\":{\"booleanMap\":{\"key1\":true},\"s1\":\"s11Value\",\"s2\":[\"s2Value11\",\"s2Value21\"]},\"bb\":{\"booleanMap\":{\"key2\":false},\"s1\":\"s12Value\",\"s2\":[\"s2Value12\",\"s2Value22\"]}}}{\"map\":{\"aa\":{\"booleanMap\":{\"key1\":true},\"s1\":\"s11Value\",\"s2\":[\"s2Value11\",\"s2Value21\"]},\"bb\":{\"booleanMap\":{\"key2\":false},\"s1\":\"s12Value\",\"s2\":[\"s2Value12\",\"s2Value22\"]}}}");
        schemaNode = JsonCodec.getInstance().parseSchema("{\"$schema\":\"http://json-schema.org/draft-03/schema#\",\"type\":\"object\",\"id\":\"urn:jsonschema:org:brutusin:fleadb:impl:SchemaImpl:TestClass\",\"properties\":{\"map\":{\"type\":\"object\",\"title\":\"map\",\"additionalProperties\":{\"type\":\"object\",\"id\":\"urn:jsonschema:org:brutusin:fleadb:impl:SchemaImpl:Class2\",\"properties\":{\"booleanMap\":{\"type\":\"object\",\"title\":\"booleanMap\",\"additionalProperties\":{\"type\":\"boolean\"},\"index\":\"facet\"},\"s1\":{\"type\":\"string\",\"title\":\"s1\"},\"s2\":{\"type\":\"array\",\"title\":\"s2\",\"items\":{\"type\":\"string\"},\"index\":\"index\"}}}}}}");

    }

    @Test
    public void testValidExpressions() throws ParseException {
        String[] ss = new String[]{
            "$",
            "$.map",
            "$.map[*]",
            "$.map[*].booleanMap",
            "$.map[*].booleanMap[*]",
            "$.map[*].s2[#]",
            "$.map[*].s2[0]",
            "$.map[*].s2[$]"};
        for (int i = 0; i < ss.length; i++) {
            String s = ss[i];
            Expression exp = Expression.compile(s);
            JsonNode projectNode = exp.projectNode(node);
            JsonSchema projectSchema = exp.projectSchema(schemaNode);
            assertNotNull(projectNode);
            assertNotNull(projectSchema);
            System.out.println(s + "\t" + projectNode + "\t" + projectSchema);
        }
    }

    @Test
    public void testInvalidExpression() throws ParseException {
        String s = "$.map2";
        Expression exp = Expression.compile(s);
        assertNull(exp.projectNode(node));
        assertNull(exp.projectSchema(schemaNode));
    }
}
