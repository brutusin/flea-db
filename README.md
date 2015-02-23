#org.brutusin:flea-db 

-*Documentation under development. Not released to maven-central yet*-

A tiny, embeddable, schema-full, java-based, object database supporting pagination, faceted search, and both object-mapping and generic API's. Built on top of Apache Lucene.

**Example:** 
```java 
// Create object database
ObjectFleaDB db = new ObjectFleaDB(indexFolder, Record.class);

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

// Generic interaction with the previously created database
GenericFleaDB gdb = new GenericFleaDB(indexFolder);

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
* **Object-mapping API**: Using `<E>` for records and generating database schema from `Class<E>`.
* **Generic API**: Using JSON for records and JSON Schema for database schema.
* **Record validation**
* **Field name and type validation**: For queries and sorting.
* **Pagination**
* **Faceted search**
* **In memory/disk versions**


##Example
See available [test classes](src/test/java/org/brutusin/fleadb/impl/) for more details.

## Implementation details
###JSON SPI
This module makes use of the [JSON SPI](https://github.com/brutusin/commons/tree/master/src/main/java/org/brutusin/commons/json/spi), so a JSON service provider like [json-codec-jackson](https://github.com/brutusin/json-codec-jackson) is needed at runtime. The choosen provider will determine JSON serialization, validation, parsing and schema generation.

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

