//package org.brutusin.fleadb.pagination;
//
//import com.dreamgenics.commons.bean.Acceptor;
//import com.dreamgenics.commons.lucene.transformers.DocTransformer;
//import com.dreamgenics.commons.lucene.utils.LuceneUtils;
//import com.dreamgenics.commons.utils.Miscellaneous;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import org.apache.lucene.document.Document;
//import org.apache.lucene.search.FieldDoc;
//import org.apache.lucene.search.IndexSearcher;
//import org.apache.lucene.search.Query;
//import org.apache.lucene.search.Sort;
//import org.apache.lucene.search.TopDocs;
//import org.apache.lucene.search.TopDocsCollector;
//import org.apache.lucene.search.TopFieldCollector;
//
///*
// * Property of DREAMgenics S.L. All rights reserved
// */
///**
// *
// * @author Ignacio del Valle Alles
// */
//public class PaginatorImpl<E> implements Paginator<E> {
//
//    private final IndexSearcher searcher;
//    private final Query q;
//    private final Sort sort;
//    private final DocTransformer<E> transformer;
//    private final Acceptor<Document> documentAcceptor;
//
//    private Integer totalHits;
//
//    public PaginatorImpl(IndexSearcher searcher, DocTransformer<E> transformer, Query q, Sort sort, Acceptor<Document> documentAcceptor) {
//        if (sort == null) {
//            sort = Sort.INDEXORDER;
//        }
//        this.documentAcceptor = documentAcceptor;
//        this.searcher = searcher;
//        this.q = q;
//        this.sort = sort;
//        this.transformer = transformer;
//    }
//
//    public E getFirstElement() {
//        return getPage(1, 1).get(0);
//    }
//
//    public int getTotalHits() {
//        if (totalHits == null) {
//            try {
//                TopDocsCollector documentCollector = TopFieldCollector.create(
//                        this.sort, 1, null, false, false, false, false);
//                searcher.search(this.q, documentCollector);
//                this.totalHits = documentCollector.getTotalHits();
//            } catch (IOException ex) {
//                throw new RuntimeException(ex);
//            }
//        }
//        return this.totalHits;
//    }
//
//    public int getTotalPages(int pageSize) {
//        return (int) Miscellaneous.getTotalPages(pageSize, getTotalHits());
//    }
//
//    public List<E> getPage(int pageNum, int pageSize) {
//        if (pageNum < 1) {
//            throw new IllegalArgumentException("pageNum must be greater than 0");
//        }
//        if (pageNum > getTotalPages(pageSize)) {
//            throw new IllegalArgumentException("pageNum (" + pageNum + ") exceeds pageSize (" + pageSize + ")");
//        }
//        int page = 1;
//        TopDocs topDocs = null;
//        try {
//            while (page <= pageNum) {
//                FieldDoc memento;
//                if (topDocs == null) {
//                    memento = null;
//                } else {
//                    memento = (FieldDoc) topDocs.scoreDocs[topDocs.scoreDocs.length - 1];
//                }
//                topDocs = queryDocuments(pageSize, memento);
//                page++;
//            }
//            List<E> ret = new ArrayList<E>(topDocs.scoreDocs.length);
//            for (int i = 0; i < topDocs.scoreDocs.length; i++) {
//                FieldDoc fieldDoc = (FieldDoc) topDocs.scoreDocs[i];
//                Document doc = searcher.doc(fieldDoc.doc);
//                ret.add(this.transformer.documentToEntity(doc));
//            }
//            return ret;
//        } catch (IOException ex) {
//            throw new RuntimeException(ex);
//        }
//    }
//
//    private TopDocs queryDocuments(int pageSize, FieldDoc memento) throws IOException {
//        TopDocsCollector documentCollector = TopFieldCollector.create(
//                this.sort, pageSize, memento, true, false, false, false);
//        if (this.documentAcceptor != null) {
//            documentCollector = LuceneUtils.proxifyCollector(documentCollector, documentAcceptor);
//        }
//        searcher.search(this.q, documentCollector);
//        this.totalHits = documentCollector.getTotalHits();
//        return documentCollector.topDocs(0, pageSize);
//    }
//
//    public IndexSearcher getSearcher() {
//        return searcher;
//    }
//
//    public Query getQ() {
//        return q;
//    }
//
//    public Sort getSort() {
//        return sort;
//    }
//
//    public Acceptor<Document> getDocumentAcceptor() {
//        return documentAcceptor;
//    }
//
//    public DocTransformer<E> getTransformer() {
//        return transformer;
//    }
//
//    public static void main(String[] args) throws Exception {
//        //See paginationTest
//    }
//}
