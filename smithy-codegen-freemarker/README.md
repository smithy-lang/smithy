# FreeMarker template integration for SmithyCodegen

This package integrates [FreeMarker](https://freemarker.apache.org/)
with SmithyCodegen.


## Install

Maven

```xml
<dependency>
    <groupId>software.amazon.smithy</groupId>
    <artifactId>smithy-codegen-freemarker</artifactId>
    <version>0.9.10</version>
</dependency>
```

Note: this package is pre-installed and provided by the Smithy CLI. Jars
dropped in the module path for the CLI do not need to include this
dependency.


## Usage

Given the following template,

```freemarker
Hello, ${name}
```

and the following code,

```java
// Create the template engine.
var engine = FreeMarkerEngine.builder()
        .classLoader(getClass().getClassLoader())
        .build();

// Create a data model.
Map<String, Object> dataModel = new HashMap<>();
dataModel.put("name", "Roosevelt");

// Render a template.
var result = engine.render("com/foo/baz/test.ftl", dataModel);
```

the template engine would render:

```
Hello, Roosevelt
```


## Data model

A *data model* is passed to the template engine when rendering templates.
The data model is a map of strings to objects that are interpreted by
FreeMarker when rendering templates. Smithy will automatically configure
FreeMarker to treat Smithy's Node values like normal Java built-in types:

* An `ObjectNode` is converted to a `Map`.
* An `ArrayNode` is converted to a `List`.
* A `StringNode` is converted to a `String`.
* A `BooleanNode` is converted to a `Boolean`.
* A `NumberNode` is converted to a `Number`.
* A `NullNode` is converted to a `null`, so use a trailing `?` or `!`
  in your templates when dealing with a `NullNode`.

Smithy will configure FreeMarker to understand some additional Java
types that were added after FreeMarker was created:

* An empty `Optional` is automatically converted to `null`.
* An `Optional` with a value is automatically unwrapped.
* A `Stream` is converted to an `Iterator`.
