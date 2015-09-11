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
import java.util.Map;
import java.util.Set;
import org.brutusin.json.ParseException;
import org.brutusin.json.annotations.IndexableProperty;
import org.brutusin.json.spi.JsonNode;
import org.brutusin.json.spi.JsonSchema;
import org.brutusin.json.util.JsonNodeVisitor;
import org.brutusin.json.util.JsonSchemaUtils;
import org.brutusin.fleadb.Schema;

/**
 * Schema implementation.
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public final class SchemaImpl implements Schema {

    private final JsonSchema jsonSchema;
    private Map<String, JsonNode.Type> indexFields;
    private Map<String, Boolean> facetFields;

    public SchemaImpl(JsonSchema jsonSchema) throws ParseException {
        this.jsonSchema = jsonSchema;
        initFields();
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

    @Override
    public JsonSchema getJSONSChema() {
        return this.jsonSchema;
    }

    @Override
    public Map<String, JsonNode.Type> getIndexFields() {
        return indexFields;
    }

    @Override
    public Map<String, Boolean> getFacetFields() {
       return facetFields;
    }
    
    public void setFacetFields(Map<String, Boolean> facetFields) {
        this.facetFields = facetFields;
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
}
