# Inline Operation Inputs / Outputs

This document describes a way to write input and output shapes as an inline
part of an operation’s definition.

## Motivation

Operation input and output shapes are always structures, almost always have
boilerplate names, and critically are almost never re-used. In some cases, they
may not even have types generated for them. Because of those properties, the
need to fully define them separately from an operation can feel like needless
boilerplate. Additionally, separating them makes reading an operation at a high
level more difficult since you have to jump around to get the information you
need.

## Proposal

Operations will allow inlining input and output definitions, indicated by a
walrus operator (`:=`). Inlined structures will deviate from normal
structure definitions in two respects. Firstly, the structure keyword will be
omitted. Additionally, the names of the structures will be generated.

### Walrus Operator

Rather than using the same standalone colon (`:`) that is used in other
cases, a walrus operator will be used to indicate an inline definition. The
reason for this is to visually distinguish it at the outset, as well as to make
parsers simpler because the walrus operator removes the need for arbitrary
lookahead.

This usage of the operator is analogous to how some programming languages use
it. For instance, Python uses it to assign a name to the result of an
expression in places where initializing a variable was previously forbidden.
In Go, it’s used to initialize and assign a variable in one step.

### Omitting the structure Keyword

When used at the top level of a Smithy IDL file, the type keywords are
necessary to indicate what type of shape you’re making. Since operation
inputs and outputs may only be structures, this isn’t necessary.

### Generated Names

Inlined structures will have names generated for them if they aren’t
provided. For inputs, the generated name will be the name of the operation with
an `Input` suffix. For outputs, the default name will be the name of the
operation with an `Output` suffix.

The reason for generating a name is that there’s rarely a better name
than what is trivially generated. Therefore, requiring users to write it out
is effectively pointless boilerplate. In AWS models, for instance, over 98% of
operation input/output structures are named by suffixing the operation name.

#### Custom Suffixes

A service team that wants to migrate to using inlined structures may have
already been using a different set of suffixes, such as `Request` and
`Response`. To remain consistent, they can use control statements to customize
their suffixes on a per-file basis.

`operationInputSuffix` controls the suffix for the input, and
`operationOutputSuffix` controls the suffix for the output.

Service teams that use these customizations SHOULD write linters to ensure that
all of the operations in a given service conform to their expected naming
convention.

### Examples

```
operation GetUser {
    input := {
        userId: String
    }

    output := {
        username: String
        userId: String
    }
}

// Inlined inputs/outputs with traits. Only 20% of current operation
// inputs/outputs in AWS services use any traits, which mostly consists of
// documentation.
operation GetUser {
    input :=
        /// Documentation is currently the most popular trait on IO shapes. That
        /// said, there isn't much point to adding docs to an IO shape since the
        /// operation docs will take over that role.
        @sensitive
        @references([{resource: User}]) {
            userId: String
        }

    // If there's only one trait and it's short, this compact form can be used.
    // The references trait is the most likely trait to be used in the future,
    // and in most cases it will be able to use this compact form.
    output := @references([{resource: User}]) {
        username: String
        userId: String
    }
}

// Inlined inputs/outputs with mixins.
operation GetUser {
    input := with BaseUser {}

    output := with BaseUser {
        username: String
    }
}
```

### ABNF

```
operation_statement =
    "operation" ws identifier ws inlineable_properties

inlineable_properties =
    "{" *(inlineable_property ws) ws "}"

inlineable_property =
    node_object_kvp / inline_structure

inline_structure =
    node_object_key ws ":=" ws inline_structure_value

inline_structure_value =
    trait_statements [mixins ws] shape_members
```

The following demonstrate customizing the suffixes.

```
$version: "2.0"
$operationInputSuffix: "Request"
$operationOutputSuffix: "Response"

namespace com.example

operation MyOperation {
    // Generated name is: MyOperationRequest
    input := {}

    // Generated name is: MyOperationResponse
    output := {}
}
```

## FAQ

### Can apply be used on inlined inputs/outputs?

Yes. This is only syntactic sugar, the shapes produced are normal shapes in
every way.

### Can inlined shapes be used anywhere else?

No. There aren't many other places where inline shapes would make sense. Errors,
for instance, can’t use generated names and are frequently referenced elsewhere.

Consider the following simplified model that uses theoretical inlined, nested
structure definition:

```
structure Foo {
    bar := {
        id: String
    }
}
```

On day one, the `bar` structure is only referenced in one place, so perhaps
there’s a desire to inline it. On day two, another structure is introduced that
references it.

```
structure Foo {
    bar := {
        id: String
    }
}

structure Baz {
    bar: bar
}
```

Now the fact that it's inlined has become a detriment, because it's hard to go
from looking at the definition of `Baz` to finding the definition of `bar`. This
problem gets worse and worse the larger the model gets and the more the nested
structure is referenced. This isn't so much a problem for operations, because
their IO shapes are almost never refrenced elsewhere and even if they were the
default name makes it pretty clear where to look.

There is at least one other place where this may make sense: resource
identifiers. When defining a resource, you could use this syntax to define a
structure that contains only the resource identifiers. This could be mixed in
to other shapes in the model. This usage, while interesting, is out of scope
for this document.

### Why can't explicit names be optionally provided?

Allowing optional names would complicate the parser by requiring additional
lookahead to disambiguate between a structure named with and the use of a
with statement. With the ability to customize the generated suffix, the
only reason to provide an overridden name is in the rare case where the
structure isn't already using the operation name as a prefix. Since fewer
than 2% of all AWS services deviate from this pattern, it's an acceptable
tradeoff to require those cases to separately define their shapes.
