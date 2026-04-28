# Typed Metadata

Metadata is a schema-less extensibility mechanism used to associate metadata to
an entire model. For example, metadata is used to define validators and
model-wide suppressions.

This document describes a way to define typing information for metadata that is
automatically validated by Smithy.

## Motivation

The schema-less nature of metadata has allowed it to be used for any purpose
without much hassle. However, that has come at the cost of increased validation
complexity. Any tool, including Smithy itself, that uses metadata in a
structured way has to perform validation itself. By allowing opt-in validation,
we can centralize and deduplicate that effort.

## Proposal

Model authors may globally declare the type of a metadata key by targeting a
shape with the `@metadata` trait.

```smithy
/// Defines a type for a metadata key.
///
/// If a matching key is defined in the model, its value will be validated
/// according to the targeted shape.
///
/// The type for any metadata key MUST only be defined once.
@trait(selector: "dataType :not([trait|input]) :not([trait|output])")
structure metadata {
    /// The metadata key to validate. Each key MUST only be defined once.
    @required
    @length(min: 1)
    key: String
}
```

For example, the
[suppressions metadata](https://smithy.io/2.0/spec/model-validation.html#suppressions-metadata)
could be defined as:

```smithy
$version: "2.0"

namespace smithy.api

@metadata(key: "suppressions")
list MetadataSuppressions {
    member: MetadataSuppression
}

structure MetadataSuppression {
    @required
    id: String

    @required
    @pattern("^(\*|[_a-zA-Z]\w*(\.[_a-zA-Z]\w*)*)$")
    namespace: String

    reason: String
}
```

### Validation

A metadata key MUST NOT be defined more than once with the `@metadata` trait. If
a metadata key is defined more than once with the `@metadata` trait, an
`ERROR`-level validation event will be emitted.

Validation of metadata values will behave no differently than validation
anywhere else, with severity levels being determined by each individual
validator.

Shapes targeted by the metadata shape and shapes transitively referenced by
those shapes will not trigger the unreferenced shapes validator.

Adding or removing this trait is considered backwards-compatible because
metadata does not inherently change any interface. It may cause a build to break
if the new validation rejects existing metadata, but that is intended behavior.

## Alternatives

### Inline definitions

The metadata trait globally defines the shape of a metadata key, but we could
allow local, inline definitions additionally or instead. This can be achieved by
using a special `$type` key in metadata's node value.

```smithy
$version: "2.0"

metadata foo = {
    "$type": "com.example#Foo"
    "bar": "baz"
}

namespace com.example

structure Foo {
    bar: String
}
```

The problem with this strategy is that it can only be applied to structure and
union shapes, because other shape types have nowhere to add the `$type` key or
it would potentially shadow a valid value key.

Other shapes could instead be defined via a `__type__` metadata key:

```smithy
$version: "2.0"

metadata "__type__": [
    ["foo", "com.example#Foo"]
]

metadata foo = "bar"

namespace com.example

string Foo

@metadata(key: "__type__")
list MetdataTypeDeclarations {
    member: MetadataTypeDeclaration
}

@length(min: 2, max: 2)
list MetadataTypeDeclaration {
    member: String
}
```

Unfortunately, this is inherently a global declaration that is not inline with
the value, defeating the intent of an inline definition. The metadata trait is
better suited to this purpose, as it is clearer, easier to validate, more easily
discoverable, and more easily trackable.

Smithy 2.1 could introduce new syntax to declare inline metadata types that isn't
tied to the value, but the AST could not support it without either exposing the
same problem or making a breaking change.

## FAQ

### Does this interact with metadata merging semantics?

When a given metadata key is defined more than once, it usually results in a
conflict that fails the build. If both definitions are arrays, however, they are
instead merged. If a metadata key is defined as a list, the merged array will be
validated.

This trait does not change merging semantics.

### Will the existing defined metadata keys get metadata-trait-based definitions?

Smithy currently has definitions for three metadata keys: `severityOverrides`,
`suppressions`, and `validators`. All three could be defined with the metadata
trait.

However, these three keys are currently parsed and validated early on when
loading a model, before validation generally is run. They will need to be
special-cased to either run early or to be skipped in favor of existing
validation.

These keys will initially be reserved so they can't be defined. As the keys get
full definitions, their reserve list entries will be removed.

### What happens if a defined metadata key is not used?

Nothing. Metadata keys are inherently optional. A `required` member may be added
later to enforce presence if there is a need.
