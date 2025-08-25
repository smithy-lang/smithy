# Smithy Trait CodeGen

This package contains a Smithy build plugin that generates Java trait classes from customized Smithy traits defined with [`@trait`](https://smithy.io/2.0/spec/model.html#trait-trait).

## Supported Trait Types

- [**Simple Types**](https://smithy.io/2.0/spec/simple-types.html), excluding `Blob` and `Boolean`
- **Structure**, including [annotation traits](https://smithy.io/2.0/spec/model.html#annotation-traits)
- **List**
- **Set**
- **Map**

> [!NOTE]
> [Annotation traits](https://smithy.io/2.0/spec/model.html#annotation-traits) are recommended instead of Boolean traits.

## Supported Smithy Prelude Traits

- [@required](https://smithy.io/2.0/spec/type-refinement-traits.html#required-trait)
- [@enumValue](https://smithy.io/2.0/spec/type-refinement-traits.html#enumvalue-trait)
- [@mixin](https://smithy.io/2.0/spec/type-refinement-traits.html#mixin-trait)
- [@idRef](https://smithy.io/2.0/spec/constraint-traits.html#idref-trait)
- [@uniqueItems](https://smithy.io/2.0/spec/constraint-traits.html#uniqueitems-trait)
- [@deprecatedTrait](https://smithy.io/2.0/spec/documentation-traits.html#deprecated-trait)
- [@documentation](https://smithy.io/2.0/spec/documentation-traits.html#documentation-trait)
- [@externalDocumentation](https://smithy.io/2.0/spec/documentation-traits.html#externaldocumentation-trait)
- [@timestampFormat](https://smithy.io/2.0/spec/protocol-traits.html#timestampformat-trait)

## Configuration

The generator supports the following configuration options:

| Option | Description | Required |
|--------|-------------|----------|
| `package` | Java package name for generated classes | Yes |
| `namespace` | Smithy namespace to search for traits | Yes |
| `header` | Header lines to include in generated files | Yes |
| `excludeTags` | Smithy tags to exclude from generation | No |

An example for `smithy-build.json`:

```json
{
  "version": "1.0",
  "plugins": {
    "trait-codegen": {
      "package": "com.example.traits",
      "namespace": "com.example.traits",
      "header": ["Copyright Example Corp"],
      "excludeTags": ["internal"]
    }
  }
}
```

## Getting Started

### Configure `smithy-build.json`

To generate Java code for your customized traits, you will add the `trait-codegen` plugin to the
[plugin configuration](https://smithy.io/2.0/guides/smithy-build-json.html) in `smithy-build.json`:

```json
{
  "version": "1.0",
  "sources": ["model"],
  "plugins": {
    "trait-codegen": {
      "package": "io.examples.traits",
      "namespace": "example.traits",
      "header": ["This is my header!"]
    }
  }
}
```

### Add dependency

#### **Using Smithy CLI**

If you are using the Smithy CLI, declare a dependency in `smithy-build.json`:

```json
{
    "version": "1.0",
    "sources": ["model"],
    "maven": {
      "dependencies": [
        "software.amazon.smithy:smithy-trait-codegen:1.61.0"
      ]
    }
    "...": "..."
}
```

Then, running `smithy build` will build the model and generate Java code for your customized traits.

#### **Using Gradle**

If you are using Gradle, declare a dependency in the `plugin` section in `build.gradle.kts`:

```kotlin
plugins {
    id("software.amazon.smithy.gradle.smithy-trait-package").version("1.3.0")
}

dependencies {
    smithyCli("software.amazon.smithy:smithy-cli:1.61.0")
    implementation("software.amazon.smithy:smithy-model:1.61.0")
}

repositories {
    mavenLocal()
    mavenCentral()
}
```

Then, running `gradle build` will build the model and generate Java code for your customized traits.

### Check the Generated Code

Finally, you can find the generated Java classes under `/build/smithyprojections/trait-codegen/`

The generated Java class for `annotationTrait` from `custom-trait` [template](https://github.com/smithy-lang/smithy-examples/tree/main/custom-trait-examples/custom-trait) would be:

```java
@SmithyGenerated
public final class AnnotationTrait extends AbstractTrait implements ToSmithyBuilder<AnnotationTrait> {
    public static final ShapeId ID = ShapeId.from("example.traits#annotationTrait");

    private AnnotationTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
    }

    @Override
    protected Node createNode() {
        return Node.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .build();
    }

    public static AnnotationTrait fromNode(Node node) {
        Builder builder = builder().sourceLocation(node);
        node.expectObjectNode();
        return builder.build();
    }

    // Builder Implementation

    // Service Provider Implementation
}
```

The generated Java class for `JsonNameTrait` from `custom-trait` [template](https://github.com/smithy-lang/smithy-examples/tree/main/custom-trait-examples/custom-trait) would be:

```java
@SmithyGenerated
public final class JsonNameTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("example.traits#jsonName");

    public JsonNameTrait(String value) {
        super(ID, value, SourceLocation.NONE);
    }

    public JsonNameTrait(String value, FromSourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
    }

    // Builder Implementation

    // Service Provider Implementation
}
```
