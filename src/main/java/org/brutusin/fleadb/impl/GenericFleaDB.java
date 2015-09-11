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
import java.util.Map;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.brutusin.commons.Pair;
import org.brutusin.json.ParseException;
import org.brutusin.json.spi.JsonNode;
import org.brutusin.json.spi.JsonSchema;
import org.brutusin.fleadb.facet.FacetMultiplicities;
import org.brutusin.fleadb.facet.FacetResponse;
import org.brutusin.fleadb.facet.FacetValueResponse;
import org.brutusin.fleadb.FleaDB;
import org.brutusin.fleadb.FleaDBInfo;
import org.brutusin.fleadb.Schema;
import org.brutusin.fleadb.pagination.Paginator;
import org.brutusin.fleadb.pagination.PaginatorImpl;
import org.brutusin.fleadb.query.BooleanQuery;
import org.brutusin.fleadb.query.Query;
import org.brutusin.fleadb.sort.Sort;

/**
 * A generic FleaDB, that allows dynamic interaction with databases, using JSON
 * syntax.
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public final class GenericFleaDB implements FleaDB<JsonNode> {

    public static final Version LUCENE_VERSION = Version.LUCENE_4_10_3;
    private static final String DESCRIPTOR_FILE_NAME = "flea.json";
    private static final String RECORD_INDEX_SUBFOLDER_NAME = "record-index";
    private static final String TAXONOMY_INDEX_SUBFOLDER_NAME = "taxonomy-index";

    private final FleaDBInfo dsInfo;
    private final JsonTransformer transformer;
    private final Directory indexDir;
    private final Directory facetDir;
    private final File indexFolder;

    private FacetsConfig facetsConfig;

    private boolean closed = false;
    private File infoFile;

    // Double-Checked Locking instances
    private volatile IndexSearcher indexSearcher;
    private volatile IndexWriter indexWriter;
    private volatile TaxonomyReader taxonomyReader;
    private volatile TaxonomyWriter taxonomyWriter;

    /**
     * Creates an in-memory instance with a Schema determined by the specified
     * JsonSchema.
     *
     * @param jsonSchema
     * @throws IOException
     */
    public GenericFleaDB(JsonSchema jsonSchema) throws IOException {
        this(null, jsonSchema);
    }

    /**
     * Opens an existing database.
     *
     * @param indexFolder
     * @throws IOException
     */
    public GenericFleaDB(File indexFolder) throws IOException {
        this(indexFolder, null);
    }

    /**
     * Creates a persistent database with a Schema determined by the specified
     * JsonSchema.
     *
     * @param indexFolder
     * @param jsonSchema
     * @throws IOException
     */
    public GenericFleaDB(File indexFolder, JsonSchema jsonSchema) throws IOException {
        try {
            this.indexFolder = indexFolder;
            Schema schema;
            if (jsonSchema == null) {
                schema = null;
            } else {
                schema = new SchemaImpl(jsonSchema);
            }
            // In memory datasource
            if (indexFolder == null) {
                if (schema == null) {
                    throw new IllegalArgumentException("In-memory datasources require an schema");
                }
                this.dsInfo = new FleaDBInfo();
                this.dsInfo.setSchema(schema);
                this.indexDir = new RAMDirectory();
                this.facetDir = new RAMDirectory();

                // Disk datasource
            } else {
                this.infoFile = new File(indexFolder, DESCRIPTOR_FILE_NAME);
                if (indexFolder.exists()) {
                    this.dsInfo = readFleaDBInfo();
                    if (this.dsInfo == null) {
                        throw new IllegalArgumentException("Unable to read schema from specified index folder '" + indexFolder + "'");
                    }
                    if (schema != null && !this.dsInfo.getSchema().getJSONSChema().equals(schema.getJSONSChema())) {
                        throw new IllegalArgumentException("Specified schema is incompatible with current datasource for index at '" + indexFolder + "'");
                    }
                } else {
                    if (schema == null) {
                        throw new IllegalArgumentException("Index folder does not exist '" + indexFolder.getAbsolutePath() + "'");
                    }
                    this.dsInfo = new FleaDBInfo();
                    this.dsInfo.setSchema(schema);
                    writeFleaDBInfo();
                }
                this.indexDir = FSDirectory.open(new File(indexFolder, RECORD_INDEX_SUBFOLDER_NAME));
                this.facetDir = FSDirectory.open(new File(indexFolder, TAXONOMY_INDEX_SUBFOLDER_NAME));
            }
            this.transformer = new JsonTransformer(this.dsInfo.getSchema());
            this.facetsConfig = new FacetsConfig();
            Map<String, Boolean> facets = getSchema().getFacetFields();
            for (Map.Entry<String, Boolean> entry : facets.entrySet()) {
                String facet = entry.getKey();
                Boolean multievaluated = entry.getValue();
                facetsConfig.setMultiValued(facet, multievaluated);
            }
        } catch (Throwable th) {
            close();
            if (th instanceof IOException) {
                throw (IOException) th;
            } else if (th instanceof RuntimeException) {
                throw (RuntimeException) th;
            } else {
                throw (Error) th;
            }
        }
    }

    public File getIndexFolder() {
        return indexFolder;
    }

    private IndexSearcher getIndexSearcher() throws IOException {
        if (indexSearcher == null) {
            synchronized (this) {
                if (indexSearcher == null) {
                    indexSearcher = new IndexSearcher(DirectoryReader.open(indexDir));
                }
            }
        }
        return indexSearcher;
    }

    private IndexWriter getIndexWriter() throws IOException {
        if (indexWriter == null) {
            synchronized (this) {
                if (indexWriter == null) {
                    IndexWriterConfig config = new IndexWriterConfig(LUCENE_VERSION, null);
                    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                    this.indexWriter = new IndexWriter(indexDir, config);
                }
            }
        }
        return indexWriter;
    }

    private TaxonomyReader getTaxonomyReader() throws IOException {
        if (taxonomyReader == null) {
            synchronized (this) {
                if (taxonomyReader == null) {
                    taxonomyReader = new DirectoryTaxonomyReader(facetDir);
                }
            }
        }
        return taxonomyReader;
    }

    private TaxonomyWriter getTaxonomyWriter() throws IOException {
        if (taxonomyWriter == null) {
            synchronized (this) {
                if (taxonomyWriter == null) {
                    taxonomyWriter = new DirectoryTaxonomyWriter(facetDir, IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                }
            }
        }
        return taxonomyWriter;
    }

    protected final FleaDBInfo getFleaDBInfo() {
        return dsInfo;
    }

    @Override
    public synchronized void close() {
        try {
            if (this.closed) {
                throw new IllegalStateException("Datasource has been closed already");
            }
            closed = true;
            if (this.taxonomyReader != null) {
                this.taxonomyReader.close();
            }
            if (this.taxonomyWriter != null) {
                this.taxonomyWriter.close();
            }
            if (this.indexSearcher != null) {
                this.indexSearcher.getIndexReader().close();
            }
            if (this.indexWriter != null) {
                this.indexWriter.close();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public JsonNode getSingleResult(Query q) {
        Paginator<JsonNode> paginator = query(q, null);
        if (paginator.getTotalHits() > 1) {
            throw new IllegalArgumentException("Query returned more than 1 results");
        }
        return paginator.getFirstElement();
    }

    @Override
    public final Paginator<JsonNode> query(final Query q) {
        return query(q, null);
    }

    @Override
    public final Paginator<JsonNode> query(final Query q, final Sort sort) {
        try {
            verifyNotClosed();
            return new PaginatorImpl<JsonNode>(getIndexSearcher(), this.transformer, q.getLuceneQuery(getSchema()), sort == null ? null : sort.getLuceneSort(getSchema()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public final List<FacetResponse> getFacetValues(final Query q, int maxFacetValues) {
        verifyNotClosed();
        if (getSchema().getFacetFields().isEmpty()) {
            return null;
        }
        FacetMultiplicities facetMultiplicities = null;
        for (Map.Entry<String, Boolean> entry : getSchema().getFacetFields().entrySet()) {
            String facetName = entry.getKey();
            if (facetMultiplicities == null) {
                facetMultiplicities = FacetMultiplicities.set(facetName, maxFacetValues);
            } else {
                facetMultiplicities.and(facetName, maxFacetValues);
            }
        }
        return getFacetValues(q, facetMultiplicities);
    }

    @Override
    public int getNumFacetValues(final Query q, final String facetName) {
        List<FacetResponse> frs = getFacetValues(q, FacetMultiplicities.set(facetName, 1));
        return frs.get(0).getNumFacetValues();
    }

    @Override
    public final List<FacetResponse> getFacetValues(final Query q, FacetMultiplicities facetMultiplicities) {
        verifyNotClosed();
        if (facetMultiplicities == null) {
            return null;
        }
        try {
            List<FacetResponse> ret = new ArrayList<FacetResponse>();
            FacetsCollector facetCollector = new FacetsCollector();
            getIndexSearcher().search(q.getLuceneQuery(getSchema()), facetCollector);
            FacetsConfig config = new FacetsConfig();
            FastTaxonomyFacetCounts facets = new FastTaxonomyFacetCounts(getTaxonomyReader(), config, facetCollector);

            Map<String, Integer> facetMap = facetMultiplicities.getFacetMap(getSchema());
            for (Map.Entry<String, Integer> entry : facetMap.entrySet()) {
                String facetName = entry.getKey();
                Integer multiplicity = entry.getValue();
                FacetResult res = facets.getTopChildren(multiplicity, facetName);
                if (res != null) {
                    FacetResponseImpl fr = new FacetResponseImpl(facetName);
                    fr.setNumFacetValues(res.childCount);
                    ret.add(fr);
                    LabelAndValue[] lvs = res.labelValues;
                    for (int j = 0; j < lvs.length; j++) {
                        LabelAndValue lv = lvs[j];
                        FacetValueResponseImpl fvresp = new FacetValueResponseImpl(lv.label, lv.value.doubleValue());
                        fr.getFacetValues().add(fvresp);
                    }
                } else {
                    FacetResponseImpl fr = new FacetResponseImpl(facetName);
                    fr.setNumFacetValues(0);
                    ret.add(fr);
                }
            }
            return ret;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public final List<FacetResponse> getFacetValuesStartingWith(String facetName, final String prefix, Query q, int max) {
        verifyNotClosed();
        BooleanQuery bq = new BooleanQuery();
        bq.add(q, BooleanClause.Occur.MUST);

        List<FacetResponse> startingResponse = getFacetValues(bq, FacetMultiplicities.set(facetName, max));
        if (prefix != null) {
            double exactMultiplicity = getFacetValueMultiplicity(facetName, prefix, q);
            if (exactMultiplicity > 0) {
                FacetValueResponseImpl exactMatch = new FacetValueResponseImpl(prefix, exactMultiplicity);
                startingResponse.get(0).getFacetValues().remove(exactMatch);
                startingResponse.get(0).getFacetValues().add(0, exactMatch);
            }
        }
        return startingResponse;
    }

    @Override
    public final double getFacetValueMultiplicity(String facetName, String facetValue, Query q) {
        verifyNotClosed();
        BooleanQuery bq = new BooleanQuery();
        bq.add(q, BooleanClause.Occur.MUST);
        bq.add(Query.createTermQuery(facetName, facetValue), BooleanClause.Occur.MUST);
        Paginator pag = query(bq, null);
        return pag.getTotalHits();
    }

    @Override
    public final Schema getSchema() {
        return this.dsInfo.getSchema();
    }

    @Override
    public final void delete(Query q) {
        try {
            verifyNotClosed();
            this.getIndexWriter().deleteDocuments(q.getLuceneQuery(getSchema()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public final void commit() {
        try {
            verifyNotClosed();
            this.getTaxonomyWriter().commit();
            this.getIndexWriter().commit();
            refresh();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public final void store(JsonNode entity) {
        try {
            verifyNotClosed();
            Pair<Document, List<FacetField>> pair = this.transformer.entityToDocument(entity);
            Document doc = pair.getElement1();
            List<FacetField> facetFields = pair.getElement2();
            if (facetFields != null) {
                for (int i = 0; i < facetFields.size(); i++) {
                    String facetName = facetFields.get(i).dim;
                    boolean multievaluated = facetName.contains("[*]") || facetName.contains("[#]");
                    if (multievaluated) {
                        facetsConfig.setMultiValued(facetName, multievaluated);
                    }
                    doc.add(facetFields.get(i));
                }
            }
            this.getIndexWriter().addDocument(this.facetsConfig.build(getTaxonomyWriter(), doc));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void verifyNotClosed() {
        if (this.closed) {
            throw new IllegalStateException("Datasource has been closed");
        }
    }

    public void optimize() throws IOException {
        verifyNotClosed();
        getIndexWriter().forceMergeDeletes();
        getIndexWriter().forceMerge(1);
        optimizeFacetIndex();
        commit();
    }

    private void writeFleaDBInfo() throws IOException {
        if (infoFile != null) { // In disk mode
            FleaDBInfo.writeToFile(dsInfo, infoFile);
        }
    }

    private FleaDBInfo readFleaDBInfo() throws IOException, ParseException {
        if (infoFile != null) { // In disk mode
            return FleaDBInfo.readFromFile(infoFile);
        }
        return null;
    }

    private synchronized void optimizeFacetIndex() throws IOException {
        /*
         Taxonomy writer merge policy is not clear:
         http://lucene.apache.org/core/4_10_2/facet/org/apache/lucene/facet/taxonomy/directory/DirectoryTaxonomyWriter.html#createIndexWriterConfig(org.apache.lucene.index.IndexWriterConfig.OpenMode)
         */

        //Next leads to errors:
//        if (taxonomyWriter != null) {
//            taxonomyWriter.close();
//            taxonomyWriter = null;
//        }
//        IndexWriterConfig config = new IndexWriterConfig(JsonDataSource.LUCENE_VERSION, null);
//        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
//        IndexWriter iw = new IndexWriter(facetDir, config);
//        try {
//            //iw.forceMergeDeletes();
//            iw.forceMerge(1);
//        } finally {
//            iw.close();
//        }
    }

    private synchronized void refresh() {
        this.indexSearcher = null;
        this.taxonomyReader = null;
    }

}
