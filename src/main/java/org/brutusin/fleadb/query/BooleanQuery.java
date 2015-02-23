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

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.lucene.search.BooleanClause;
import org.brutusin.fleadb.Schema;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class BooleanQuery extends Query {

    private final LinkedHashMap<Query, BooleanClause.Occur> clauses = new LinkedHashMap();

    public void add(Query query, BooleanClause.Occur occur) {
        this.clauses.put(query, occur);
    }

    @Override
    public org.apache.lucene.search.Query getLuceneQuery(Schema schema) {
        org.apache.lucene.search.BooleanQuery q = new org.apache.lucene.search.BooleanQuery();
        for (Map.Entry<Query, BooleanClause.Occur> entry : clauses.entrySet()) {
            Query query = entry.getKey();
            BooleanClause.Occur occur = entry.getValue();
            q.add(q, occur);
        }
        return q;
    }
}
