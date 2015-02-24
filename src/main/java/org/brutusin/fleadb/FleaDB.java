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
package org.brutusin.fleadb;

import java.util.List;
import org.brutusin.fleadb.facet.FacetMultiplicities;
import org.brutusin.fleadb.facet.FacetResponse;
import org.brutusin.fleadb.pagination.Paginator;
import org.brutusin.fleadb.query.Query;
import org.brutusin.fleadb.sort.Sort;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public interface FleaDB<E> {

    public E getSingleResult(final Query q);

    public Paginator<E> query(final Query q);
    
    public Paginator<E> query(final Query q, final Sort sort);

    public void store(E entity);

    public void delete(Query q);

    public List<FacetResponse> getFacetValues(final Query q, FacetMultiplicities activeFacets);

    public List<FacetResponse> getFacetValues(final Query q, int maxFacetValues);

    public List<FacetResponse> getFacetValuesStartingWith(String facetName, String prefix, Query q, int max);

    public int getNumFacetValues(Query q, String facetName);

    public double getFacetValueMultiplicity(String facetName, String facetValue, Query q);

    public Schema getSchema();

    public void commit();

    public void close();

}
