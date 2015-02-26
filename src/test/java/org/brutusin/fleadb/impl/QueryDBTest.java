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

import java.util.List;
import org.brutusin.fleadb.record.Record;
import org.brutusin.fleadb.pagination.Paginator;
import org.brutusin.fleadb.query.Query;
import org.brutusin.fleadb.sort.Sort;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class QueryDBTest extends InMemoryFleaDBTest {

    @Test
    public void testStoreAndRetrieve() {
        Paginator<Record> paginator = db.query(Query.MATCH_ALL_DOCS_QUERY);
        assertEquals(getMaxRecords(), paginator.getTotalHits());
    }

    @Test
    public void testQueryInteger() {
        Query q = Query.createIntegerRangeQuery("$.age", 1l, (long) getMaxRecords() - 1, true, true);
        Paginator<Record> paginator = db.query(q);
        assertEquals(getMaxRecords() - 1, paginator.getTotalHits());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnkownFieldQuery() {
        Query q = Query.createIntegerRangeQuery("$.age3", 0, 0, true, true);
        q.getLuceneQuery(db.getSchema());
    }

    @Test
    public void testQueryString() {
        Query q = Query.createTermQuery("$.id", "0");
        Paginator<Record> paginator = db.query(q);
        assertEquals(1, paginator.getTotalHits());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidTypeString() {
        Query q = Query.createTermQuery("$.age", "0");
        q.getLuceneQuery(db.getSchema());
    }

    @Test
    public void testQueryArray() {
        Query q = Query.createTermQuery("$.categories[#]", "mod2:0");
        Paginator<Record> paginator = db.query(q);
        int pageSize = getMaxRecords();
        List<Record> page = paginator.getPage(1, pageSize);
        for (int i = 0; i < page.size(); i++) {
            Record record = page.get(i);
            assertTrue(record.getAge() % 2 == 0);
        }
    }

    @Test
    public void testSort() {
        Query q = Query.MATCH_ALL_DOCS_QUERY;
        Paginator<Record> paginator = db.query(q, Sort.by("$.age", true));
        int pageSize = getMaxRecords();
        List<Record> page = paginator.getPage(1, pageSize);
        int prevAge = Integer.MAX_VALUE;
        for (int i = 0; i < page.size(); i++) {
            Record record = page.get(i);
            assertTrue(record.getAge() <= prevAge);
            prevAge = record.getAge();
        }
    }

    @Test
    public void testSortWithNullsAndPagination() {
        Query q = Query.MATCH_ALL_DOCS_QUERY;
        Paginator<Record> paginator = db.query(q, Sort.by("$.components").thenBy("$.id", true));
        int pageSize = 1;
        int totalPages = paginator.getTotalPages(pageSize);
        int counter = 0;
        for (int i = 1; i <= totalPages; i++) {
            assertTrue(counter <= getMaxRecords());
            List<Record> page = paginator.getPage(i, pageSize);
            for (int j = 0; j < page.size(); j++) {
                Record record = page.get(j);
                System.out.println(record.getId() + "\t" + record.getComponents());
                counter++;
            }
        }
        assertTrue(counter == getMaxRecords());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnkownFieldSort() {
        Query q = Query.createIntegerRangeQuery("$.age3", 0, 0, true, true);
        q.getLuceneQuery(db.getSchema());
    }
}
