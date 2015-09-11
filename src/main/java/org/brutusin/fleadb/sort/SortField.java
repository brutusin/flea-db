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
package org.brutusin.fleadb.sort;

import org.brutusin.json.spi.JsonNode;
import org.brutusin.fleadb.Schema;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class SortField {

    private final String field;
    private final boolean reverse;

    public SortField(String field, boolean reverse) {
        this.field = field;
        this.reverse = reverse;
    }

    public org.apache.lucene.search.SortField getLuceneSortField(Schema schema) {
        JsonNode.Type jsonType = schema.getIndexFields().get(field);
        if (jsonType == null) {
            throw new IllegalArgumentException("Unknown sort field '" + field + "' found. Supported field are: " + schema.getIndexFields().keySet());
        }
        org.apache.lucene.search.SortField.Type type;
        switch (jsonType) {
            case BOOLEAN:
            case STRING:
                type = org.apache.lucene.search.SortField.Type.STRING;
                break;
            case NUMBER:
                type = org.apache.lucene.search.SortField.Type.DOUBLE;
                break;
            case INTEGER:
                type = org.apache.lucene.search.SortField.Type.LONG;
                break;
            default:
                throw new AssertionError();
        }
        return new org.apache.lucene.search.SortField(field, type, reverse);
    }
}
