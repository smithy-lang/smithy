# Smithy Trait CodeGen

Smithy build plugin that generates Java trait classes from customized Smithy trait defined with [`@trait`](https://smithy.io/2.0/spec/model.html#trait-trait).

## Supported Trait Types
- [**Simple Types**](https://smithy.io/2.0/spec/simple-types.html) (Excluding `Blob` and `Boolean`)
- **Structure**
- **List**
- **Set**
- **Map**

> [!NOTE]
> [Annotation Trait](https://smithy.io/2.0/spec/model.html#annotation-traits) is recommended rather than Boolean trait

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
| `header` | Header lines to include in generated files | No |
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

First, create a gradle-based Smithy model project. You can also use Smithy CLI to generate a [complete custom trait example](https://github.com/smithy-lang/smithy-examples/tree/main/custom-trait-examples/custom-trait): `smithy init -o custom-trait -t custom-trait`.

> [!NOTE]
> The generator currently cannot be run with non-gradle-based projects.

In `build.gradle.kts`, add `id("software.amazon.smithy.gradle.smithy-trait-package").version("1.3.0")` to the `plugin` section.
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

Next, add the `trait-codegen` plugin to the
[plugin configuration](https://smithy.io/2.0/guides/smithy-build-json.html) in `smithy-build.json`.

```json
"plugins": {
    "trait-codegen": {
        "package": "io.examples.traits",
        "namespace": "example.traits",
    }
}
```

Finally, build the model with Gradle: `gradle build` and you can find the the generated Java classes under `/build/smithyprojections/trait-codegen/`

The generated Java class for `annotationTrait` from `custom-trait` template would be:
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
The generated Java class for `JsonNameTrait` from `custom-trait` template would be:
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

## License

Licensed under the Apache 2.0 License.
