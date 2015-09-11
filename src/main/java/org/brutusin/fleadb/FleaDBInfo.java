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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.brutusin.json.ParseException;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.json.spi.JsonNode;
import org.brutusin.commons.utils.Miscellaneous;
import org.brutusin.fleadb.impl.SchemaImpl;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class FleaDBInfo {

    private Schema schema;

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public Schema getSchema() {
        return schema;
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder("{\"jsonSchema\":");
        sb.append(getSchema().getJSONSChema());
        sb.append("}");
        try {
            return JsonCodec.getInstance().prettyPrint(sb.toString());
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static FleaDBInfo readFromFile(File f) throws ParseException, IOException {
        FleaDBInfo ret = new FleaDBInfo();
        String json = Miscellaneous.toString(new FileInputStream(f), "UTF-8");
        JsonNode jsonNode = JsonCodec.getInstance().parse(json);
        Schema schema = new SchemaImpl(JsonCodec.getInstance().parseSchema(jsonNode.get("jsonSchema").toString()));
        ret.setSchema(schema);
        return ret;
    }

    public static void writeToFile(FleaDBInfo fleaDBInfo, File f) throws IOException {
        Miscellaneous.writeStringToFile(f, fleaDBInfo.toString(), "UTF-8");
    }
}
