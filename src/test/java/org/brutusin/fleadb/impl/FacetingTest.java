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
import org.brutusin.fleadb.facet.FacetMultiplicities;
import org.brutusin.fleadb.facet.FacetResponse;
import org.brutusin.fleadb.facet.FacetValueResponse;
import org.brutusin.fleadb.query.Query;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class FacetingTest extends InMemoryFleaDBTest {

    private final int MAX_FACET_VALUES = 5;

    @Test
    public void testAllFacets() {
        Query q = Query.MATCH_ALL_DOCS_QUERY;
        List<FacetResponse> frs = db.getFacetValues(q, MAX_FACET_VALUES);
        assertTrue(frs.size() == db.getSchema().getFacetFields().size());
        for (int i = 0; i < frs.size(); i++) {
            FacetResponse fr = frs.get(i);
            List<FacetValueResponse> fvs = fr.getFacetValues();
            assertTrue(MAX_FACET_VALUES >= fvs.size());
            System.out.println(fr.getFacetName() + "\t" + fr.getNumFacetValues());
            for (int j = 0; j < fvs.size(); j++) {
                FacetValueResponse fv = fvs.get(j);
                System.out.println("\t" + fv.getValue() + "\t" + fv.getMultiplicity());
                double queriedMultiplicity = db.getFacetValueMultiplicity(fr.getFacetName(), fv.getValue(), q);
                assertEquals(queriedMultiplicity, fv.getMultiplicity(), 0.1);
            }
        }
    }

    @Test
    public void testSelectedFacets() {
        Query q = Query.MATCH_ALL_DOCS_QUERY;
        List<FacetResponse> frs = db.getFacetValues(q, FacetMultiplicities.set("$.categories[#]", 2).and("$.components[*].name", 2));
        assertTrue(frs.size() == 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFacet() {
        Query q = Query.MATCH_ALL_DOCS_QUERY;
        db.getFacetValues(q, FacetMultiplicities.set("aaa", MAX_FACET_VALUES));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRepeatedFacet() {
        Query q = Query.MATCH_ALL_DOCS_QUERY;
        db.getFacetValues(q, FacetMultiplicities.set("$.categories[#]", 2).and("$.categories[#]", MAX_FACET_VALUES));
    }
}
