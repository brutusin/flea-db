//package org.brutusin.fleadb;
//
//import com.dreamgenics.commons.facets.FacetResponse;
//import com.dreamgenics.commons.facets.ActiveFacetMap;
//import java.io.IOException;
//import java.util.List;
//import org.apache.lucene.search.Query;
//
//public interface FacetDB {
//
//
//    public List<FacetResponse> getFacetValues(final Query q, ActiveFacetMap activeFacets) throws IOException;
//
//    public List<FacetResponse> getFacetValues(final Query q, int maxFacetValues) throws IOException;
//
//    public List<FacetResponse> getFacetValuesStartingWith(String facetName, String prefix, Query q, int max) throws IOException;
//
//    public int getNumFacetValues(Query q, String facetName) throws IOException;
//
//    public double getFacetValueMultiplicity(String facetName, String facetValue, Query q) throws IOException;
//
//}
