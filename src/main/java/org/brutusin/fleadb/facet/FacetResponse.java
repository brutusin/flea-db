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
package org.brutusin.fleadb.facet;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class FacetResponse {

    private int numFacetValues;

    public int getNumFacetValues() {
        return numFacetValues;
    }

    public void setNumFacetValues(int numFacetValues) {
        this.numFacetValues = numFacetValues;
    }
    private List<FacetValueResponse> facetValues;
    protected String facetName;

    public FacetResponse() {
        this.facetValues = new ArrayList<FacetValueResponse>();
    }

    public FacetResponse(String facetName) {
        this.facetName = facetName;
        this.facetValues = new ArrayList<FacetValueResponse>();
    }

    public List<FacetValueResponse> getFacetValues() {
        return facetValues;
    }

    public void setFacetValues(List<FacetValueResponse> facetValues) {
        this.facetValues = facetValues;
    }

    public String getFacetName() {
        return facetName;
    }

    public void setFacetName(String facetName) {
        this.facetName = facetName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FacetResponse) {
            return this.facetName.equals(((FacetResponse) obj).getFacetName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.facetName.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.facetName).append(":\n").append(numFacetValues);
        for (int i = 0; i < facetValues.size(); i++) {
            FacetValueResponse facetValueResponse = facetValues.get(i);
            sb.append("\n\t");
            sb.append(facetValueResponse);
        }
        return sb.toString();
    }
}
