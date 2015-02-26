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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.brutusin.fleadb.record.Component;
import org.brutusin.fleadb.record.Record;
import org.junit.After;
import org.junit.Before;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class FleaDBTest {

    protected ObjectFleaDB<Record> db;
    
    protected int getMaxRecords(){
        return 20;
    }

    @Before
    public void setUp() {
        try {
            db = new ObjectFleaDB(getIndexFolder() , Record.class);
            for (int i = 0; i < getMaxRecords(); i++) {
                Record r = new Record();
                r.setId(String.valueOf(i));
                r.setAge(i);
                String[] categories = new String[]{"mod2:" + i % 2, "mod3:" + i % 3};
                r.setCategories(categories);
                if (i > 5) {
                    Map<String, Component> components = new HashMap();
                    components.put("component-" + (i < 10), new Component("item " + i, i));
                    r.setComponents(components);
                }
                db.store(r);
            }
            db.commit();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    protected File getIndexFolder(){
        return null;
    }

    @After
    public void tearDown() {
        db.close();
    }
}
