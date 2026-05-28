# Inline collection declarations

* **Author**: Manuel Sugawara
* **Created**: 2026-05-14

## Abstract

This proposal introduces syntactic sugar for declaring list and map
shapes inline within member definitions. Instead of requiring a
separate top-level shape definition, members can declare their
collection type directly using `[Target]` for lists and `{Key: Value}`
for maps. The assembler generates synthetic shapes with stable,
content-derived names that are grouped by structural equivalence.

## Motivation

Lists and maps are the most common shapes that exist solely to be
referenced by a single member. Unlike structures, they rarely carry
meaningful names, e.g., `ListOfStrings`, `StringToStringMap`, etc. are
boilerplate that adds noise without conveying intent. Requiring a
separate top-level definition forces authors to context-switch and
invent names for shapes that have no semantic identity beyond their
structure.

Consider a typical model today:

```smithy
list StringList {
    member: String
}

map TagMap {
    key: String
    value: String
}

map AccountIndex {
    key: AccountId
    value: Account
}

structure MyStructure {
    names: StringList
    tags: TagMap
    accounts: AccountIndex
}
```

With inline collections, this becomes:

```smithy
structure MyStructure {
    names: [String]
    tags: {String: String}
    accounts: {AccountId: Account}
}
```

The inline form is more readable, reduces boilerplate, and keeps the
collection definition co-located with its usage.

## Proposal

### Syntax

Two new forms are allowed in member target positions:

- **Lists**: `[Target]`, declares a list whose member targets `Target`.
- **Maps**: `{KeyTarget: ValueTarget}`, declares a map whose key targets
  `KeyTarget` and whose value targets `ValueTarget`.

```smithy
structure Foo {
    strings: [String]
    tags: {String: String}
    nested: {PersonId: [Account]}
}
```

These forms are only permitted in member target positions. They cannot
appear as top-level shape definitions.

#### Nesting

Inline collections can be nested. The inner collection produces its
own synthetic shape, and the outer collection references it:

```smithy
structure Foo {
    // A map from PersonId to a list of Account
    accounts: {PersonId: [Account]}
}
```

This desugars into two synthetic shapes: one list and one map.

#### ABNF

The member target production is extended:

```
member_target = shape_id / inline_list / inline_map

inline_list = "[" ws member_target ws "]"

inline_map = "{" ws member_target ws ":" ws member_target ws "}"
```

### Synthetic shape naming

A predictable, content-derived naming convention enables the assembler
to recognize when two inline declarations refer to the same
collection type and reuse a single shape for both. This keeps the
model compact (one shape per unique collection type rather than one
per member) and ensures that the generated shape ID is stable across
refactorings: reordering members, moving them between structures, or
adding new usages of the same type does not change existing shape IDs.
The `_Synthetic` prefix makes these shapes easy to identify in
tooling and ensures they cannot collide with user-defined names.

Inline collections produce synthetic shapes with content-derived
names. The naming scheme uses a `_Synthetic` prefix followed by a
description of the collection type and its targets.

The format is:

```
_Synthetic<ShapeType>Of<TargetNames>
```

Examples:

| Inline syntax | Synthetic shape name |
|---------------|---------------------|
| `[String]` | `_SyntheticListOfString` |
| `[Account]` | `_SyntheticListOfAccount` |
| `{String: String}` | `_SyntheticMapOfStringToString` |
| `{AccountId: Account}` | `_SyntheticMapOfAccountIdToAccount` |
| `{PersonId: [Account]}` | `_SyntheticMapOfPersonIdTo_SyntheticListOfAccount` |

These names are stable: they are derived from content, not declaration
order, so reordering members or adding new inline collections does not
change existing names. They are also deterministic: the same inline
declaration always produces the same name.

Note: It is theoretically possible for a user-defined shape to
conflict with a synthetic name (e.g., a user defines a shape named
`_SyntheticListOfString`). In practice this is unlikely, and the
assembler SHOULD emit a validation error if a conflict is detected,
instructing the user to rename their shape.

The synthetic shapes are placed in the same namespace as the structure
that contains them.

### Grouping

Grouping is scoped to a single namespace. Within a namespace, all
members using the same inline collection type (e.g., `[String]`)
reference the same synthetic shape. Across namespaces, each namespace
produces its own synthetic shape independently:
`com.foo#_SyntheticListOfString` and `com.bar#_SyntheticListOfString`
are distinct shapes.

Explicit user-defined shapes are never grouped with synthetic shapes.
A `list MyList { member: String }` is a distinct shape from
`_SyntheticListOfString` even though they are structurally equivalent.

### The `@generated` trait

Synthetic shapes carry a `@generated` trait in the semantic model and
AST serialization. This trait identifies the shape as
assembler-generated and allows IDL serializers to reconstruct the
inline syntax when converting from AST back to IDL. It is not
user-applicable: only the assembler may attach it.

### Traits on synthetic shapes

Because synthetic shapes are shared across all members that use the
same inline type, applying a trait to a synthetic shape would affect
every usage simultaneously. This creates action at a distance: adding
`@sparse` to `_SyntheticListOfString` would make every `[String]`
member in the namespace sparse, which is almost never the intent. To
prevent this, traits cannot be applied to synthetic shapes.

No traits may be applied to synthetic shapes, either at definition
time or via `apply` statements. The `apply` statement MUST NOT target
a shape that has the `@generated` trait.

Traits that would normally apply to a collection shape (such as
`@sparse` or `@uniqueItems`) cannot be expressed using inline syntax.
Authors requiring these traits on the collection shape itself MUST use
explicit shape definitions.

Note that some traits like `@length` can also be applied to the
containing member. When applied to a member, the trait constrains only
that specific member usage, not the shared synthetic shape. For
example, `@length(min: 1) names: [String]` constrains the `names`
member but does not affect other members that reference the same
`_SyntheticListOfString` shape.

### Referencing synthetic shapes

Synthetic shape names exist as valid shape IDs in the semantic model
and AST. However, since traits cannot be applied to synthetic shapes
and their members cannot be independently referenced, there is no
practical use case for referencing them directly in `apply` statements
(IDL or AST) or member target positions.

Synthetic shapes can be referenced in:

- Selectors (e.g., `[id = ns#_SyntheticListOfString]`)
- Programmatic model access (Java API, JSON AST)

### Selectors

Synthetic shapes are normal shapes in the semantic model. They appear
in selector results and can be matched by type:

```
list                           // matches all lists, including synthetic
[trait|generated]              // matches only generated synthetic shapes
```

### AST serialization

In the JSON AST, synthetic shapes are serialized as normal shapes with
the `@generated` trait attached:

```json
{
    "smithy": "2.1",
    "shapes": {
        "com.example#_SyntheticListOfString": {
            "type": "list",
            "member": {
                "target": "smithy.api#String"
            },
            "traits": {
                "smithy.synthetic#generated": {}
            }
        },
        "com.example#MyStructure": {
            "type": "structure",
            "members": {
                "strings": {
                    "target": "com.example#_SyntheticListOfString"
                }
            }
        }
    }
}
```

### IDL round-tripping

When converting from JSON AST to IDL 2.1, a serializer SHOULD
reconstruct inline syntax for shapes bearing the `@generated`
trait. When converting to IDL 2.0 or earlier, synthetic shapes MUST be
serialized as explicit top-level shape definitions.

## Alternatives considered

### Member-derived naming (`Foo~listMember`)

Names derived from the containing structure and member (e.g.,
`Foo~listMember` for a member named `listMember` in structure `Foo`)
using a character not valid in Smithy identifiers as separator.

This was rejected because:

- It prevents grouping. Each usage produces a unique shape even when
  structurally identical.
- Renaming a member changes the synthetic shape ID, which is a
  breaking change for anything referencing it.
- It creates many more shapes in the model than necessary.

### Illegal character separator (`Synthetic~ListOfString`)

Using a character not valid in Smithy identifiers (e.g., `~`) as part
of the synthetic name to guarantee no conflicts with user-defined
shapes.

This was rejected because:

- It requires extending the shape ID grammar to accommodate a new
  character.
- It adds complexity to parsers and tooling for a marginal benefit.
- The probability of a user-defined shape colliding with a
  `_Synthetic`-prefixed content-derived name is negligible in
  practice.

### Counter-based naming (`synthetic_1`, `synthetic_2`)

Sequential numbering of synthetic shapes in declaration order.

This was rejected because:

- Names are unstable, reordering declarations or adding new members
  renumbers existing shapes.
- Non-deterministic across serializations.
- Produces meaningless names in error messages and tooling.
- Breaks diffing, caching, and any tooling that indexes by shape ID.

### Allowing traits on synthetic shapes

Permitting traits like `@sparse`, `@uniqueItems`, or `@length` on
synthetic shapes, either via inline syntax or `apply`.

This was rejected because:

- It complicates grouping, traits on the shape would need to be part
  of the equivalence check.
- Applying traits to a shared/grouped shape affects all usages, which
  is rarely the intent.
- It introduces the new concept of "traits declared on a member that
  semantically apply to the target shape."
- The explicit shape definition path already handles these cases
  cleanly.
- Many common traits applied to lists and maps (such as `@length`,
  `@deprecated`, and `@documentation`) can also be applied to the
  containing member, reducing the need for shape-level trait
  application in practice.

### Square bracket syntax for maps (`[Key, Value]`)

Using `[String, String]` for maps.

This was rejected because:

- Ambiguous with list syntax, `[String]` is a list,
  `[String, String]` would be a map, creating a subtle and error-prone
  distinction.
- Does not convey key/value semantics; relies on positional
  convention.
- Forecloses future tuple or multi-type-parameter syntax.

### Curly braces with comma (`{Key, Value}`)

Using `{String, String}` for maps.

This was rejected because:

- Curly braces already denote structure/block bodies in Smithy,
  creating visual ambiguity.
- Does not convey key/value relationship.
- Resembles set literal syntax in many languages.

## Limitations

- Inline collections cannot carry shape-level traits. Authors needing
  `@sparse`, `@uniqueItems`, `@length` (on the collection), or other
  shape-level traits must use explicit shape definitions.
- Synthetic shapes cannot be targeted by `apply` statements.
- The inline syntax is only available in member target positions, not
  as top-level definitions.

## Migration

### Adopting inline syntax

Existing models using explicit list and map definitions can adopt
inline syntax incrementally. Replacing an explicit shape with inline
syntax changes the shape ID (from `ns#MyList` to
`ns#_SyntheticListOfString`), which is a breaking change for:

- Other models referencing the shape by name
- Selectors targeting the shape by ID
- Code generators that derive type names from shape names

Authors should only migrate shapes that are not referenced externally.

### Outgrowing inline syntax

When a collection needs traits that cannot be expressed inline, the
migration path is:

1. Define an explicit named shape with the required traits.
2. Update the member to reference the explicit shape.

Since synthetic shapes cannot be referenced cross-model, this
migration is always local to the model that defines the inline
collection.

## FAQ

### Can inline collections be used in union members?

Yes. Inline collections can appear in any member target position,
including union members, structure members, and list/map member
targets (for nesting).

### Do synthetic shapes appear in code generation?

Synthetic shapes are normal shapes in the model. Code generators will
encounter them. However, most languages use idiomatic collection types
(e.g., `List<String>`, `Map<String, String>`) for lists and maps
regardless of the shape name, so the synthetic name is typically not
visible in generated code.

Code generators that do produce named types for collections can use
the synthetic name directly or transform it as needed for the target
language.

### What if two namespaces produce the same synthetic name?

Synthetic shapes are scoped to their
namespace. `com.foo#_SyntheticListOfString` and
`com.bar#_SyntheticListOfString` are distinct shapes, just as any two
shapes in different namespaces are distinct.

### Can a member target both an explicit shape and an inline collection?

A member targets exactly one shape. It either references an explicit
shape by name or uses inline syntax (which references the synthetic
shape). There is no way to "merge" the two.

### How does this interact with mixins?

If a mixin defines a member with an inline collection, the synthetic
shape is created in the mixin's namespace. Structures that apply the
mixin inherit the member, which continues to reference the same
synthetic shape.
