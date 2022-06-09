# Operation input, output, and unit types

* **Author**: Michael Dowling
* **Created**: 2021-11-11
* **Updated**: 2022-06-08

## Abstract

This proposal introduces new traits and shapes that makes operation inputs
and outputs more explicit resulting in a simplified semantic model, simplified
code generation, the ability for operations to opt-in to more flexible
backward compatibility semantics, and more expressiveness for tagged unions.
This is achieved through introducing the `@input` and `@output` traits to
specialize a structure as the input or output of a single operation, and
introducing a built-in [unit](https://en.wikipedia.org/wiki/Unit_type) shape
to explicitly indicate that an operation has no meaningful input or output and
that a tagged union member has no meaningful value.

## Motivation

### Best practices for defining operations are too easy to miss

Operations in Smithy prior to this proposal can have no input, no output,
or target any structure. A longstanding best-practice for Smithy models is
to always define a dedicated input and output structure for every operation,
even if an operation at its inception has no meaningful input or output.
This allows input members and output members to be added over time if ever
needed, which is important for services that plan to stay in production for
decades.

Reusing input and output shapes for multiple operations can hinder how a
service can evolve over time. For example, if operations diverge over time
but share the same input structure, the operations might have input members
that have to resort to documentation to caution users that a member is only
used when calling certain operations but not others.

### Operation backward compatibility semantics push too much complexity onto code generators

It is now considered backward compatible for an operation to change from no
input or output to defining input and output, and this has resulted in
pushing complexity onto Smithy code generators. Smithy code generators today
often generate dedicated "synthetic" input and output shapes for each
operation to account for this kind of model evolution. Generators
essentially make a copy of shapes used as input and output, and in the case
that a shape isn't defined for an operation, they generate one. This allows
the generated code to add context-specific functionality to shapes used as
operation input or output, like methods for customizing middleware, adding
HTTP headers, etc.

The reason for this backward compatibility affordance is largely because
service teams writing models are unaware that defining input and output shapes
for all operations is a best-practice. Its consequence is that each Smithy code
generation project needs to internalize all the nuance of synthetic input and
output shapes independently and figure out how to account for it in their
generators.

## Proposal

This proposal makes the following changes that impact both Smithy 1.0 and
Smithy 2.0:

1. The input and output of operations defaults to `smithy.api#Unit`, a shape
   in the Smithy prelude that represents the lack of a meaningful value.
2. A `Unit` shape will be added to the Smithy prelude. This shape is used when
   an operation truly has no input or output, and for tagged union members with
   no meaningful value.
3. `@input` and `@output` traits will be added that specialize a structure as
   the input or output of a single operation. Shapes marked with the `@input`
   and `@output` traits can only be referenced by a single operation, and
   cannot act as both input and output.
4. A WARNING will be emitted for operations that target shapes that are not
   marked with the `@input` and `@ouput` traits.
5. The `@input` and `@output` traits will automatically be applied to input and
   output shapes that are defined with the inline operation input and output
   syntax being introduced in Smithy IDL 2.0.

### `@input` and `@output` traits

`@input` and `@output` traits specialize a structure as allowed for use only
as top-level operation input and output. Shapes marked with these traits
cannot be targeted by members or used in any other way than to serve as the
input or output of a single operation.

The following example is a valid use of `@input` and `@output` traits:

```
operation GetFoo {
    input: GetFooInput,
    output: GetFooOutput
}

@input
structure GetFooInput {}

@output
structure GetFooOutput {}
```

The following structure is invalid because a member targets a shape marked
with the `@input` trait:

```
structure Hello {
    hi: GetFooInput // <- ERROR
}
```

The `@input` and `@output` traits make code generation easier because
generators can use the defined shape as-is without needing to generate a
synthetic shape. These traits also encourage modelers to define operations
that utilize known best practices, ensuring operations can easily evolve over
time.

#### @input and @output trait definitions

The `@input` and `@output` traits are defined in the prelude as:

```
/// Specializes a structure for use only as the input of a single operation.
@trait(selector: "structure", conflicts: [output, error])
@tags(["diff.error.const"])
structure input {}

/// Specializes a structure for use only as the output of a single operation.
@trait(selector: "structure", conflicts: [input, error])
@tags(["diff.error.const"])
structure output {}
```

#### Relaxed backward compatibility semantics

Structures marked with the `@input` trait will have more relaxed backward
compatibility semantics in that the `@required` trait can be freely removed
from their members. Other proposals are being explored in Smithy that allow
code generators to use traits like `@required` and `@default` to generate
more idiomatic code without sacrificing too much of a service's ability to
evolve their models over time. Code generators SHOULD take these relaxed
backward compatibility semantics into account when generating code for
structures marked with the `@input` trait to avoid breaking previously
generated clients as models evolve.

In Smithy IDL 2.0, the `@input` trait automatically applies the
`@clientOptional` trait to each member.

#### Automatic application using inline syntax

To encourage their use and remove the burden of needing to apply the `@input`
and `@output` traits to structures, the `@input` and `@output` traits will be
added automatically when using the inline operation input and output syntax
introduced in Smithy 2.0. For example, the following model,

```
$version: "2"

namespace smithy.example

operation GetFoo {
    input := {}
    output := {}
}
```

Will be equivalent to the following:

```
$version: "2"

namespace smithy.example

operation GetFoo {
    input: GetFooInput
    output: GetFooOutput
}

@input
structure GetFooInput {}

@output
structure GetFooOutput {}
```

#### Validation updates

The name of a shape targeted by the `@input` or `@output` trait SHOULD start
with the name of the operation that references it (if any). If not, then a
WARNING is emitted with an ID `OperationInputOutputName`.

For example, the following model would emit an `OperationInputOutputName`
WARNING because the input shape of the operation does not start with the name
of the operation:

```
operation GetFoo {
    input: GetFooInput,
    output: Foo
}

@input
structure GetFooInput {}

@output
structure Foo {} // <- this should be named GetFooOutput
```

To encourage models to utilize the `@input` and `@output` traits, the
`InputOutputStructureReuse` linter will be updated to ensure that every
operation defines a dedicated input and output shape marked with the
`@input` and `@output` traits.

### The `smithy.api#Unit` shape

A [unit](https://en.wikipedia.org/wiki/Unit_type) shape will be added to the
Smithy prelude to represent a shape that has no meaningful value. There will
be a single `Unit` shape in Smithy, modeled as a structure with the internal
`@unitType` trait attached to differentiate it from other structures. The
selector of the `@unitType` trait ensures that no other unit structures can be
defined.

The `Unit` shape and `unitType` trait are modeled in the prelude as:

```
namespace smithy.api

@unitType
structure Unit {}

/// Specializes a structure as a unit type that has no meaningful value.
/// This trait can only be applied to smithy.api#Unit, which ensures that
/// only a single Unit shape can be created.
@trait(selector: "[id=smithy.api#Unit]")
structure unitType {}
```

`smithy.api#Unit` can only be targeted as the input of an operation, output
of an operation, or from a member of a tagged union.

* Operations that do not define input or output will target `smithy.api#Unit`
  by default, and it is no longer considered a backward compatible change to
  change the shape targeted as the input or output of an operation.
* The unit type is also useful for unions. In some cases, the tag of a tagged
  union is the only meaningful value that needs to be communicated. In
  these cases, the union member can target the `Unit` type to indicate that the
  associated value has no meaning.

While `smithy.api#Unit` is modeled as a specialized structure, code generators
and protocols MAY special-case how a `Unit` is represented. For example, the
following Smithy model:

```
union ItemAction {
    delete: Unit,
    replaceWith: Item
}
```

Might be generated in Rust as:

```
enum Message {
    Delete,
    ReplaceWith(Item)
}
```

### Other considerations

#### Add a Smithy transform to generate synthetic input and output shapes

Because `@input` and `@output` traits are optional, some code generators that
want to define dedicated input and output shapes for every operation will still
want to generate synthetic input and output shapes. To make this easier,
an opt-in model transform will be added to smithy-build to reduce complexity in
Smithy code generators, and centralize the complexity in a single location.

#### AWS model migration from 1.0 to 2.0

Most models created for AWS use dedicated input and output shapes for every
operation. These input and output shapes will be updated to apply the `@input`
and `@output` traits. AWS models that do not define dedicated input and output
shapes (for example, Amazon API Gateway) will be updated to use generated
synthetic input and output shapes marked with the `@input` and `@output`
traits.

## FAQ

**Why do we not want to reuse input and output shapes?**

Every operation should have a dedicated input and output shape that only
functions as the input or output for one operation. The shape should not be
reused by other operations, should not function as both input and output, and
should not be used nested in other members.

1. Referencing the same input or output structure from multiple operations can
   lead to backward-compatibility problems in the future if the inputs or
   outputs of the operations ever need to diverge. By using the same structure,
   teams unnecessarily tie the interfaces and future evolution of operations
   together.
2. Using the same structure for both input and output can lead to
   backward-compatibility problems in the future if the members or traits used
   in input needs to diverge from those used in output. Reuse between operations
   is better facilitated by mixins, which allow members and traits to be shared,
   but also allows teams to later refactor or even remove the applied mixins.
3. Referencing an operation input or output shape as a member of another shape
   gives the type multiple use cases. This makes it harder for code generators
   to give special functionality to input and output types without affecting
   the way nested types are handled, leading code generators to generate
   synthetic types.

**How will we get service teams to use the `@input` and `@output` traits?**

* The inline operation input and output syntax being introduced in Smithy 2.0
  is so convenient it will likely be used by most new models, and it will
  automatically add these traits.
* Model validation will emit a WARNING when operations target shapes that do
  not have the `@input` or `@output` trait.

**What happens to operations that have no input or output?**

* Operations will by default target `smithy.api#Unit`.
* A WARNING will be emitted when an operation defaults to `smithy.api#Unit`
  without explicitly targeting `smithy.api#Unit`. This encourages modelers
  to be deliberate in their commitment to never needing input or output in
  the future for an operation.
* Existing AWS service models will all be updated to define input or output if
  they do not already.

**Will this be a breaking change in the reference Java implementation?**

No. The existing methods that return things like `Optional.empty` will continue
to function exactly as they did before, but will be marked as deprecated. New
methods will be added that always return a `ShapeId` or `StructureShape` when
accessing things like the input of an operation or querying `OperationIndex`.

**Won't it be annoying to have to model resource operations that reuse the same members?**

No. Models typically SHOULD NOT use the same input or output structures for
resources that perform CRUDL operations.

1. The data returned in a Get operation SHOULD NOT be coupled to the data
   returned in a List operation because:
   * Getting the details of a resource SHOULD require more elevated permissions
     than knowing a resource exists. If List and Get responses are coupled in
     the model, sensitive members that might need to be added later to the
     output of a Get response will automatically show up in the List response.
   * Getting all the details of a resource can be expensive, which impacts
     the ability to meet service level agreements around latency when listing
     many resources. If List and Get responses are coupled in the model, then
     every additional member added to the resource impacts the latency of List
     operations multiplied by the number of resources returned.
2. The data returned in a Get response SHOULD NOT be the same data sent in an
   update or create input because properties returned in a Get response are
   often generated by the server (like `createdAt`).
3. For any case where this _is_ valid overlap, Smithy IDL 2.0 introduces
   mixins, which allow for build-time copy and paste.

**Will this be awkward in AWS SDKs that don't know about unit types?**

No. Model transformations that convert Smithy models to older AWS modeling
formats will remove operation input and output references to `smithy.api#Unit`.
