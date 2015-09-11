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
package org.brutusin.fleadb.record;

import java.util.Map;
import java.util.Set;
import org.brutusin.json.annotations.IndexableProperty;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class Record {

    @IndexableProperty
    private String id;
    @IndexableProperty(mode = IndexableProperty.IndexMode.facet)
    private String[] categories;
    @IndexableProperty(mode = IndexableProperty.IndexMode.facet)
    private Map<String, Component> components;
    @IndexableProperty
    private int age;
    @IndexableProperty
    private Set<Integer> integerSet;

    private Component mainComponent;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String[] getCategories() {
        return categories;
    }

    public void setCategories(String[] categories) {
        this.categories = categories;
    }

    public Map<String, Component> getComponents() {
        return components;
    }

    public void setComponents(Map<String, Component> components) {
        this.components = components;
    }

    public Set<Integer> getIntegerSet() {
        return integerSet;
    }

    public void setIntegerSet(Set<Integer> integerSet) {
        this.integerSet = integerSet;
    }
    
    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Component getMainComponent() {
        return mainComponent;
    }

    public void setMainComponent(Component mainComponent) {
        this.mainComponent = mainComponent;
    }
}