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
package org.brutusin.fleadb.query;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.brutusin.commons.json.spi.JsonNode;
import org.brutusin.fleadb.Schema;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public abstract class Query {

    public static final Query MATCH_ALL_DOCS_QUERY = new Query() {
        private final org.apache.lucene.search.Query luceneQuery = new MatchAllDocsQuery();

        @Override
        public org.apache.lucene.search.Query getLuceneQuery(Schema schema) {
            return luceneQuery;
        }
    };

    public static Query createNumericRangeQuery(final String field, final double min, final double max, final boolean minInclusive, final boolean maxInclusive) {
        return new Query() {
            @Override
            public org.apache.lucene.search.Query getLuceneQuery(Schema schema) {
                validateType(field, schema, JsonNode.Type.NUMBER);
                return NumericRangeQuery.newDoubleRange(field, min, max, minInclusive, maxInclusive);
            }
        };
    }

    public static Query createIntegerRangeQuery(final String field, final long min, final long max, final boolean minInclusive, final boolean maxInclusive) {
        return new Query() {
            @Override
            public org.apache.lucene.search.Query getLuceneQuery(Schema schema) {
                validateType(field, schema, JsonNode.Type.INTEGER);
                return NumericRangeQuery.newLongRange(field, min, max, minInclusive, maxInclusive);
            }
        };
    }

    public static Query createTermQuery(final String field, final String value) {
        return new Query() {
            @Override
            public org.apache.lucene.search.Query getLuceneQuery(Schema schema) {
                validateType(field, schema, JsonNode.Type.STRING);
                return new TermQuery(new Term(field, value));
            }
        };
    }

    public abstract org.apache.lucene.search.Query getLuceneQuery(Schema schema);

    private static void validateType(String field, Schema schema, JsonNode.Type allowedType) {
        JsonNode.Type type = schema.getIndexFields().get(field);
        if (type == null) {
            throw new IllegalArgumentException("Unknown query field '" + field + "' found. Supported field are: " + schema.getIndexFields().keySet());
        }
        if (type != allowedType) {
            throw new IllegalArgumentException("Field " + field + " type error. Expected: " + allowedType + " , found: " + type);
        }
    }
}
