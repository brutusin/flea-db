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
package org.brutusin.fleadb.sort;

import java.util.ArrayList;
import java.util.List;
import org.brutusin.fleadb.Schema;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class Sort {

    private final List<SortField> sortFields = new ArrayList();

    public static Sort by(String field){
        return new Sort(field);
    }
    
    public static Sort by(SortField sortField){
        return new Sort(sortField);
    }
    
    public static Sort by(String field, boolean reverse){
        return new Sort(field, reverse);
    }
    
    private Sort(String field) {
        this(field, false);
    }

    private Sort(String field, boolean reverse) {
        this(new SortField(field, reverse));
    }

    private Sort(SortField sortField) {
        this.sortFields.add(sortField);
    }

    public Sort thenBy(String field) {
        return thenBy(field, false);
    }

    public Sort thenBy(String field, boolean reverse) {
        return thenBy(new SortField(field, reverse));
    }

    public Sort thenBy(SortField sortField) {
        this.sortFields.add(sortField);
        return this;
    }

    public org.apache.lucene.search.Sort getLuceneSort(Schema schema) {
        org.apache.lucene.search.Sort ret = new org.apache.lucene.search.Sort();
        org.apache.lucene.search.SortField[] sfs = new org.apache.lucene.search.SortField[this.sortFields.size()];
        for (int i = 0; i < sortFields.size(); i++) {
            SortField sortField = sortFields.get(i);
            sfs[i] = sortField.getLuceneSortField(schema);
        }
        ret.setSort(sfs);
        return ret;
    }
}
