#org.brutusin:flea-db 

-*Documentation under development. Not released to maven-central yet*-

A tiny, embeddable, schema-full, java-based, **object database** supporting pagination, faceted search, and both strong-typed and generic APIs. 

Built on top of [Apache Lucene](http://lucene.apache.org/core/).

##Main features
* **Schema-full**: Based on [JSON Schema](http://json-schema.org/).
* **Strong-typed API**: Using `<E>` for records and generating database schema from `Class<E>`.
* **Generic API**: Using JSON for records and JSON Schema for database schema.
* **Record validation**
* **Field name and type validation**: For queries and sorting.
* [**Pagination**](http://en.wikipedia.org/wiki/Pagination#Pagination_in_web_content)
* [**Faceted search**](http://en.wikipedia.org/wiki/Faceted_search): Powered by [lucene-facet](http://lucene.apache.org/core/4_10_3/facet/index.html).
* **In memory/persistent versions**

##APIs
*flea-db* functionality is defined in the interface [FleaDB](src/main/java/org/brutusin/fleadb/FleaDB.java). 

The library provides two implementations for it, a low-level generic implementation [GenericFleaDB](src/main/java/org/brutusin/fleadb/impl/GenericFleaDB.java) and high-level strong-typed implementation [ObjectFleaDB](src/main/java/org/brutusin/fleadb/impl/ObjectFleaDB.java).

###GenericFleaDB
[GenericFleaDB](src/main/java/org/brutusin/fleadb/impl/GenericFleaDB.java) is the lowest level implementation that directly uses *Apache Lucene* and [JSON SPI](https://github.com/brutusin/commons/blob/master/README.md#json-spi) to maintain two different indexes (one for the terms and other for the taxonomy), hyding the underlying complexity from the user perspective.

This is how it works:
* **On instantiation**: A `JsonSchema` (from  [JSON SPI](https://github.com/brutusin/commons/blob/master/README.md#json-spi)) and an index folder are passed depending on the database being newly created and persistent. Then the JSON schema (passed or readed from the existing database `flea.json` descriptor file) is processed, looking for its [`index`](#json-schema-extension) properties, and finally a database [Schema](src/main/java/org/brutusin/fleadb/Schema.java) is created.
* **On storing**: The passed `JsonNode` record is validated against the JSON schema. Then a [JsonTransformer](src/main/java/org/brutusin/fleadb/impl/JsonTransformer.java) instance (making use of the processed database schema) transforms the records in terms understandable by Lucene (Document, Fields, FacetField ...) and finally the storage is delegated to the Lucene API.
* **On querying**: 
The [Query](src/main/java/org/brutusin/fleadb/query) and [Sort](src/main/java/org/brutusin/fleadb/sort/Sort.java) objects are transformed into terms understandable by Lucene making use of the database schema. The returned [Paginator](src/main/java/org/brutusin/fleadb/pagination) is basically a wrapper around the underlying luecene `IndexSearcher` and `Query` objects that lazily (on demand) performs searches to the index.

Example:
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
###ObjectFleaDB
[ObjectFleaDB](src/main/java/org/brutusin/fleadb/impl/ObjectFleaDB.java) is built on top of *GenericFleaDB*.

Basically an *ObjectFleaDB* delegates all its functionality to a wrapped *GenericFleaDB* instance, making use of [JSON SPI](https://github.com/brutusin/commons/blob/master/README.md#json-spi) to perform transformations `POJO<->JsonNode` and `Class<->JsonSchema`. This is the reason why all *flea-db* databases can be used with *GenericFleaDB*.

Example:  
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
## Schema
###JSON SPI
As cited before, this library makes use of the [JSON SPI](https://github.com/brutusin/commons/blob/master/README.md#json-spi), so a JSON service provider like [json-codec-jackson](https://github.com/brutusin/json-codec-jackson) is needed at runtime. The choosen provider will determine JSON serialization, validation, parsing and schema generation.

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
Databases are self descriptive, they provide information of their schema and indexed fields. Nevertheless, the nomenclature of the fields is defined as follows:

Case | Field name
-----| ---------
Simple property| `$.id`
Nested property| `$.header.id`
Array/Collection property| `$.items[#]`
Map property (additionalProperty in schema)| `$.map` for keys and `$.map[*]` for values

##Example tests

See available [test classes](src/test/java/org/brutusin/fleadb/impl/) for more examples.

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

