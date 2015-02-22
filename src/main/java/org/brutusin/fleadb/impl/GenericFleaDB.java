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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
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
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.brutusin.commons.Pair;
import org.brutusin.commons.json.ParseException;
import org.brutusin.commons.json.annotations.IndexableProperty;
import org.brutusin.commons.json.spi.JsonCodec;
import org.brutusin.commons.json.spi.JsonNode;
import org.brutusin.commons.json.spi.JsonSchema;
import org.brutusin.commons.search.ActiveFacetMap;
import org.brutusin.commons.search.FacetResponse;
import org.brutusin.commons.search.FacetValueResponse;
import org.brutusin.commons.utils.CryptoUtils;
import org.brutusin.fleadb.FleaDB;
import org.brutusin.fleadb.FleaDBInfo;
import org.brutusin.fleadb.Schema;
import org.brutusin.fleadb.pagination.Paginator;
import org.brutusin.fleadb.pagination.PaginatorImpl;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public final class GenericFleaDB implements FleaDB<JsonNode> {

    public static final Version LUCENE_VERSION = Version.LUCENE_4_10_3;

    private final FleaDBInfo dsInfo;
    private final FleaTransformer transformer;
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

    public GenericFleaDB(File indexFolder) throws IOException {
        this(indexFolder, null);
    }

    public GenericFleaDB(JsonSchema jsonSchema) throws IOException {
        this(null, jsonSchema);
    }

    public GenericFleaDB(File indexFolder, JsonSchema jsonSchema) throws IOException {
        this(indexFolder, jsonSchema, false);
    }

    public GenericFleaDB(File indexFolder, JsonSchema jsonSchema, boolean ignoreHash) throws IOException {
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
                this.infoFile = new File(indexFolder, "flea.json");
                if (indexFolder.exists()) {
                    this.dsInfo = readFleaDBInfo();
                    if (this.dsInfo == null) {
                        throw new IllegalArgumentException("Unable to read schema from specified index folder '" + indexFolder + "'");
                    }
                    String hash = getHash(indexFolder);
                    if (dsInfo.getHash() == null) {
                        dsInfo.setHash(hash);
                        writeFleaDBInfo();
                    }
                    if (!ignoreHash && !dsInfo.getHash().equals(hash)) {
                        throw new IllegalStateException("Invalid index hash '" + hash + "'. Index at '" + indexFolder + "' has been modified externally");
                    }
                    if (schema != null && !this.dsInfo.getSchema().equals(schema)) {
                        throw new IllegalArgumentException("Specified schema is incompatible with current datasource for index at '" + indexFolder + "'");
                    }
                } else {
                    if (schema == null) {
                        throw new IllegalArgumentException("Index folder does not exist '" + indexFolder.getAbsolutePath() + "'");
                    }
                    this.dsInfo = new FleaDBInfo();
                    this.dsInfo.setSchema(schema);
                }
                this.indexDir = FSDirectory.open(indexFolder);
                this.facetDir = FSDirectory.open(new File(indexFolder, "facets"));
            }
            this.transformer = new FleaTransformer(this.dsInfo.getSchema());
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
                writeFleaDBInfo();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public JsonNode getSingleResult(Query q) {
        Paginator<JsonNode> paginator = query(q, Sort.RELEVANCE);
        if (paginator.getTotalHits() > 1) {
            throw new IllegalArgumentException("Query returned more than 1 results");
        }
        return paginator.getFirstElement();
    }

    @Override
    public final Paginator<JsonNode> query(final Query q, final Sort sort) {
        try {
            verifyNotClosed();
            return new PaginatorImpl<JsonNode>(getIndexSearcher(), this.transformer, q, sort);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public final List<FacetResponse> getFacetValues(final Query q, int maxFacetValues) {
        try {
            verifyNotClosed();
            List<String> allFacets = queryAllFacets();
            if (allFacets == null || allFacets.isEmpty()) {
                return null;
            }
            int[] maxFacetValuesArr = new int[allFacets.size()];
            for (int i = 0; i < maxFacetValuesArr.length; i++) {
                maxFacetValuesArr[i] = maxFacetValues;
            }
            return getFacetValues(q, allFacets, maxFacetValuesArr);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public final List<FacetResponse> getFacetValues(final Query q, ActiveFacetMap facets) {
        try {
            if (facets == null) {
                facets = new ActiveFacetMap();
            }
            List<String> facetNames = new ArrayList<String>();
            int[] maxFacetValues = new int[facets.size()];
            int i = 0;
            for (Map.Entry<String, Integer> entry : facets.entrySet()) {
                String string = entry.getKey();
                Integer integer = entry.getValue();
                facetNames.add(string);
                maxFacetValues[i] = integer;
                i++;
            }
            return getFacetValues(q, facetNames, maxFacetValues);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int getNumFacetValues(final Query q, final String facetName) {
        try {
            List<String> facetNames = new ArrayList<String>(1);
            facetNames.add(facetName);
            List<FacetResponse> frs = getFacetValues(q, facetNames, new int[]{1});
            return frs.get(0).getNumFacetValues();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private final List<FacetResponse> getFacetValues(final Query q, List<String> facetNames, int[] maxFacetValues) throws IOException {
        verifyNotClosed();
        if (facetNames == null || facetNames.size() == 0) {
            return null;
        }

        List<FacetResponse> ret = new ArrayList<FacetResponse>();
        FacetsCollector facetCollector = new FacetsCollector();
        getIndexSearcher().search(q, facetCollector);
        FacetsConfig config = new FacetsConfig();
        FastTaxonomyFacetCounts facets = new FastTaxonomyFacetCounts(getTaxonomyReader(), config, facetCollector);

        for (int i = 0; i < facetNames.size(); i++) {
            String facetName = facetNames.get(i);
            FacetResult res = facets.getTopChildren(maxFacetValues[i], facetName);
            if (res != null) {
                FacetResponse fr = new FacetResponse(facetName);
                fr.setNumFacetValues(res.childCount);
                ret.add(fr);
                LabelAndValue[] lvs = res.labelValues;
                for (int j = 0; j < lvs.length; j++) {
                    LabelAndValue lv = lvs[j];
                    FacetValueResponse fvresp = new FacetValueResponse(lv.label, lv.value.doubleValue());
                    fr.getFacetValues().add(fvresp);
                }
            } else {
                FacetResponse fr = new FacetResponse(facetName);
                fr.setNumFacetValues(0);
                ret.add(fr);
            }
        }
        return ret;
    }

    @Override
    public final List<FacetResponse> getFacetValuesStartingWith(String facetName, final String prefix, Query q, int max) {
        try {
            verifyNotClosed();
            BooleanQuery bq = new BooleanQuery();
            bq.add(q, BooleanClause.Occur.MUST);

            List<FacetResponse> startingResponse = getFacetValues(bq, Arrays.asList(new String[]{facetName}), new int[]{max});
            if (prefix != null) {
                double exactMultiplicity = getFacetValueMultiplicity(facetName, prefix, q);
                if (exactMultiplicity > 0) {
                    FacetValueResponse exactMatch = new FacetValueResponse(prefix, exactMultiplicity);
                    startingResponse.get(0).getFacetValues().remove(exactMatch);
                    startingResponse.get(0).getFacetValues().add(0, exactMatch);
                }
            }
            return startingResponse;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public final double getFacetValueMultiplicity(String facetName, String facetValue, Query q) {
        verifyNotClosed();
        BooleanQuery bq = new BooleanQuery();
        bq.add(q, BooleanClause.Occur.MUST);
        bq.add(new TermQuery(new Term(facetName, facetValue)), BooleanClause.Occur.MUST);
        Paginator pag = query(bq, Sort.RELEVANCE);
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
            this.getIndexWriter().deleteDocuments(q);
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
            writeFleaDBInfo();
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

    public final Paginator<JsonNode> getAllEntities() throws IOException {
        return query(new MatchAllDocsQuery(), Sort.RELEVANCE);
    }

    public final List<String> queryAllFacets() {
        verifyNotClosed();
        try {
            List<String> ret = new ArrayList<String>();

            FacetsCollector sfc = new FacetsCollector();
            getIndexSearcher().search(new MatchAllDocsQuery(), sfc);
            FacetsConfig config = new FacetsConfig();
            Facets facets = new FastTaxonomyFacetCounts(getTaxonomyReader(), config, sfc);
            for (FacetResult result : facets.getAllDims(1)) {
                ret.add(result.dim);
            }
            return ret;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void verifyNotClosed() {
        if (this.closed) {
            throw new IllegalStateException("Datasource has been closed");
        }
    }

    private static String getHash(File file) throws IOException {

        if (file == null || !file.exists()) {
            return null;
        }
        String ret = CryptoUtils.getHashMD5(file.getName());
        if (file.isFile()) {
            if (file.getName().equals("info.xml") || file.length() == 0 || file.getName().startsWith(".")) {
                return null;
            }
            ret = CryptoUtils.getHashMD5(ret + file.length());
        } else {
            File[] listFiles = file.listFiles();
            if (listFiles != null) {
                Arrays.sort(listFiles);
                for (int i = 0; i < listFiles.length; i++) {
                    String hash = getHash(listFiles[i]);
                    if (hash != null) {
                        ret = CryptoUtils.getHashMD5(ret + hash);
                    }
                }
            } else {
                return null;
            }
        }
        return ret;
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

    public static void main(String[] args) throws Exception {
        JsonSchema jsonSchema = JsonCodec.getInstance().parseSchema(JsonCodec.getInstance().getSchemaString(Record.class));
        GenericFleaDB db = new GenericFleaDB(jsonSchema);
        System.out.println(db.getFleaDBInfo().getSchema().getFacetFields());
        Record r = new Record();
        r.setId("jairo");
        r.setCategory(new String[]{"man", "supergay"});
        r.setAge(31);
        r.setDog(new Dog("perrin"));
        HashMap<String, Stuff> map = new HashMap<String, Stuff>();
        map.put("bici", new Stuff("orbea", 1));
        map.put("coche", new Stuff("fiat", 1));
        r.setStuff(map);
        db.store(JsonCodec.getInstance().parse(JsonCodec.getInstance().transform(r)));

        r = new Record();
        r.setId("nacho");
        r.setCategory(new String[]{"man"});
        r.setAge(35);
        map = new HashMap<String, Stuff>();
        map.put("lancha", new Stuff("narval", 1));
        map.put("bici", new Stuff("scott", 1));
        r.setStuff(map);
        db.store(JsonCodec.getInstance().parse(JsonCodec.getInstance().transform(r)));
        
        r = new Record();
        r.setId("bego");
        r.setCategory(new String[]{"woman"});
        r.setAge(35);
        map = new HashMap<String, Stuff>();
        map.put("maquina", new Stuff("singer", 1));
         map.put("bici", new Stuff("bh", 1));
        r.setStuff(map);
        db.store(JsonCodec.getInstance().parse(JsonCodec.getInstance().transform(r)));

        db.commit();
        Query q = new MatchAllDocsQuery();
        //        Query q = new TermQuery(new Term("$.category[#]", "guay"));
        //Query q = NumericRangeQuery.newLongRange("$.age", 0l, 33l, true, true);

        Sort sort = new Sort(new SortField("$.dog.name", SortField.Type.STRING, true));
        Paginator<JsonNode> pag = db.query(q, sort);

        System.out.println(db.getFleaDBInfo());
        System.out.println(db.getFleaDBInfo().getSchema().getIndexFields());
        System.out.println(db.getFleaDBInfo().getSchema().getFacetFields());
        int pageSize = 1;
        int totalPages = pag.getTotalPages(pageSize);
        long prev = System.currentTimeMillis();
        for (int i = 1; i <= totalPages; i++) {
            List<JsonNode> page = pag.getPage(i, pageSize);
            System.out.println("**** Page " + i);
            for (JsonNode rec : page) {
                System.out.println(rec);
            }
            long current = System.currentTimeMillis();
            System.out.println(current - prev + " ms");
            prev = current;
        }
        long start = System.currentTimeMillis();
        List<FacetResponse> facetValues = db.getFacetValues(q, 10);
        System.out.println("Facet time: " + (System.currentTimeMillis() - start));
        for (int i = 0; i < facetValues.size(); i++) {
            FacetResponse fr = facetValues.get(i);
            List<FacetValueResponse> facetValues1 = fr.getFacetValues();
            System.out.println(fr.getFacetName() + "\t" + fr.getNumFacetValues());
            for (int j = 0; j < facetValues1.size(); j++) {
                FacetValueResponse fvr = facetValues1.get(j);
                System.out.println("\t" + fvr.getValue() + "\t" + fvr.getMultiplicity());
            }
        }
    }

    static class Record {

        @IndexableProperty
        private String id;
        @IndexableProperty(mode = IndexableProperty.IndexMode.facet)
        private String[] category;
        @IndexableProperty(mode = IndexableProperty.IndexMode.facet)
        private Map<String, Stuff> stuff;
        @IndexableProperty
        private int age;

        private Dog dog;

        public Map<String, Stuff> getStuff() {
            return stuff;
        }

        public Dog getDog() {
            return dog;
        }

        public void setDog(Dog dog) {
            this.dog = dog;
        }

        public void setStuff(Map<String, Stuff> stuff) {
            this.stuff = stuff;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String[] getCategory() {
            return category;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public void setCategory(String[] category) {
            this.category = category;
        }
    }

    static class Stuff {

        @IndexableProperty(mode = IndexableProperty.IndexMode.facet)
        private String name;
        @IndexableProperty
        private int number;

        public Stuff(String name, int number) {
            this.name = name;
            this.number = number;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }
    }

    static class Dog {

        @IndexableProperty(mode = IndexableProperty.IndexMode.facet)
        private String name;

        public Dog(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }
}
