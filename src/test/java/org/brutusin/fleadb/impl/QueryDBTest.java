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
import org.brutusin.fleadb.impl.record.Record;
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

//    public static void main(String[] args) throws Exception {
//        JsonSchema jsonSchema = JsonCodec.getInstance().parseSchema(JsonCodec.getInstance().getSchemaString(Record.class));
//        GenericFleaDB db = new GenericFleaDB(new File("C:/temp/flea"), jsonSchema);
//        System.out.println(db.getFleaDBInfo().getSchema().getFacetFields());
//        Record r = new Record();
//        r.setId("jairo");
//        r.setCategory(new String[]{"man", "supergay"});
//        r.setAge(31);
//        r.setDog(new Dog("perrin"));
//        HashMap<String, Stuff> map = new HashMap<String, Stuff>();
//        map.put("bici", new Stuff("orbea", 1));
//        map.put("coche", new Stuff("fiat", 1));
//        r.setStuff(map);
//        db.store(JsonCodec.getInstance().parse(JsonCodec.getInstance().transform(r)));
//
//        r = new Record();
//        r.setId("nacho");
//        r.setCategory(new String[]{"man"});
//        r.setAge(35);
//        map = new HashMap<String, Stuff>();
//        map.put("lancha", new Stuff("narval", 1));
//        map.put("bici", new Stuff("scott", 1));
//        r.setStuff(map);
//        db.store(JsonCodec.getInstance().parse(JsonCodec.getInstance().transform(r)));
//
//        r = new Record();
//        r.setId("bego");
//        r.setCategory(new String[]{"woman"});
//        r.setAge(35);
//        map = new HashMap<String, Stuff>();
//        map.put("maquina", new Stuff("singer", 1));
//        map.put("bici", new Stuff("bh", 1));
//        r.setStuff(map);
//        db.store(JsonCodec.getInstance().parse(JsonCodec.getInstance().transform(r)));
//
//        db.commit();
//        Query q = new MatchAllDocsQuery();
//        //        Query q = new TermQuery(new Term("$.category[#]", "guay"));
//        //Query q = NumericRangeQuery.newLongRange("$.age", 0l, 33l, true, true);
//
//        Sort sort = new Sort(new SortField("$.dog.name", SortField.Type.STRING, true));
//        Paginator<JsonNode> pag = db.query(q, sort);
//
//        System.out.println(db.getFleaDBInfo());
//        System.out.println(db.getFleaDBInfo().getSchema().getIndexFields());
//        System.out.println(db.getFleaDBInfo().getSchema().getFacetFields());
//        int pageSize = 1;
//        int totalPages = pag.getTotalPages(pageSize);
//        long prev = System.currentTimeMillis();
//        for (int i = 1; i <= totalPages; i++) {
//            List<JsonNode> page = pag.getPage(i, pageSize);
//            System.out.println("**** Page " + i);
//            for (JsonNode rec : page) {
//                System.out.println(rec);
//            }
//            long current = System.currentTimeMillis();
//            System.out.println(current - prev + " ms");
//            prev = current;
//        }
//        long start = System.currentTimeMillis();
//        List<FacetResponse> facetValues = db.getFacetValues(q, 10);
//        System.out.println("Facet time: " + (System.currentTimeMillis() - start));
//        for (int i = 0; i < facetValues.size(); i++) {
//            FacetResponse fr = facetValues.get(i);
//            List<FacetValueResponse> facetValues1 = fr.getFacetValues();
//            System.out.println(fr.getFacetName() + "\t" + fr.getNumFacetValues());
//            for (int j = 0; j < facetValues1.size(); j++) {
//                FacetValueResponse fvr = facetValues1.get(j);
//                System.out.println("\t" + fvr.getValue() + "\t" + fvr.getMultiplicity());
//            }
//        }
//        db.close();
//    }
}
