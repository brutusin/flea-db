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
import java.util.Map;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetField;
import org.brutusin.commons.Pair;
import org.brutusin.commons.json.ParseException;
import org.brutusin.commons.json.annotations.IndexableProperty;
import org.brutusin.commons.json.spi.JsonCodec;
import org.brutusin.commons.json.spi.JsonNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class SchemaImplTest {

    public SchemaImplTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getJSONSChema method, of class SchemaImpl.
     */
    @Test
    public void test() throws ParseException {
        SchemaImpl s = new SchemaImpl(JsonCodec.getInstance().parseSchema(JsonCodec.getInstance().getSchemaString(TestClass.class)));
        System.out.println(s.getJSONSChema());
        System.out.println("Index:");
        Map<String, JsonNode.Type> indexFields = s.getIndexFields();
        for (Map.Entry<String, JsonNode.Type> entry : indexFields.entrySet()) {
            String name = entry.getKey();
            JsonNode.Type type = entry.getValue();
            System.out.println(name + ":" + type);
        }
        assertEquals(5, indexFields.size());
        System.out.println("Facet:");
        Map<String, Boolean> facetFields = s.getFacetFields();
        for (Map.Entry<String, Boolean> entry : facetFields.entrySet()) {
            String name = entry.getKey();
            Boolean mutival = entry.getValue();
            System.out.println(name + ":" + mutival);
        }
        assertEquals(2, facetFields.size());
        JsonNode node = JsonCodec.getInstance().parse("{\"map\":{\"aa\":{\"int1\":3,\"booleanMap\":{\"key1\":true},\"s1\":\"s11Value\",\"s2\":[\"s2Value11\",\"s2Value21\"]},\"bb\":{\"booleanMap\":{\"key2\":false},\"s1\":\"s12Value\",\"s2\":[\"s2Value12\",\"s2Value22\"]}}}{\"map\":{\"aa\":{\"booleanMap\":{\"key1\":true},\"s1\":\"s11Value\",\"s2\":[\"s2Value11\",\"s2Value21\"]},\"bb\":{\"booleanMap\":{\"key2\":false},\"s1\":\"s12Value\",\"s2\":[\"s2Value12\",\"s2Value22\"]}}}");
        JsonTransformer transformer = new JsonTransformer(s);
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
