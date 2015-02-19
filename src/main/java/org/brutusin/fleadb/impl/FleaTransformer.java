package org.brutusin.fleadb.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.index.IndexableField;
import org.brutusin.commons.Pair;
import org.brutusin.commons.json.spi.JsonCodec;
import org.brutusin.commons.json.spi.JsonNode;
import org.brutusin.commons.json.spi.JsonSchema;
import org.brutusin.fleadb.DocTransformer;
import org.brutusin.fleadb.Schema;
import org.brutusin.fleadb.utils.Expression;

public class FleaTransformer implements DocTransformer<String> {

    private static final FieldType NON_INDEXED_TYPE = new FieldType();
    private static final String OBJECT_FIELD_NAME = "$json";

    static {
        NON_INDEXED_TYPE.setIndexed(false);
        NON_INDEXED_TYPE.setStored(true);
    }

    private final Schema schema;
    private final JsonSchema jsonSchema;

    public FleaTransformer(Schema schema) {
        this.schema = schema;
        try {
            if (schema != null) {
                this.jsonSchema = JsonCodec.getInstance().parseSchema(schema.getJSONSChema());
            } else {
                this.jsonSchema = null;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public Pair<Document, List<FacetField>> entityToDocument(String entity) {
        if (entity == null) {
            return null;
        }
        JsonNode jsonNode;
        try {
            jsonNode = JsonCodec.getInstance().parse(entity);
            jsonSchema.validate(jsonNode);
        } catch (Exception e) {
            throw new RuntimeException("Error transforming entity: " + entity, e);
        }
        Document doc = new Document();
        doc.add(new Field(OBJECT_FIELD_NAME, jsonNode.toString(), NON_INDEXED_TYPE));
        Pair<Document, List<FacetField>> ret = new Pair<Document, List<FacetField>>();
        ret.setElement1(doc);
        Pair<List<IndexableField>, List<FacetField>> indexTerms = getIndexTerms(jsonNode);
        ret.setElement2(indexTerms.getElement2());

        List<IndexableField> indexFields = indexTerms.getElement1();
        for (int i = 0; i < indexFields.size(); i++) {
            IndexableField field = indexFields.get(i);
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
            Expression exp = Expression.compile(indexField);
            JsonNode projectedNode = exp.projectNode(jsonNode);
            addLuceneValues(indexField, indexFields, projectedNode);
        }
        
        Set<String> facetFields = this.schema.getFacetFields();
        for (String facetField : facetFields) {
            Expression exp = Expression.compile(facetField);
            JsonNode projectedNode = exp.projectNode(jsonNode);
            addLuceneFacets(facetField, facets, projectedNode);
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

    private void addLuceneValues(String indexField, List<IndexableField> list, JsonNode node) {
        JsonNode.Type type = node.getNodeType();
        if (type == JsonNode.Type.ARRAY) {
            for (int i = 0; i < node.getSize(); i++) {
                addLuceneValues(indexField, list, node.get(i));
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

    public String documentToEntity(Document doc) {
        return doc.get(OBJECT_FIELD_NAME);
    }
}
