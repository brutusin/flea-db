package org.brutusin.fleadb;

import javax.xml.validation.Schema;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.brutusin.fleadb.pagination.Paginator;

public interface LuceneDB<E>{

    public E getSingleResult(final Query q);

    public Paginator<E> query(final Query q, final Sort sort);

    public void store(E entity);

    public void delete(Query q);

    public Schema getSchema();

    public void commit();

    public void close();

}
