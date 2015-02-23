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
package org.brutusin.fleadb.impl;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetField;
import org.brutusin.commons.Pair;
import org.brutusin.commons.json.ParseException;
import org.brutusin.commons.json.annotations.IndexableProperty;
import org.brutusin.commons.json.spi.JsonCodec;
import org.brutusin.commons.json.spi.JsonNode;
import org.brutusin.commons.json.spi.JsonSchema;
import org.brutusin.commons.json.util.JsonNodeVisitor;
import org.brutusin.commons.json.util.JsonSchemaUtils;
import org.brutusin.fleadb.Schema;

/**
 * Schema implementation.
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class SchemaImpl implements Schema {

    private final JsonSchema jsonSchema;
    private Map<String, JsonNode.Type> indexFields;
    private Map<String, Boolean> facetFields;

    public SchemaImpl(JsonSchema jsonSchema) throws ParseException {
        this.jsonSchema = jsonSchema;
        initFields();
    }

    @Override
    public JsonSchema getJSONSChema() {
        return this.jsonSchema;
    }

    public void setFacetFields(Map<String, Boolean> facetFields) {
        this.facetFields = facetFields;
    }

    @Override
    public Map<String, JsonNode.Type> getIndexFields() {
        return indexFields;
    }

    @Override
    public Map<String, Boolean> getFacetFields() {
       return facetFields;
    }
    
    private void initFields() {
        this.indexFields = new LinkedHashMap();
        this.facetFields = new LinkedHashMap();

        accept(jsonSchema, new JsonNodeVisitor() {

            private void add(String name, JsonNode.Type type, IndexableProperty.IndexMode mode) {
                SchemaImpl.this.indexFields.put(name, type);
                if (mode == IndexableProperty.IndexMode.facet) {
                    boolean multievaluated = name.contains("[*]") || name.contains("[#]");
                    SchemaImpl.this.facetFields.put(name, multievaluated);
                }
            }

            private void add(String name, JsonNode.Type type, IndexableProperty.IndexMode mode, boolean multievaluated) {
                SchemaImpl.this.indexFields.put(name, type);
                if (mode == IndexableProperty.IndexMode.facet) {
                    SchemaImpl.this.facetFields.put(name, multievaluated);
                }
            }

            @Override
            public void visit(String name, JsonNode schema) {
                JsonNode indexProperty = schema.get("index");
                if (indexProperty != null) {
                    final IndexableProperty.IndexMode mode = IndexableProperty.IndexMode.valueOf(indexProperty.asString());
                    JsonNode.Type type = JsonNode.Type.valueOf(schema.get("type").asString().toUpperCase());
                    if (type == JsonNode.Type.OBJECT) {
                        JsonNode.Type valueType = JsonSchemaUtils.getMapValueType(schema);
                        if (valueType != null) {
                            if (valueType != JsonNode.Type.OBJECT) {
                                add(name, JsonNode.Type.STRING, mode, true);
                                add(name + "[*]", valueType, mode);
                            } else {
                                add(name, JsonNode.Type.STRING, mode, true);
                            }
                        }
                    } else if (type == JsonNode.Type.ARRAY) {
                        JsonNode.Type valueType = JsonSchemaUtils.getArrayValueType(schema);
                        if (valueType != null) {
                            add(name + "[#]", valueType, mode);
                        }
                    } else {
                        add(name, type, mode);
                    }
                }
            }
        });

        Set<String> parentMapNames = new LinkedHashSet();
        for (Map.Entry<String, JsonNode.Type> entry : indexFields.entrySet()) {
            String name = entry.getKey();
            addParentMapNamesToList(name, parentMapNames);
        }
        for (String parentMapName : parentMapNames) {
            // Used to index map keys even if the node has not the "index" property (but an inned node does)
            if (!indexFields.containsKey(parentMapName)) {
                indexFields.put(parentMapName, JsonNode.Type.STRING);
            }
        }
    }

    private static void addParentMapNamesToList(String name, Set<String> set) {
        String[] tokens = name.split("\\[\\*\\]");
        if (tokens == null || tokens.length == 0) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) {
                sb.append("[*]");
            }
            sb.append(tokens[i]);
            set.add(sb.toString());
        }
    }

    /**
     * Starts visitor notification, from the specified schema as root schema.
     *
     * @param schema
     * @param visitor
     */
    private static void accept(JsonSchema rootSchema, JsonNodeVisitor... visitors) {
        accept("$", rootSchema, visitors);
    }

    /**
     *
     * Helper method for recursive processing
     *
     * @param name
     * @param schema
     * @param visitor
     */
    private static void accept(String name, JsonNode schema, JsonNodeVisitor... visitors) {
        if (schema == null) {
            return;
        }
        if (schema.getNodeType() == JsonNode.Type.OBJECT) {
            JsonNode typeNode = schema.get("type");
            if (typeNode != null && typeNode.getNodeType() == JsonNode.Type.STRING) {
                String type = typeNode.asString();
                for (int i = 0; i < visitors.length; i++) {
                    JsonNodeVisitor visitor = visitors[i];
                    visitor.visit(name, schema);
                }
                if ("object".equals(type)) {
                    JsonNode propertiesNode = schema.get("properties");
                    if (propertiesNode != null && propertiesNode.getNodeType() == JsonNode.Type.OBJECT) {
                        Iterator<String> ps = propertiesNode.getProperties();
                        while (ps.hasNext()) {
                            String prop = ps.next();
                            accept(name + (name.isEmpty() ? "" : ".") + prop, propertiesNode.get(prop), visitors);
                        }
                    }
                    JsonNode additionalPropertiesNode = schema.get("additionalProperties");
                    if (additionalPropertiesNode != null && additionalPropertiesNode.getNodeType() == JsonNode.Type.OBJECT) {
                        accept(name + "[*]", additionalPropertiesNode, visitors);
                    }
                } else if ("array".equals(type)) {
                    JsonNode itemsNode = schema.get("items");
                    if (itemsNode != null && itemsNode.getNodeType() == JsonNode.Type.OBJECT) {
                        accept(name + "[#]", itemsNode, visitors);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // SchemaImpl s = new SchemaImpl("{\"type\":\"object\",\"properties\":{\"map\":{\"type\":\"object\",\"index\":\"index\",\"additionalProperties\":{\"type\":\"boolean\"}},\"firstName\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{\"aaa\":{\"type\":\"string\"},\"bbb\":{\"type\":\"object\",\"additionalProperties\":{\"type\":\"string\",\"index\":\"facet\"}}}}},\"middleName\":{\"type\":\"string\",\"index\":\"index\"},\"lastName\":{\"type\":\"string\"}},\"required\":[\"firstName\",\"lastName\"],\"additionalProperties\":false}");
        SchemaImpl s = new SchemaImpl(JsonCodec.getInstance().parseSchema(JsonCodec.getInstance().getSchemaString(TestClass.class)));
        System.out.println(s.getJSONSChema());
        System.out.println("Index:");
        Map<String, JsonNode.Type> indexFields1 = s.getIndexFields();
        for (Map.Entry<String, JsonNode.Type> entry
                : indexFields1.entrySet()) {
            String name = entry.getKey();
            JsonNode.Type type = entry.getValue();
            System.out.println(name + ":" + type);
        }
        System.out.println("Facet:");
        Map<String, Boolean> facetFields1 = s.getFacetFields();
        System.out.println(facetFields1);
        System.out.println("---");
        JsonNode node = JsonCodec.getInstance().parse("{\"map\":{\"aa\":{\"int1\":3,\"booleanMap\":{\"key1\":true},\"s1\":\"s11Value\",\"s2\":[\"s2Value11\",\"s2Value21\"]},\"bb\":{\"booleanMap\":{\"key2\":false},\"s1\":\"s12Value\",\"s2\":[\"s2Value12\",\"s2Value22\"]}}}{\"map\":{\"aa\":{\"booleanMap\":{\"key1\":true},\"s1\":\"s11Value\",\"s2\":[\"s2Value11\",\"s2Value21\"]},\"bb\":{\"booleanMap\":{\"key2\":false},\"s1\":\"s12Value\",\"s2\":[\"s2Value12\",\"s2Value22\"]}}}");
        GenericTransformer transformer = new GenericTransformer(s);
        Pair<Document, List<FacetField>> entityToDocument = transformer.entityToDocument(node);
        System.out.println(entityToDocument);
    }

    static class Class2 {

        private String s1;
        @IndexableProperty
        private String[] s2;
        @IndexableProperty(mode = IndexableProperty.IndexMode.facet)
        private Map<String, Boolean> booleanMap;

        @IndexableProperty
        private int int1;

        public String getS1() {
            return s1;
        }

        public void setS1(String s1) {
            this.s1 = s1;
        }

        public String[] getS2() {
            return s2;
        }

        public void setS2(String[] s2) {
            this.s2 = s2;
        }

        public Map<String, Boolean> getBooleanMap() {
            return booleanMap;
        }

        public void setBooleanMap(Map<String, Boolean> booleanMap) {
            this.booleanMap = booleanMap;
        }

        public int getInt1() {
            return int1;
        }

        public void setInt1(int int1) {
            this.int1 = int1;
        }
    }

    static class TestClass {

        private Map<String, Class2> map;

        public Map<String, Class2> getMap() {
            return map;
        }

        public void setMap(Map<String, Class2> map) {
            this.map = map;
        }
    }
}
