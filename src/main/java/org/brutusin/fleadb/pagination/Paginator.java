package org.brutusin.fleadb.pagination;

import java.util.List;

public interface Paginator<E> {

    public int getTotalHits();

    public int getTotalPages(int pageSize);

    public List<E> getPage(int pageNum, int pageSize);
    
    public E getFirstElement();
    
}
