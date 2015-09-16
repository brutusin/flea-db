#org.brutusin:flea-db [![Build Status](https://api.travis-ci.org/brutusin/flea-db.svg?branch=master)](https://travis-ci.org/brutusin/flea-db) [![Maven Central Latest Version](https://maven-badges.herokuapp.com/maven-central/org.brutusin/flea-db/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.brutusin/flea-db/) 

A java library for creating standalone, portable, schema-full object databases supporting [pagination](http://en.wikipedia.org/wiki/Pagination#Pagination_in_web_content) and [faceted search](http://en.wikipedia.org/wiki/Faceted_search), and offering strong-typed and generic APIs.

Built on top of [Apache Lucene](http://lucene.apache.org/core/).

**Main features:**
* Schema-full/self-descriptive
* Simple and powerful API. Strong-typed and generic flavors
* High robustness. Record, field names and type validation.
* Pagination
* Faceted search
* In memory and persistent versions

**Table of Contents:** 

- [org.brutusin:flea-db](#)
  - [Motivation](#motivation)
  - [Maven dependency](#maven-dependency)
  - [APIs](#apis)
    - [GenericFleaDB](#genericfleadb)
    - [ObjectFleaDB](#objectfleadb)
  - [Schema](#schema)
    - [JSON SPI](#json-spi)
    - [JSON Schema extension](#json-schema-extension)
    - [Annotations](#annotations)
    - [Indexed fields nomenclature](#indexed-fields-nomenclature)
    - [Indexation values](#indexation-values)
  - [Usage](#usage)
    - [Database persistence](#database-persistence)
    - [Write operations](#write-operations)
      - [Store](#store)
      - [Delete](#delete)
      - [Commit](#commit)
      - [Optimization](#optimization)
    - [Read operations](#read-operations)
      - [Record queries](#record-queries)
      - [Facet queries](#facet-queries)
    - [Closing](#closing)
  - [Index structure](#index-structure)
  - [ACID properties](#acid-properties)
  - [Examples](#examples)
  - [Main stack](#main-stack)
  - [Lucene version](#lucene-version)
  - [Support, bugs and requests](#support-bugs-and-requests)
  - [Authors](#authors)
  - [License](#license)

##Motivation
* Create a library with a very simple API, self-descriptive with high robustness aimed at indexing objects and providing advanced search capabilities, pagination and faceted search. 
* Originally born with the purpose of indexing raw data files, and (almost) steady data sets.
* *Lucene* is an extense and powerful low level library, but its API is not very easy to understand.
* Putting schemas into play, self-description can be used to simplify API (fields type), to provide strong validation mechanisms, and to enable the creation of flexible and generic downstream components.
* *Lucene* has a lot of experimental APIs that may (and use to) change in time. This library adds a level of indirection. providing a stable high level interface. Upgrades in the underlying *Lucene* version are absorved by *flea-db*.

##Maven dependency 
```xml
<dependency>
    <groupId>org.brutusin</groupId>
    <artifactId>flea-db</artifactId>
</dependency>
```
Click [here](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.brutusin%22%20a%3A%22flea-db%22) to see the latest available version released to the Maven Central Repository.

If you are not using maven and need help you can ask [here](https://github.com/brutusin/flea-db/issues).

##APIs
All `flea-db` functionality is defined by [`FleaDB`](src/main/java/org/brutusin/fleadb/FleaDB.java) interface. 

The library provides two implementations for it:

1. A low-level generic implementation [`GenericFleaDB`](src/main/java/org/brutusin/fleadb/impl/GenericFleaDB.java).
2. A high-level strong-typed implementation [`ObjectFleaDB`](src/main/java/org/brutusin/fleadb/impl/ObjectFleaDB.java) built on top of the previous one.

###GenericFleaDB
[`GenericFleaDB`](src/main/java/org/brutusin/fleadb/impl/GenericFleaDB.java) is the lowest level *flea-db* implementation that defines the database schema using a JSON schema and stores and indexes records of type [`JsonNode`](https://github.com/brutusin/json/tree/master/src/main/java/org/brutusin/json/spi/JsonNode.java). It uses *Apache Lucene* APIs and [`org.brutusin:json` SPI](https://github.com/brutusin/json) to maintain two different indexes (one for the terms and other for the taxonomy, see [index structure](#index-structure)), hyding the underlying complexity from the user perspective.

This is how it works:
* **On instantiation**: A [`JsonSchema`](https://github.com/brutusin/json/tree/master/src/main/java/org/brutusin/json/spi/JsonSchema.java) and an index folder are passed depending on whether the database is new and/or persistent. Then the JSON schema (passed or readed from the existing database `flea.json` descriptor file) is processed, looking for its [`index`](#json-schema-extension) properties, and finally a database [schema](src/main/java/org/brutusin/fleadb/Schema.java) is created.
* **On storing**: The passed `JsonNode` record is validated against the JSON schema. Then a [`JsonTransformer`](src/main/java/org/brutusin/fleadb/impl/JsonTransformer.java) instance (making use of the processed database schema) transforms the records in terms understandable by *Lucene* (*documents*, *fields*, *facet fields* ...) and finally the storage is delegated to the *Lucene* API.
* **On commit**: Underlying index and taxonomy writters are commited and searchers are refreshed to reflect the changes.
* **On querying**: The [`Query`](src/main/java/org/brutusin/fleadb/query) and [`Sort`](src/main/java/org/brutusin/fleadb/sort/Sort.java) objects are transformed into terms understandable by *Lucene* making use of the database schema. The returned [paginator](src/main/java/org/brutusin/fleadb/pagination) is basically a wrapper around the underlying luecene `IndexSearcher` and `Query` objects that lazily (on demand) performs searches to the index.

###ObjectFleaDB
[`ObjectFleaDB`](src/main/java/org/brutusin/fleadb/impl/ObjectFleaDB.java) is built on top of `GenericFleaDB`.

Basically an `ObjectFleaDB` delegates all its functionality to a wrapped `GenericFleaDB` instance, making use of `org.brutusin:json` to perform transformations `POJO<->JsonNode` and `Class<->JsonSchema`. This is the reason why all `flea-db` databases can be used with `GenericFleaDB`.

## Schema
###JSON SPI
As cited before, this library makes use of the [`org.brutusin:json`](https://github.com/brutusin/json), so a JSON service provider like [`json-provider`](https://github.com/brutusin/json-provider) is needed at runtime. The choosen provider will determine JSON serialization, validation, parsing, schema generation and expression semantics.

###JSON Schema extension
Standard JSON schema specification has been extended to declare indexable properties (`"index":"index"` and `"index":"facet"` options). See [annotations section](#annotations) for more details.

Example:
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
* `"index":"index"`: Means that the property is indexed by *Lucene* under a field with name set according to the rules explained in [nomenclature section](#indexed-fields-nomenclature).
* `"index":"facet"`: Means that the property is indexed as in the previous case, but also a facet is created with this field name.

###Annotations
See [documentation in JSON SPI](https://github.com/brutusin/json/tree/master/src/main/java/org/brutusin/json/annotations) for supported annotations used in the strong-typed scenario.

###Indexed fields nomenclature
Databases are self descriptive, they provide information of their schema and indexed fields (via [`Schema`](src/main/java/org/brutusin/fleadb/Schema.java)). 

Field semantics are inherited from the expression semantics defined in the [`org.brutusin:json-provider`](https://github.com/brutusin/json-provider/blob/master/README.md#expression-dsl)

### Indexation values
Supose `JsonNode node` to be stored and let `fieldId` be the expression identifying a database field, according to the previous section. 

```java
Expression exp = JsonCodec.getInstance().compile(fieldId);
JsonSchema fieldSchema = exp.projectSchema(rootSchema);
JsonNode fieldNode = exp.projectNode(node);
```

Then, the following rules apply to extract index and facet values for that field:

|fieldSchema| index:index | index:facet
|--------|-------------|------------
|String|`fieldNode.asString()`|`fieldNode.asString()`
|Boolean|`fieldNode.asString()`|`fieldNode.asString()`
|Integer|`fieldNode.asLong()`| Unsupported
|Number|`fieldNode.asDouble()`|Unsupported
|Object|each of its property names|each of its property names
|Array|recurse for each of its elements|recurse for each of its elements

##Usage
### Database persistence
Databases can be created in RAM memory or in disk, depending on the addressed problem characteristics (performance, dataset size, indexation time ...).

In order to create a persistent database, a constructor(s) with a `File` argument has to be choosen:
```java
Flea db1 = new GenericFleaDB(indexFolder, jsonSchema);
// or
Flea db2 = new ObjectFleaDB(indexFolder, Record.class);
```
>NOTE: Multiple instances can be used to read the same persistent database (for example different concurrent JVM executions), but only one can hold the writing file-lock (claimed the first time a write method is called).

On the other side, the database will be kept in RAM memory and lost at the end of the JVM execution.
```java
Flea db1 = new GenericFleaDB(jsonSchema);
// or
Flea db2 = new ObjectFleaDB(Record.class);
```

### Write operations
The following operations perform modifications on the database.
#### Store
In order to store a record the `store(...)` method has to be used:
```java 
db1.store(jsonNode);
// or
db2.store(record);
```
internally this ends up calling `addDocument` in the underlying *Lucene* `IndexWriter`.
#### Delete
The API enables to delete a set of records using `delete(Query q)`.
>NOTE: Due to Lucene facet internals, categories are never deleted from the taxonomy index, despite of being orphan.

#### Commit
Previous operations (store and delete) are not (and won't ever be) visible until `commit()` is called. Underlying seachers and writers are released, to be lazily created in further read or write operations.

#### Optimization
Databases can be optimized in order to achieve a better performance by using `optimize()`. This method triggers a highly costly (in terms of free disk space needs and computation) merging of the *Lucene* index segments into a single one. 

Nevertheless, this operation is useful for immutable databases, that can be once optimized prior its usage.

### Read operations
Two kind of read operations can be performed, both supporting a [Query](src/main/java/org/brutusin/fleadb/query) argument, that defines the search criteria.

#### Record queries
Record queries can be [paginated](http://en.wikipedia.org/wiki/Pagination#Pagination_in_web_content) and the ordering of the results can be specified via a [Sort](src/main/java/org/brutusin/fleadb/sort/Sort.java) argument.

* `public E getSingleResult(final Query q)`
* `public Paginator<E> query(final Query q)`
* `public Paginator<E> query(final Query q, final Sort sort)`

#### Facet queries
[`FacetResponse`](src/main/java/org/brutusin/fleadb/facet/FacetResponse.java) represents the faceting info returned by the database.

* `public List<FacetResponse> getFacetValues(final Query q, FacetMultiplicities activeFacets)`
* `public List<FacetResponse> getFacetValues(final Query q, int maxFacetValues)`
* `public List<FacetResponse> getFacetValuesStartingWith(String facetName, String prefix, Query q, int max)`
* `public int getNumFacetValues(Query q, String facetName)`
* `public double getFacetValueMultiplicity(String facetName, String facetValue, Query q)`

Faceting is provided by [lucene-facet](http://lucene.apache.org/core/4_10_3/facet/index.html).

### Closing
Databases must be closed after its usage, via `close()` method in order to free the resources and locks hold. Closing a database makes it no longer usable.

##Threading issues
Both implementations are thread safe and can be shared across multiple threads.

##Index structure
Persistent *flea-db* databases create the following index structure: 
```
/flea-db/
|-- flea.json
|-- record-index
|   |-- ...
|-- taxonomy-index
|   |-- ...
```
being `flea.json` the database descriptor containing its schema, and being `record-index` and `taxonomy-index` subfolders the underlying *Lucene* index structures.

##ACID properties
`flea-db` offers the following [ACID](http://en.wikipedia.org/wiki/ACID) properties, inherited from *Lucene* ones:

* **Atomicity:** When changes are performed, and then committed, either all (if the commit succeeds) or none (if the commit fails) of them will be visible.
* **Consistency:** if the computer or OS crashes, or the JVM crashes or is killed, or power is lost, indexes will remain intact (ie, not corrupt).
* **Isolation:** Changes performed are not visible until committed. 
* **Durability:** In case of a persistent database, when the commit returns, all changes have been written to disk. If the JVM crashes, all changes will still be present in the index, despite of not the database not being properly closed.

##Examples:
**Generic API:**
```java 
// Generic interaction with a previously created database
FleaDB<JsonNode> db = new GenericFleaDB(indexFolder);

// Store records
JsonNode json = JsonCodec.getInstance.parse("...");
db.store(json);
db.commit();

// Query records
Query q = Query.createTermQuery("$.id", "0");
Paginator<JsonRecord> paginator = db.query(q);
int totalPages = paginator.getTotalPages(pageSize);
for (int i = 1; i <= totalPages; i++) {
    List<JsonRecord> page = paginator.getPage(i, pageSize);
    for (int j = 0; j < page.size(); j++) {
        JsonRecord json = page.get(j);
        System.out.println(json);
    }
}
db.close();
```

**Strong-typed API:**  
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

