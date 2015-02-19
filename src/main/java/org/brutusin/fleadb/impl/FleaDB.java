//package org.brutusin.fleadb.impl;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import org.apache.commons.io.FileUtils;
//import org.apache.lucene.document.Document;
//import org.apache.lucene.facet.FacetField;
//import org.apache.lucene.facet.FacetResult;
//import org.apache.lucene.facet.Facets;
//import org.apache.lucene.facet.FacetsCollector;
//import org.apache.lucene.facet.FacetsConfig;
//import org.apache.lucene.facet.LabelAndValue;
//import org.apache.lucene.facet.taxonomy.TaxonomyReader;
//import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
//import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
//import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
//import org.apache.lucene.index.*;
//import org.apache.lucene.search.BooleanClause;
//import org.apache.lucene.search.BooleanQuery;
//import org.apache.lucene.search.IndexSearcher;
//import org.apache.lucene.search.MatchAllDocsQuery;
//import org.apache.lucene.search.PrefixQuery;
//import org.apache.lucene.search.Query;
//import org.apache.lucene.search.Sort;
//import org.apache.lucene.search.TermQuery;
//import org.apache.lucene.store.Directory;
//import org.apache.lucene.store.FSDirectory;
//import org.apache.lucene.store.RAMDirectory;
//import org.apache.lucene.util.Version;
//import org.brutusin.fleadb.FacetDB;
//import org.brutusin.fleadb.LuceneDB;
//import org.brutusin.fleadb.Schema;
//
//public final class FleaDB implements LuceneDB<String>, FacetDB {
//
//    public static final Version LUCENE_VERSION = Version.LUCENE_4_10_3;
//    private final DSInfo dsInfo;
//    private final List<String> dynamicFieldFamilies;
//    private final StringJsonTransformer transformer;
//    private final Directory indexDir;
//    private final Directory facetDir;
//
//    private final File indexFolder;
//    private FacetsConfig facetsConfig;
//
//    private boolean closed = false;
//    private File infoFile;
//
//    // Double-Checked Locking instances
//    private volatile IndexSearcher indexSearcher;
//    private volatile IndexWriter indexWriter;
//    private volatile TaxonomyReader taxonomyReader;
//    private volatile TaxonomyWriter taxonomyWriter;
//    private volatile Schema schema;
//
//    public FleaDB(File indexFolder) throws IOException {
//        this(indexFolder, null);
//    }
//
//    public FleaDB(File indexFolder, Schema definitionSchema) throws IOException {
//        this(indexFolder, definitionSchema, false);
//    }
//
//    public FleaDB(File indexFolder, Schema definitionSchema, boolean ignoreHash) throws IOException {
//        try {
//            this.indexFolder = indexFolder;
//            // In memory datasource
//            if (indexFolder == null) {
//                if (definitionSchema == null) {
//                    throw new IllegalArgumentException("In-memory datasources require a schema");
//                }
//                this.dsInfo = new DSInfo();
//                this.dsInfo.setSchema(definitionSchema);
//                this.indexDir = new RAMDirectory();
//                this.facetDir = new RAMDirectory();
//
//                // Disk datasource
//            } else {
//                this.infoFile = new File(indexFolder, "info.xml");
//                if (indexFolder.exists()) {
//                    this.dsInfo = readDSInfo();
//                    if (this.dsInfo == null) {
//                        throw new IllegalArgumentException("Unable to read schema from specified index folder '" + indexFolder + "'");
//                    }
//                    String hash = getHash(indexFolder);
//                    if (dsInfo.getHash() == null) {
//                        dsInfo.setHash(hash);
//                        writeDSInfo();
//                    }
//                    if (!ignoreHash && !dsInfo.getHash().equals(hash)) {
//                        throw new IllegalStateException("Invalid index hash '" + hash + "'. Index at '" + indexFolder + "' has been modified externally");
//                    }
//                    if (definitionSchema != null && !this.dsInfo.getSchema().equals(definitionSchema)) {
//                        throw new IllegalArgumentException("Specified schema is incompatible with current datasource for index at '" + indexFolder + "'");
//                    }
//                } else {
//                    if (definitionSchema == null) {
//                        throw new IllegalArgumentException("Index folder does not exist '" + indexFolder.getAbsolutePath() + "'");
//                    }
//                    this.dsInfo = new DSInfo();
//                    this.dsInfo.setSchema(definitionSchema);
//                }
//                this.indexDir = FSDirectory.open(indexFolder);
//                this.facetDir = FSDirectory.open(new File(indexFolder, "facets"));
//            }
//            this.dynamicFieldFamilies = getDynamicFieldFamilies(this.dsInfo.getSchema());
//            this.transformer = new StringJsonTransformer(this.dsInfo.getSchema());
//            this.facetsConfig = new FacetsConfig();
//            Set<String> facets = getCurrentSchema().getFacets();
//            for (String facet : facets) {
//                boolean multievaluated = facet.contains("[*]") || facet.contains("[#]");
//                facetsConfig.setMultiValued(facet, multievaluated);
//            }
//        } catch (Throwable th) {
//            close();
//            if (th instanceof IOException) {
//                throw (IOException) th;
//            } else if (th instanceof RuntimeException) {
//                throw (RuntimeException) th;
//            } else {
//                throw (Error) th;
//            }
//        }
//    }
//
//    public File getIndexFolder() {
//        return indexFolder;
//    }
//
//    private IndexSearcher getIndexSearcher() throws IOException {
//        if (indexSearcher == null) {
//            synchronized (this) {
//                if (indexSearcher == null) {
//                    indexSearcher = new IndexSearcher(DirectoryReader.open(indexDir));
//                }
//            }
//        }
//        return indexSearcher;
//    }
//
//    private IndexWriter getIndexWriter() throws IOException {
//        if (indexWriter == null) {
//            synchronized (this) {
//                if (indexWriter == null) {
//                    IndexWriterConfig config = new IndexWriterConfig(LUCENE_VERSION, null);
//                    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
//                    this.indexWriter = new IndexWriter(indexDir, config);
//                }
//            }
//        }
//        return indexWriter;
//    }
//
//    private TaxonomyReader getTaxonomyReader() throws IOException {
//        if (taxonomyReader == null) {
//            synchronized (this) {
//                if (taxonomyReader == null) {
//                    taxonomyReader = new DirectoryTaxonomyReader(facetDir);
//                }
//            }
//        }
//        return taxonomyReader;
//    }
//
//    private TaxonomyWriter getTaxonomyWriter() throws IOException {
//        if (taxonomyWriter == null) {
//            synchronized (this) {
//                if (taxonomyWriter == null) {
//                    taxonomyWriter = new DirectoryTaxonomyWriter(facetDir, IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
//                }
//            }
//        }
//        return taxonomyWriter;
//    }
//
//    protected final DSInfo getDSInfo() {
//        return dsInfo;
//    }
//
//    private static List<String> getDynamicFieldFamilies(Schema schema) {
//        Set<String> keySet = schema.getFields().keySet();
//        List<String> ret = new ArrayList<String>();
//        for (String fieldName : keySet) {
//            if (fieldName.contains("[*]") || fieldName.contains("[#]")) {
//                ret.add(fieldName);
//            }
//        }
//        if (ret.isEmpty()) {
//            return null;
//        }
//        return ret;
//    }
//
//    private DSInfo readDSInfo() {
//        if (!this.infoFile.exists()) {
//            return null;
//        }
//        return (DSInfo) Serializer.getInstance().fromXML(this.infoFile);
//    }
//
//    @Override
//    public synchronized void close() throws IOException {
//        if (this.closed) {
//            throw new IllegalStateException("Datasource has been closed already");
//        }
//        closed = true;
//        if (this.taxonomyReader != null) {
//            this.taxonomyReader.close();
//        }
//        if (this.taxonomyWriter != null) {
//            this.taxonomyWriter.close();
//        }
//        if (this.indexSearcher != null) {
//            this.indexSearcher.getIndexReader().close();
//        }
//        if (this.indexWriter != null) {
//            this.indexWriter.close();
//            writeDSInfo();
//        }
//    }
//
//    @Override
//    public String getSingleResult(Query q) throws IOException {
//        Paginator<String> paginator = query(q, Sort.RELEVANCE);
//        if (paginator.getTotalHits() > 1) {
//            throw new IllegalArgumentException("Query returned more than 1 results");
//        }
//        return paginator.getFirstElement();
//    }
//
//    @Override
//    public final Paginator<String> query(final Query q, final Sort sort) throws IOException {
//        return query(q, sort, null);
//    }
//
//    @Override
//    public final Paginator<String> query(final Query q, final Sort sort, Acceptor<String> documentAcceptor) throws IOException {
//        verifyNotClosed();
//        return new PaginatorImpl<String>(getIndexSearcher(), this.transformer, q, sort, getAcceptor(documentAcceptor));
//    }
//
//    @Override
//    public final List<FacetResponse> getFacetValues(final Query q, int maxFacetValues) throws IOException {
//        verifyNotClosed();
//        List<String> allFacets = queryAllFacets();
//        if (allFacets == null || allFacets.isEmpty()) {
//            return null;
//        }
//        int[] maxFacetValuesArr = new int[allFacets.size()];
//        for (int i = 0; i < maxFacetValuesArr.length; i++) {
//            maxFacetValuesArr[i] = maxFacetValues;
//        }
//        return getFacetValues(q, allFacets, maxFacetValuesArr, null);
//    }
//
//    @Override
//    public final List<FacetResponse> getFacetValues(final Query q, ActiveFacetMap facets) throws IOException {
//        if (facets == null) {
//            facets = new ActiveFacetMap();
//        }
//        List<String> facetNames = new ArrayList<String>();
//        int[] maxFacetValues = new int[facets.size()];
//        int i = 0;
//        for (Map.Entry<String, Integer> entry : facets.entrySet()) {
//            String string = entry.getKey();
//            Integer integer = entry.getValue();
//            facetNames.add(string);
//            maxFacetValues[i] = integer;
//            i++;
//        }
//        return getFacetValues(q, facetNames, maxFacetValues, null);
//    }
//
//    public int getNumFacetValues(final Query q, final String facetName) throws IOException {
//        List<String> facetNames = new ArrayList<String>(1);
//        facetNames.add(facetName);
//        List<FacetResponse> frs = getFacetValues(q, facetNames, new int[]{1}, null);
//        return frs.get(0).getNumFacetValues();
//    }
//
//    private final List<FacetResponse> getFacetValues(final Query q, List<String> facetNames, int[] maxFacetValues, Acceptor<String> acceptor) throws IOException {
//        verifyNotClosed();
//        if (facetNames == null || facetNames.size() == 0) {
//            return null;
//        }
//
//        List<FacetResponse> ret = new ArrayList<FacetResponse>();
//        FacetsCollector facetCollector = new FacetsCollector();
//        getIndexSearcher().search(q, facetCollector);
//        FacetsConfig config = new FacetsConfig();
//        DGTaxonomyFacets facets = new DGTaxonomyFacets(getTaxonomyReader(), config, facetCollector);
//
//        for (int i = 0; i < facetNames.size(); i++) {
//            String facetName = facetNames.get(i);
//            FacetResult res = facets.getTopChildren(maxFacetValues[i], facetName, acceptor);
//            if (res != null) {
//                FacetResponse fr = new FacetResponse(facetName);
//                fr.setNumFacetValues(res.childCount);
//                ret.add(fr);
//                LabelAndValue[] lvs = res.labelValues;
//                for (int j = 0; j < lvs.length; j++) {
//                    LabelAndValue lv = lvs[j];
//                    FacetValueResponse fvresp = new FacetValueResponse(lv.label, lv.value.doubleValue());
//                    fr.getFacetValues().add(fvresp);
//                }
//            } else {
//                FacetResponse fr = new FacetResponse(facetName);
//                fr.setNumFacetValues(0);
//                ret.add(fr);
//            }
//        }
//        return ret;
//    }
//
//    @Override
//    public final List<FacetResponse> getFacetValuesStartingWith(String facetName, final String prefix, Query q, int max) throws IOException {
//        verifyNotClosed();
//        BooleanQuery bq = new BooleanQuery();
//        bq.add(q, BooleanClause.Occur.MUST);
//        Acceptor<String> acceptor;
//        if (prefix != null) {
//            bq.add(new PrefixQuery(new Term(facetName, prefix)), BooleanClause.Occur.MUST);
//            acceptor = new Acceptor<String>() {
//                public boolean accept(String str) {
//                    return str.toLowerCase().startsWith(prefix.toLowerCase());
//                }
//            };
//        } else {
//            acceptor = null;
//        }
//        List<FacetResponse> startingResponse = getFacetValues(bq, Arrays.asList(new String[]{facetName}), new int[]{max}, acceptor);
//        if (prefix != null) {
//            double exactMultiplicity = getFacetValueMultiplicity(facetName, prefix, q);
//            if (exactMultiplicity > 0) {
//                FacetValueResponse exactMatch = new FacetValueResponse(prefix, exactMultiplicity);
//                startingResponse.get(0).getFacetValues().remove(exactMatch);
//                startingResponse.get(0).getFacetValues().add(0, exactMatch);
//            }
//        }
//        return startingResponse;
//    }
//
//    @Override
//    public final double getFacetValueMultiplicity(String facetName, String facetValue, Query q) throws IOException {
//        verifyNotClosed();
//        BooleanQuery bq = new BooleanQuery();
//        bq.add(q, BooleanClause.Occur.MUST);
//        bq.add(new TermQuery(new Term(facetName, facetValue)), BooleanClause.Occur.MUST);
//        Paginator pag = query(bq, Sort.RELEVANCE);
//        return pag.getTotalHits();
//    }
//
//    @Override
//    public Schema getCurrentSchema() {
//        verifyNotClosed();
//        if (currentSchema == null) {
//            synchronized (this) {
//                if (currentSchema == null) {
//                    if (this.dynamicFieldFamilies == null) {
//                        return dsInfo.getSchema();
//                    }
//                    currentSchema = new Schema();
//                    Map<String, IndexType> fieldMap = new HashMap<String, IndexType>();
//                    currentSchema.setFields(fieldMap);
//                    Map<String, IndexType> staticFieldMap = dsInfo.getSchema().getFields();
//                    for (Map.Entry<String, IndexType> entry : staticFieldMap.entrySet()) {
//                        String fieldName = entry.getKey();
//                        IndexType indexType = entry.getValue();
//                        fieldMap.put(fieldName, indexType);
//                    }
//                    Set<String> dynamicFields = dsInfo.getDynamicFields();
//                    if (dynamicFields != null) {
//                        for (String dynField : dynamicFields) {
//                            String family = LuceneUtils.getFamily(dynField, this.dynamicFieldFamilies);
//                            if (family == null) {
//                                throw new AssertionError();
//                            }
//                            fieldMap.put(dynField, staticFieldMap.get(family));
//                        }
//                    }
//                    currentSchema.setDefinition(dsInfo.getSchema().getJsonNode().toString());
//                }
//            }
//        }
//        return currentSchema;
//    }
//
//    @Override
//    public final Schema getDefinitionSchema() {
//        verifyNotClosed();
//        return this.dsInfo.getSchema();
//    }
//
//    @Override
//    public final void delete(Query q) throws IOException {
//        verifyNotClosed();
//        this.getIndexWriter().deleteDocuments(q);
//    }
//
//    @Override
//    public final void commit() throws IOException {
//        verifyNotClosed();
//        this.getTaxonomyWriter().commit();
//        this.getIndexWriter().commit();
//        refresh();
//        writeDSInfo();
//    }
//
//    @Override
//    public final void store(String entity) throws IOException {
//        verifyNotClosed();
//        Pair<Document, List<FacetField>> pair = this.transformer.entityToDocument(entity);
//        Document doc = pair.getElement1();
//        List<FacetField> facetFields = pair.getElement2();
//        if (facetFields != null) {
//            for (int i = 0; i < facetFields.size(); i++) {
//                String facetName = facetFields.get(i).dim;
//                boolean multievaluated = facetName.contains("[*]") || facetName.contains("[#]");
//                if (multievaluated) {
//                    facetsConfig.setMultiValued(facetName, multievaluated);
//                }
//                doc.add(facetFields.get(i));
//            }
//        }
//        this.getIndexWriter().addDocument(this.facetsConfig.build(getTaxonomyWriter(), doc));
//        List<IndexableField> fields = doc.getFields();
//        for (int i = 0; i < fields.size(); i++) {
//            IndexableField indexableField = fields.get(i);
//            if (indexableField.fieldType().indexed()) {
//                if (!getDefinitionSchema().getFields().containsKey(indexableField.name())) {
//                    // Hay campos internos metidos en el documento por lucene, por ejemplo $facets. Estos no se tiene en cuenta
//                    if (LuceneUtils.getFamily(indexableField.name(), getDefinitionSchema().getFields().keySet()) == null) {
//                        continue;
//                    }
//                    Set<String> dynamicFields = getDSInfo().getDynamicFields();
//                    if (dynamicFields == null) {
//                        dynamicFields = new HashSet<String>();
//                        getDSInfo().setDynamicFields(dynamicFields);
//                    }
//                    dynamicFields.add(indexableField.name());
//                }
//            }
//        }
//    }
//
//    public final Paginator<String> getAllEntities() throws IOException {
//        return query(new MatchAllDocsQuery(), Sort.RELEVANCE);
//    }
//
//    public final List<String> queryAllFacets() {
//        verifyNotClosed();
//        try {
//            List<String> ret = new ArrayList<String>();
//
//            FacetsCollector sfc = new FacetsCollector();
//            getIndexSearcher().search(new MatchAllDocsQuery(), sfc);
//            FacetsConfig config = new FacetsConfig();
//            Facets facets = new DGTaxonomyFacets(getTaxonomyReader(), config, sfc);
//            for (FacetResult result : facets.getAllDims(1)) {
//                ret.add(result.dim);
//            }
//            return ret;
//        } catch (IOException ex) {
//            throw new RuntimeException(ex);
//        }
//    }
//
//    private final Acceptor<Document> getAcceptor(final Acceptor<String> entityAcceptor) {
//        if (entityAcceptor == null) {
//            return null;
//        }
//        return new Acceptor<Document>() {
//            public boolean accept(Document t) {
//                String entity = transformer.documentToEntity(t);
//                return entityAcceptor.accept(entity);
//            }
//        };
//    }
//
//    private void verifyNotClosed() {
//        if (this.closed) {
//            throw new IllegalStateException("Datasource has been closed");
//        }
//    }
//
//    private void writeDSInfo() throws IOException {
//        if (infoFile != null) { // In disk mode
//            if (infoFile.exists()) {
//                FileUtils.deleteQuietly(infoFile);
//            }
//            DSInfo dsInfo = getDSInfo();
//            dsInfo.setHash(getHash(indexFolder));
//            String str = Serializer.getInstance().toXML(dsInfo);
//            FileUtils.writeStringToFile(infoFile, str, "UTF-8");
//        }
//    }
//
//    private static String getHash(File file) throws IOException {
//
//        if (file == null || !file.exists()) {
//            return null;
//        }
//        String ret = CryptoUtils.getHashMD5(file.getName());
////        System.out.println("md5 of " + file.getName() +" " + ret);
////        System.out.println("length of " + file.getName() +" " + file.length());
//        if (file.isFile()) {
//            if (file.getName().equals("info.xml") || file.length() == 0 || file.getName().startsWith(".")) {
//                return null;
//            }
//            ret = CryptoUtils.getHashMD5(ret + file.length());
//        } else {
//            File[] listFiles = file.listFiles();
//            if (listFiles != null) {
//                Arrays.sort(listFiles);
//                for (int i = 0; i < listFiles.length; i++) {
//                    String hash = getHash(listFiles[i]);
//                    if (hash != null) {
//                        ret = CryptoUtils.getHashMD5(ret + hash);
//                    }
//                }
//            } else {
//                return null;
//            }
//        }
////       System.out.println("computing hash of " + file.getName() + " " + ret);
//        return ret;
//    }
//
//    public void optimize() throws IOException {
//        verifyNotClosed();
//        getIndexWriter().forceMergeDeletes();
//        getIndexWriter().forceMerge(1);
//        optimizeFacetIndex();
//        commit();
//    }
//
//    private synchronized void optimizeFacetIndex() throws IOException {
//        /*
//         Taxonomy writer merge policy is not clear:
//         http://lucene.apache.org/core/4_10_2/facet/org/apache/lucene/facet/taxonomy/directory/DirectoryTaxonomyWriter.html#createIndexWriterConfig(org.apache.lucene.index.IndexWriterConfig.OpenMode)
//         */
//
//        //Next leads to errors:
////        if (taxonomyWriter != null) {
////            taxonomyWriter.close();
////            taxonomyWriter = null;
////        }
////        IndexWriterConfig config = new IndexWriterConfig(JsonDataSource.LUCENE_VERSION, null);
////        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
////        IndexWriter iw = new IndexWriter(facetDir, config);
////        try {
////            //iw.forceMergeDeletes();
////            iw.forceMerge(1);
////        } finally {
////            iw.close();
////        }
//    }
//
//    private synchronized void refresh() {
//        this.indexSearcher = null;
//        this.taxonomyReader = null;
//        this.currentSchema = null;
//    }
//
//    public static void main(String[] args) throws Exception {
//        FleaDB ds = new FleaDB(new File("E:\\dreamgenics\\data\\genomicds\\cosmic\\71-1\\.index"));
//        ds.optimize();
//
////        BooleanQuery q = new BooleanQuery();
////        
////        q.add(new TermQuery(new Term("refName", "1")), BooleanClause.Occur.MUST);
////        q.add(new TermQuery(new Term("refName", "2")), BooleanClause.Occur.SHOULD);
//        Query q = new MatchAllDocsQuery();
//
//        ds.getFacetValues(q, 1);
//        ds.query(q, null);
//        long start = System.currentTimeMillis();
//        ActiveFacetMap afm = new ActiveFacetMap();
//        afm.put("refName", 10);
//        List<FacetResponse> facetValues = ds.getFacetValues(q, 10);
//        System.out.println("Facet time: " + (System.currentTimeMillis() - start));
////        for (int i = 0; i < facetValues.size(); i++) {
////            FacetResponse fr = facetValues.get(i);
////            List<FacetValueResponse> facetValues1 = fr.getFacetValues();
////            System.out.println(fr.getFacetName() + "\t" + fr.getNumFacetValues());
////            for (int j = 0; j < facetValues1.size(); j++) {
////                FacetValueResponse fvr = facetValues1.get(j);
////                System.out.println("\t" + fvr.getValue() + "\t" + fvr.getMultiplicity());
////            }
////        }
//        // System.out.println(ds.getNumFacetValues(new MatchAllDocsQuery(), "refName"));
//        int pageSize = 20;
//
//        Paginator<String> pag = ds.query(q, null);
//        start = System.currentTimeMillis();
//        System.out.println(pag.getTotalHits());
//        int totalPages = pag.getTotalPages(pageSize);
//        double max = 0;
//        for (int i = 1; i <= totalPages; i++) {
//            //long s = System.currentTimeMillis();
//            pag.getPage(i, pageSize);
//            // System.out.println(System.currentTimeMillis() - s);
//            max++;
//            if (i > 20) {
//                break;
//            }
//        }
//        System.out.println("Query time: " + (System.currentTimeMillis() - start) / max);
//
////        int pageSize = 20;
////        int totalPages = pag.getTotalPages(pageSize);
////        long prev = System.currentTimeMillis();
////        for (int i = 1; i <= totalPages; i++) {
////            List<String> page = pag.getPage(i, pageSize);
////            System.out.println("**** Page " + i);
//////            for (String rec : page) {
//////                System.out.println(rec);
//////            }
////            long current = System.currentTimeMillis();
////            System.out.println(current - prev + " ms");
////            prev = current;
////        }
////       
////        ds.optimize();
////        ds.close();
//    }
//}
