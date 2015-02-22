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
import org.brutusin.commons.json.ParseException;
import org.brutusin.commons.json.spi.JsonCodec;
import org.brutusin.commons.json.spi.JsonNode;
import org.brutusin.commons.utils.Miscellaneous;
import org.brutusin.fleadb.impl.SchemaImpl;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class FleaDBInfo {

    private Schema schema;
    private String hash;

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public Schema getSchema() {
        return schema;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder("{\"jsonSchema\":");
        sb.append(getSchema().getJSONSChema());
        String hash = getHash();
        if (hash != null) {
            sb.append(",\"hash\":\"");
            sb.append(hash);
            sb.append("\"}");
        } else {
            sb.append("}");
        }
        try {
            return JsonCodec.getInstance().prettyPrint(sb.toString());
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static FleaDBInfo readFromFile(File f) throws ParseException, IOException {
        String json = Miscellaneous.toString(new FileInputStream(f), "UTF-8");
        JsonNode jsonNode = JsonCodec.getInstance().parse(json);
        Schema schema = new SchemaImpl(JsonCodec.getInstance().parseSchema(jsonNode.get("jsonSchema").toString()));
        String hash = jsonNode.get("hash").toString();
        FleaDBInfo ret = new FleaDBInfo();
        ret.setHash(hash);
        ret.setSchema(schema);
        return ret;
    }

    public static void writeToFile(FleaDBInfo fleaDBInfo, File f) throws IOException {
        Miscellaneous.writeStringToFile(f, fleaDBInfo.toString(), "UTF-8");
    }
}
