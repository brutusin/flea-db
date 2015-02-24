#org.brutusin:flea-db 

-*Documentation under development. Not released to maven-central yet*-

A tiny, embeddable, schema-full, java-based, object database supporting pagination, faceted search, and both strong-typed and generic API's. Built on top of Apache Lucene.

**Examples:** 
```java 
// Create object database
FleaDB<Record> db = new ObjectFleaDB(indexFolder, Record.class);

// Store records
for (int i = 0; i < REC_NO; i++) {
    Record r = new Record();
    // ... populate record
    db.store(r);
}
db.commit();

// Query records
Query q = Query.createTermQuery("$.id", "0");
Paginator<Record> paginator = db.query(q);
int totalPages = paginator.getTotalPages(pageSize);
for (int i = 1; i <= totalPages; i++) {
    List<Record> page = paginator.getPage(i, pageSize);
    for (int j = 0; j < page.size(); j++) {
        Record r = page.get(j);
        System.out.println(r);
    }
}
db.close();
``` 

```java 
// Generic interaction with the previously created database
FleaDB<JsonNode> gdb = new GenericFleaDB(indexFolder);

// Both for storing ...
JsonNode json = JsonCodec.getInstance.parse("...");
gdb.store(json);
gdb.commit();

// ... and querying:
Paginator<JsonRecord> paginator = gdb.query(q); // same query instance
totalPages = paginator.getTotalPages(pageSize);
for (int i = 1; i <= totalPages; i++) {
    List<JsonRecord> page = paginator.getPage(i, pageSize);
    for (int j = 0; j < page.size(); j++) {
        JsonRecord json = page.get(j);
        System.out.println(json);
    }
}
gdb.close();
```

##Main features
* **Schema-full**: Based on [JSON Schema](http://json-schema.org/).
* **Strong typed API**: Using `<E>` for records and generating database schema from `Class<E>`.
* **Generic API**: Using JSON for records and JSON Schema for database schema.
* **Record validation**
* **Field name and type validation**: For queries and sorting.
* [**Pagination**](http://en.wikipedia.org/wiki/Pagination#Pagination_in_web_content)
* [**Faceted search**](http://en.wikipedia.org/wiki/Faceted_search): Powered by [lucene-facet](http://lucene.apache.org/core/4_10_3/facet/index.html).
* **In memory/persistent versions**

## Schema
###JSON SPI
This module makes use of the [JSON SPI](https://github.com/brutusin/commons/blob/master/README.md#json-spi), so a JSON service provider like [json-codec-jackson](https://github.com/brutusin/json-codec-jackson) is needed at runtime. The choosen provider will determine JSON serialization, validation, parsing and schema generation.

###JSON Schema extension
Standard JSON schema specification has been extended to declare indexable properties (`"index":"index"` and `"index":"facet"` options):
```json
{
  "type": "object",
  "properties": {
    "age": {
      "type": "integer",
      "index": "index"
    },
    "category": {
      "type": "string",
      "index": "facet"
    }
  }
}
```
* `"index":"index"`: Means that the property is indexed by Lucene under a field with name set according to the rules explained in [nomenclature section](#indexed-fields-nomenclature).
* `"index":"facet"`: Means that the property is indexed as in the previous case, but also a facet is created with this field name.

###Annotations
See [JSON SPI](https://github.com/brutusin/commons/blob/master/README.md#json-spi) for supported annotations used in the strong-typed scenario.

###Indexed fields nomenclature

##Example tests
See available [test classes](src/test/java/org/brutusin/fleadb/impl/) for more details.

##Main stack
This module could not be possible without:
* [Apache Lucene](http://lucene.apache.org/core/).
* The following [json-codec-jackson](https://github.com/brutusin/json-codec-jackson) dependencies:
  * [FasterXML/jackson stack](https://github.com/FasterXML/jackson): The underlying JSON stack.
  * [com.fasterxml.jackson.module:jackson-module-jsonSchema](https://github.com/FasterXML/jackson-module-jsonSchema): For java class to JSON schema mapping.
  * [com.github.fge:json-schema-validator](https://github.com/fge/json-schema-validator): For validation against a JSON schema.

##Currently used Lucene version
`4.10.3` (Dec, 2014)

## Support, bugs and requests
https://github.com/brutusin/flea-db/issues

## Authors

- Ignacio del Valle Alles (<https://github.com/idelvall/>)

Contributions are always welcome and greatly appreciated!

##License
Apache License, Version 2.0
http://www.apache.org/licenses/LICENSE-2.0

