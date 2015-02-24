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
package org.brutusin.fleadb.facet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.brutusin.fleadb.Schema;

/**
 * Instances of this class are not thread safe.
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public final class FacetMultiplicities {

    private final Map<String, Integer> facetMap = new HashMap();
    private Map<String, Integer> unmodifableMap;

    private FacetMultiplicities(String facetName, int multiplicity) {
        and(facetName, multiplicity);
    }

    public static FacetMultiplicities set(String facetName, int multiplicity) {
        FacetMultiplicities ret = new FacetMultiplicities(facetName, multiplicity);
        return ret;
    }

    public FacetMultiplicities and(String facetName, int multiplicity) {
        validate(facetName, multiplicity);
        facetMap.put(facetName, multiplicity);
        this.unmodifableMap = null;
        return this;
    }

    private void validate(String facetName, int multiplicity) {
        if (this.facetMap.containsKey(facetName)) {
            throw new IllegalArgumentException("Facet name already registered: " + facetName);
        }
        if (multiplicity < 1) {
            throw new IllegalArgumentException("Multiplicity must be greater than 0");
        }
    }

    public Map<String, Integer> getFacetMap(Schema schema) {
        if (unmodifableMap == null) {
            this.unmodifableMap = Collections.unmodifiableMap(facetMap);
        }
        for (Map.Entry<String, Integer> entry : facetMap.entrySet()) {
            String facetName = entry.getKey();
            if (!schema.getFacetFields().containsKey(facetName)) {
                throw new IllegalArgumentException("Invalid facet name: " + facetName + ". Supported values are: " + schema.getFacetFields().keySet());
            }
        }
        return this.unmodifableMap;
    }
}
