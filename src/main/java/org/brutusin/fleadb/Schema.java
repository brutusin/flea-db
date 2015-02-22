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
package org.brutusin.fleadb;

import java.util.Map;
import java.util.Set;
import org.brutusin.commons.json.spi.JsonNode;

/**
 * Flea-db database schema.
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public interface Schema {

    /**
     * Returns the JSON-Schema governing the structure of the records
     *
     * @return
     */
    public String getJSONSChema();

    /**
     * Returns the lucene index field names and their type, that can be used in
     * queries
     *
     * @return
     */
    public Map<String, JsonNode.Type> getIndexFields();

    /**
     * Returns the lucene facet names, and if they are multievaluated
     *
     * @return
     */
    public Map<String, Boolean> getFacetFields();
}
