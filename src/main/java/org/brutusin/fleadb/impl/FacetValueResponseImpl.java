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

import org.brutusin.fleadb.facet.*;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public final class FacetValueResponseImpl implements FacetValueResponse {

    public static final Comparator COMPARATOR = new Comparator();
    private double multiplicity;
    private String value;

    public FacetValueResponseImpl() {
    }

    public FacetValueResponseImpl(String value, double multiplicity) {
        this.multiplicity = multiplicity;
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public double getMultiplicity() {
        return multiplicity;
    }

    public void setMultiplicity(double multiplicity) {
        this.multiplicity = multiplicity;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value + "(" + this.multiplicity + ")";
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof FacetValueResponseImpl)) {
            return false;
        }
        return this.value.equals(((FacetValueResponseImpl) obj).value);
    }

    static class Comparator implements java.util.Comparator<FacetValueResponseImpl> {

        private Comparator() {
        }

        public int compare(FacetValueResponseImpl o1, FacetValueResponseImpl o2) {
            return -Double.compare(o1.multiplicity, o2.multiplicity);
        }
    }
}
