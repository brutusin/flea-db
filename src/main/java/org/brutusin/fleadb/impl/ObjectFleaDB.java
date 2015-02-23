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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.brutusin.commons.json.ParseException;
import org.brutusin.commons.json.spi.JsonCodec;
import org.brutusin.commons.json.spi.JsonNode;
import org.brutusin.commons.search.ActiveFacetMap;
import org.brutusin.commons.search.FacetResponse;
import org.brutusin.fleadb.FleaDB;
import org.brutusin.fleadb.FleaDBInfo;
import org.brutusin.fleadb.Schema;
import org.brutusin.fleadb.pagination.Paginator;
import org.brutusin.fleadb.query.Query;
import org.brutusin.fleadb.sort.Sort;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public final class ObjectFleaDB<E> implements FleaDB<E> {

    private final GenericFleaDB genericFleaDB;
    private final Class<E> clazz;

    public ObjectFleaDB(Class<E> clazz) throws IOException {
        this(null, clazz);
    }

    public ObjectFleaDB(File indexFolder, Class<E> clazz) throws IOException {
        this.genericFleaDB = new GenericFleaDB(indexFolder, JsonCodec.getInstance().getSchema(clazz));
        this.clazz = clazz;
    }

    public File getIndexFolder() {
        return genericFleaDB.getIndexFolder();
    }

    protected final FleaDBInfo getFleaDBInfo() {
        return genericFleaDB.getFleaDBInfo();
    }

    @Override
    public synchronized void close() {
        genericFleaDB.close();
    }

    @Override
    public E getSingleResult(Query q) {
        return JsonCodec.getInstance().load(genericFleaDB.getSingleResult(q), clazz);
    }

    @Override
    public final Paginator<E> query(final Query q) {
        return query(q, null);
    }

    @Override
    public final Paginator<E> query(Query q, Sort sort) {
        final Paginator<JsonNode> paginator = genericFleaDB.query(q, sort);
        return new Paginator<E>() {
            public int getTotalHits() {
                return paginator.getTotalHits();
            }

            public int getTotalPages(int pageSize) {
                return paginator.getTotalPages(pageSize);
            }

            public List<E> getPage(int pageNum, int pageSize) {
                try {
                    List<JsonNode> page = paginator.getPage(pageNum, pageSize);
                    List<E> ret = new ArrayList(page.size());
                    for (JsonNode jsonNode : page) {
                        ret.add(JsonCodec.getInstance().parse(jsonNode.toString(), clazz));
                    }
                    return ret;
                } catch (ParseException ex) {
                    throw new RuntimeException();
                }
            }

            public E getFirstElement() {
                JsonNode firstElement = paginator.getFirstElement();
                try {
                    return JsonCodec.getInstance().parse(firstElement.toString(), clazz);
                } catch (ParseException ex) {
                    throw new RuntimeException();
                }
            }
        };
    }

    @Override
    public final List<FacetResponse> getFacetValues(Query q, int maxFacetValues) {
        return genericFleaDB.getFacetValues(q, maxFacetValues);
    }

    @Override
    public final List<FacetResponse> getFacetValues(Query q, ActiveFacetMap facets) {
        return genericFleaDB.getFacetValues(q, facets);
    }

    @Override
    public int getNumFacetValues(Query q, String facetName) {
        return genericFleaDB.getNumFacetValues(q, facetName);
    }

    @Override
    public final List<FacetResponse> getFacetValuesStartingWith(String facetName, String prefix, Query q, int max) {
        return genericFleaDB.getFacetValuesStartingWith(facetName, prefix, q, max);
    }

    @Override
    public final double getFacetValueMultiplicity(String facetName, String facetValue, Query q) {
        return genericFleaDB.getFacetValueMultiplicity(facetName, facetValue, q);
    }

    @Override
    public final Schema getSchema() {
        return genericFleaDB.getSchema();
    }

    @Override
    public final void delete(Query q) {
        genericFleaDB.delete(q);
    }

    @Override
    public final void commit() {
        genericFleaDB.commit();
    }

    @Override
    public final void store(E entity) {
        try {
            genericFleaDB.store(JsonCodec.getInstance().parse(JsonCodec.getInstance().transform(entity)));
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void optimize() throws IOException {
        genericFleaDB.optimize();
    }
}
