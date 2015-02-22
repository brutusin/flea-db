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
package org.brutusin.fleadb.pagination;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.brutusin.fleadb.DocTransformer;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class PaginatorImpl<E> implements Paginator<E> {

    private final IndexSearcher searcher;
    private final Query q;
    private final Sort sort;
    private final DocTransformer<E> transformer;

    private Integer totalHits;

    public PaginatorImpl(IndexSearcher searcher, DocTransformer<E> transformer, Query q, Sort sort) {
        if (sort == null) {
            sort = Sort.INDEXORDER;
        }
        this.searcher = searcher;
        this.q = q;
        this.sort = sort;
        this.transformer = transformer;
    }

    public E getFirstElement() {
        return getPage(1, 1).get(0);
    }

    public int getTotalHits() {
        if (totalHits == null) {
            try {
                TopDocsCollector documentCollector = TopFieldCollector.create(
                        this.sort, 1, null, false, false, false, false);
                searcher.search(this.q, documentCollector);
                this.totalHits = documentCollector.getTotalHits();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return this.totalHits;
    }

    public int getTotalPages(int pageSize) {
        return (int) getTotalPages(pageSize, getTotalHits());
    }

    public List<E> getPage(int pageNum, int pageSize) {
        if (pageNum < 1) {
            throw new IllegalArgumentException("pageNum must be greater than 0");
        }
        if (pageNum > getTotalPages(pageSize)) {
            throw new IllegalArgumentException("pageNum (" + pageNum + ") exceeds pageSize (" + pageSize + ")");
        }
        int page = 1;
        TopDocs topDocs = null;
        try {
            while (page <= pageNum) {
                FieldDoc memento;
                if (topDocs == null) {
                    memento = null;
                } else {
                    memento = (FieldDoc) topDocs.scoreDocs[topDocs.scoreDocs.length - 1];
                }
                topDocs = queryDocuments(pageSize, memento);
                page++;
            }
            List<E> ret = new ArrayList<E>(topDocs.scoreDocs.length);
            for (int i = 0; i < topDocs.scoreDocs.length; i++) {
                FieldDoc fieldDoc = (FieldDoc) topDocs.scoreDocs[i];
                Document doc = searcher.doc(fieldDoc.doc);
                ret.add(this.transformer.documentToEntity(doc));
            }
            return ret;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private TopDocs queryDocuments(int pageSize, FieldDoc memento) throws IOException {
        TopDocsCollector documentCollector = TopFieldCollector.create(
                this.sort, pageSize, memento, true, false, false, false);
        searcher.search(this.q, documentCollector);
        this.totalHits = documentCollector.getTotalHits();
        return documentCollector.topDocs(0, pageSize);
    }

    public IndexSearcher getSearcher() {
        return searcher;
    }

    public Query getQ() {
        return q;
    }

    public Sort getSort() {
        return sort;
    }

    public DocTransformer<E> getTransformer() {
        return transformer;
    }

    private static long getTotalPages(int pageSize, long totalRecords) {
        long r = totalRecords % pageSize;
        if (r == 0) {
            return totalRecords / pageSize;
        }
        return totalRecords / pageSize + 1;
    }
}
