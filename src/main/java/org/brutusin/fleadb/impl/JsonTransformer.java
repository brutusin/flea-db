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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.index.IndexableField;
import org.brutusin.commons.Pair;
import org.brutusin.json.ValidationException;
import org.brutusin.json.util.LazyJsonNode;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.json.spi.JsonNode;
import org.brutusin.json.spi.JsonSchema;
import org.brutusin.fleadb.DocTransformer;
import org.brutusin.fleadb.Schema;
import org.brutusin.json.spi.Expression;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class JsonTransformer implements DocTransformer<JsonNode> {

    private static final FieldType NON_INDEXED_TYPE = new FieldType();
    private static final String OBJECT_FIELD_NAME = "$json";

    static {
        NON_INDEXED_TYPE.setIndexed(false);
        NON_INDEXED_TYPE.setStored(true);
    }

    private final Schema schema;
    private final JsonSchema jsonSchema;

    public JsonTransformer(Schema schema) {
        this.schema = schema;
        try {
            if (schema != null) {
                this.jsonSchema = schema.getJSONSChema();
            } else {
                this.jsonSchema = null;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public Pair<Document, List<FacetField>> entityToDocument(JsonNode jsonNode) {
        if (jsonNode == null) {
            return null;
        }
        try {
            jsonSchema.validate(jsonNode);
        } catch (ValidationException e) {
            throw new RuntimeException("Error transforming entity: " + jsonNode, e);
        }
        Document doc = new Document();
        doc.add(new Field(OBJECT_FIELD_NAME, jsonNode.toString(), NON_INDEXED_TYPE));
        Pair<Document, List<FacetField>> ret = new Pair<Document, List<FacetField>>();
        ret.setElement1(doc);
        Pair<List<IndexableField>, List<FacetField>> indexTerms = getIndexTerms(jsonNode);
        ret.setElement2(indexTerms.getElement2());

        List<IndexableField> indexFields = indexTerms.getElement1();
        for (IndexableField field : indexFields) {
            doc.add(field);
        }
        return ret;
    }

    private Pair<List<IndexableField>, List<FacetField>> getIndexTerms(JsonNode jsonNode) {
        Pair<List<IndexableField>, List<FacetField>> ret = new Pair<List<IndexableField>, List<FacetField>>();
        final List<IndexableField> indexFields = new ArrayList<IndexableField>();
        ret.setElement1(indexFields);
        final List<FacetField> facets = new ArrayList<FacetField>();
        ret.setElement2(facets);

        Map<String, JsonNode.Type> fields = this.schema.getIndexFields();

        for (Map.Entry<String, JsonNode.Type> entry : fields.entrySet()) {
            String indexField = entry.getKey();
            Expression exp = JsonCodec.getInstance().compile(indexField);
            JsonNode projectedNode = exp.projectNode(jsonNode);
            if (projectedNode != null) {
                addLuceneIndexFields(indexField, indexFields, projectedNode, exp.projectSchema(jsonSchema));
            }
        }

        Map<String, Boolean> facetFields = this.schema.getFacetFields();
        for (String facetField : facetFields.keySet()) {
            Expression exp = JsonCodec.getInstance().compile(facetField);
            JsonNode projectedNode = exp.projectNode(jsonNode);
            if (projectedNode != null) {
                addLuceneFacets(facetField, facets, projectedNode);
            }
        }
        return ret;
    }

    private void addLuceneFacets(String facetField, List<FacetField> list, JsonNode node) {
        JsonNode.Type type = node.getNodeType();
        if (type == JsonNode.Type.ARRAY) {
            for (int i = 0; i < node.getSize(); i++) {
                addLuceneFacets(facetField, list, node.get(i));
            }
        } else if (type == JsonNode.Type.OBJECT) {
            Iterator<String> properties = node.getProperties();
            while (properties.hasNext()) {
                String propName = properties.next();
                list.add(new FacetField(facetField, propName));
            }
        } else if (type == JsonNode.Type.STRING) {
            list.add(new FacetField(facetField, node.asString()));
        } else if (type == JsonNode.Type.BOOLEAN) {
            list.add(new FacetField(facetField, node.asString()));
        } else {
            throw new UnsupportedOperationException("Node type " + type + " not supported for facet field " + facetField);
        }
    }

    private void addLuceneIndexFields(String indexField, List<IndexableField> list, JsonNode node, JsonSchema nodeSchema) {
        JsonNode.Type type = nodeSchema.getSchemaType();
        if (type == JsonNode.Type.ARRAY) {
            for (int i = 0; i < node.getSize(); i++) {
                addLuceneIndexFields(indexField, list, node.get(i), nodeSchema.getItemSchema());
            }
        } else if (type == JsonNode.Type.OBJECT) {
            Iterator<String> properties = node.getProperties();
            while (properties.hasNext()) {
                String propName = properties.next();
                // Index property key for object nodes
                list.add(new StringField(indexField, propName, Field.Store.NO));
            }
        } else if (type == JsonNode.Type.STRING) {
            list.add(new StringField(indexField, node.asString(), Field.Store.NO));
        } else if (type == JsonNode.Type.BOOLEAN) {
            list.add(new StringField(indexField, node.asString(), Field.Store.NO));
        } else if (type == JsonNode.Type.INTEGER) {
            list.add(new LongField(indexField, node.asLong(), Field.Store.NO));
        } else if (type == JsonNode.Type.NUMBER) {
            list.add(new DoubleField(indexField, node.asDouble(), Field.Store.NO));
        } else {
            throw new UnsupportedOperationException("Node type " + type + " not supported for index field " + indexField);
        }
    }

    public JsonNode documentToEntity(Document doc) {
        return new LazyJsonNode(doc.get(OBJECT_FIELD_NAME));
    }
}
