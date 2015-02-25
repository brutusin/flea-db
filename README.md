#org.brutusin:flea-db 

-*Documentation under development. Not released to maven-central yet*-

A java library for creating standalone, portable, schema-full object databases supporting pagination and faceted search, and offering strong-typed and generic APIs.

Built on top of [Apache Lucene](http://lucene.apache.org/core/).

**Table of Contents** 

- [org.brutusin:flea-db](#)
  - [Motivation](#motivation)
  - [Main features](#main-features)
  - [APIs](#apis)
    - [GenericFleaDB](#genericfleadb)
    - [ObjectFleaDB](#objectfleadb)
  - [Schema](#schema)
    - [JSON SPI](#json-spi)
    - [JSON Schema extension](#json-schema-extension)
    - [Annotations](#annotations)
    - [Indexed fields nomenclature](#indexed-fields-nomenclature)
  - [Usage](#usage)
    - [Database persistence](#database-persistence)
    - [Store and commit](#store-and-commit)
    - [Queries and sorting](#queries-and-sorting)
    - [Pagination](#pagination)
    - [Faceting](#faceting)
    - [Optimization](#optimization)
    - [Closing](#closing)
  - [Example tests](#example-tests)
  - [Main stack](#main-stack)
  - [Lucene version](#lucene-version)
  - [Support, bugs and requests](#support-bugs-and-requests)
  - [Authors](#authors)
  - [License](#license)

##Motivation
* Create a library with a very simple API, self-descriptive with high robustness aimed at indexing objects and providing advanced search capabilities, pagination and faceted search. 
* Originally born with the purpose of indexing raw data files, and (almost) steady data sets.
* Lucene is an extense and powerful low level library, but its API is not very easy to understand.
* Putting schemas into play, self-description can be used to simplify API (fields type), to provide strong validation mechanisms, and to enable the creation of flexible and generic downstream components.
* Lucene has a lot of experimental APIs that may (and use to) change in time. This library adds a level of indirection. providing a stable high level interface. Upgrades in the underlying Lucene version are absorved by flea-db.

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
All *flea-db* functionality is defined by [FleaDB](src/main/java/org/brutusin/fleadb/FleaDB.java) interface. 

The library provides two implementations for it:

1. A low-level generic implementation [GenericFleaDB](src/main/java/org/brutusin/fleadb/impl/GenericFleaDB.java).
2. A high-level strong-typed implementation [ObjectFleaDB](src/main/java/org/brutusin/fleadb/impl/ObjectFleaDB.java) built on top of the previous one.

###GenericFleaDB
[GenericFleaDB](src/main/java/org/brutusin/fleadb/impl/GenericFleaDB.java) is the lowest level *flea-db* implementation that defines the database schema using a JSON schema and stores and indexes records of type `JsonNode`. It directly uses *Apache Lucene* APIs and [JSON SPI](https://github.com/brutusin/commons/blob/master/README.md#json-spi) to maintain two different indexes (one for the terms and other for the taxonomy), hyding the underlying complexity from the user perspective.

This is how it works:
* **On instantiation**: A `JsonSchema` (from  [JSON SPI](https://github.com/brutusin/commons/blob/master/README.md#json-spi)) and an index folder are passed depending on whether the database is new and/or persistent. Then the JSON schema (passed or readed from the existing database `flea.json` descriptor file) is processed, looking for its [`index`](#json-schema-extension) properties, and finally a database [Schema](src/main/java/org/brutusin/fleadb/Schema.java) is created.
* **On storing**: The passed `JsonNode` record is validated against the JSON schema. Then a [JsonTransformer](src/main/java/org/brutusin/fleadb/impl/JsonTransformer.java) instance (making use of the processed database schema) transforms the records in terms understandable by Lucene (Document, Fields, FacetField ...) and finally the storage is delegated to the Lucene API.
* **On commit**: Underlying index and taxonomy writters are commited and searchers are refreshed to reflect the changes.
* **On querying**: The [Query](src/main/java/org/brutusin/fleadb/query) and [Sort](src/main/java/org/brutusin/fleadb/sort/Sort.java) objects are transformed into terms understandable by Lucene making use of the database schema. The returned [Paginator](src/main/java/org/brutusin/fleadb/pagination) is basically a wrapper around the underlying luecene `IndexSearcher` and `Query` objects that lazily (on demand) performs searches to the index.

**Example:**
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

**Example:**  
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
Databases are self descriptive, they provide information of their schema and indexed fields (via [Schema](src/main/java/org/brutusin/fleadb/Schema.java)). Nevertheless, the nomenclature of the fields is defined as follows:

Case | Field name
-----| ---------
Simple property| `$.id`
Nested property| `$.header.id`
Array/Collection property| `$.items[#]`
Map property (additionalProperty in schema)| `$.map` for keys and `$.map[*]` for values

##Usage
### Database persistence
### Store and commit
### Queries and sorting
### Pagination
### Faceting
### Optimization
### Closing

##Example tests
See available [test classes](src/test/java/org/brutusin/fleadb/impl/) for more examples.

##Main stack
This module could not be possible without:
* [Apache Lucene](http://lucene.apache.org/core/).
* The following [json-codec-jackson](https://github.com/brutusin/json-codec-jackson) dependencies:
  * [FasterXML/jackson stack](https://github.com/FasterXML/jackson): The underlying JSON stack.
  * [com.fasterxml.jackson.module:jackson-module-jsonSchema](https://github.com/FasterXML/jackson-module-jsonSchema): For java class to JSON schema mapping.
  * [com.github.fge:json-schema-validator](https://github.com/fge/json-schema-validator): For validation against a JSON schema.

##Lucene version
`4.10.3` (Dec, 2014)

## Support, bugs and requests
https://github.com/brutusin/flea-db/issues

## Authors

- Ignacio del Valle Alles (<https://github.com/idelvall/>)

Contributions are always welcome and greatly appreciated!

##License
Apache License, Version 2.0
http://www.apache.org/licenses/LICENSE-2.0

