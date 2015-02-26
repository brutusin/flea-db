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
import org.junit.Test;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class PersistentFleaDBTest extends FleaDBTest {

    @Override
    protected File getIndexFolder() {
        return new File(System.getProperty("java.io.tmpdir"), "flea-db");
    }
    
    @Test
    public void test(){
        System.out.println("Temporary index folder at " + getIndexFolder());
    }
    
}
