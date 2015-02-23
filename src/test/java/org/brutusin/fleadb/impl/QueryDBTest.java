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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.brutusin.fleadb.impl.record.Component;
import org.brutusin.fleadb.impl.record.Record;
import org.brutusin.fleadb.pagination.Paginator;
import org.brutusin.fleadb.query.Query;
import org.brutusin.fleadb.sort.Sort;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class QueryDBTest {

    private final int REC_NO = 20;
    private ObjectFleaDB<Record> db;

    @Before
    public void setUp() {
        try {
            db = new ObjectFleaDB(Record.class);
            for (int i = 0; i < REC_NO; i++) {
                Record r = new Record();
                r.setId(String.valueOf(i));
                r.setAge(i);
                String[] categories = new String[]{"mod2:" + i % 2, "mod3:" + i % 3};
                r.setCategories(categories);
                if (i > 5) {
                    Map<String, Component> components = new HashMap();
                    components.put("component-" + (i < 10), new Component("item " + i, i));
                    r.setComponents(components);
                }
                db.store(r);
            }
            db.commit();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void testStoreAndRetrieve() {
        Paginator<Record> paginator = db.query(Query.MATCH_ALL_DOCS_QUERY);
        assertEquals(REC_NO, paginator.getTotalHits());
    }

    @Test
    public void testQueryInteger() {
        Query q = Query.createIntegerRangeQuery("$.age", 1l, (long) REC_NO - 1, true, true);
        Paginator<Record> paginator = db.query(q);
        assertEquals(REC_NO - 1, paginator.getTotalHits());
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

    @Test
    public void testQueryArray() {
        Query q = Query.createTermQuery("$.categories[#]", "mod2:0");
        Paginator<Record> paginator = db.query(q);
        int pageSize = REC_NO;
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
        int pageSize = REC_NO;
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
            assertTrue(counter <= REC_NO);
            List<Record> page = paginator.getPage(i, pageSize);
            for (int j = 0; j < page.size(); j++) {
                Record record = page.get(j);
                System.out.println(record.getId() + "\t" + record.getComponents());
                counter++;
            }
        }
        assertTrue(counter == REC_NO);
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
