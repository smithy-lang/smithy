# Smithy Mixins

This proposal defines _mixins_, a modeling mechanism that allows for shape
definition reuse.

> Note: mixins will be considered for Smithy IDL 1.1, which treats commas as
> optional whitespace.


## Motivation

When authoring Smithy models, the same members often need to be duplicated
across multiple related structures. For example, the identifiers of a resource
need to be present in each input structure of resource operations:

```
namespace smithy.example

resource City {
    identifiers: { cityId: CityId }
    read: GetCity
    delete: DeleteCity
    update: UpdateCity
}

structure GetCityInput {
    @required
    @httpLabel
    cityId: CityId
}

structure DeleteCityInput {
    @required
    @httpLabel
    cityId: CityId
}

structure UpdateCityInput {
    @required
    @httpLabel
    cityId: CityId
}
```

Repetition is also required to define paginated operations in a service.

```
structure ListCitiesInput {
    nextToken: String
    pageSize: Integer
}

structure ListCitiesOutput {
    nextToken: String

    @required
    items: CitySummaries
}

structure ListWeatherStationsInput {
    nextToken: String
    pageSize: Integer
}

structure ListWeatherStationsOutput {
    nextToken: String

    @required
    items: WeatherStationSummaries,
}
```

This repetition is tedious and error-prone as it can lead to unintentional
inconsistencies due to copy/paste errors and drift between related shapes
over time.


## Proposal

This proposal introduces *mixins* to reduce the amount of repetition in
shapes, reduce copy/paste errors, and provide the ability to define reusable
partial shapes.


### Goals

1. Improve maintainability of large models by reducing repetition and
   copy/paste errors of related concepts.
2. Mixins are an implementation detail of the model that have no impact on
   generated code or collaborators that need to apply traits to shapes that use
   mixins.
3. Allow refinement of mixin traits so that a mixin can provide reasonable
   default traits, and shapes that use mixins can redefine them with more
   specific traits.
4. Reduce the surface area for bugs by treating mixin members that are copied
   onto structures just like normal members when using the Smithy meta model.


### Overview

A mixin is a shape marked with the `@mixin` trait(`smithy.api#mixin`):

```
$version: "1.1"

namespace smithy.example

@mixin
structure CityResourceInput {
    @httpLabel
    @required
    cityId: String
}
```

Adding a mixin to a shape causes the members and traits of the other
shape to be copied into the shape. Mixins can be added to a shape using
`with` followed by any number of shape IDs. Each shape ID MUST target a
shape marked with the `@mixin` trait. Shapes can only use mixins that
are of the same shape type.

```
structure GetCityInput with [CityResourceInput] {
    foo: String
}
```

Multiple mixins can be applied:

```
structure GetAnotherCityInput with [
    CityResourceInput
    SomeOtherMixin
] {
    foo: String
}
```

Mixins can be composed of other mixins:

```
@mixin
structure MixinA {
    a: String
}

@mixin
structure MixinB with [MixinA] {
    b: String
}

structure C with [MixinB] {
    c: String
}
```

When a member is copied from a mixin into a target shape, the shape ID of the
copied member takes on the containing shape ID of the target shape. This
ensures that members defined via mixins are treated the same way as members
defined directly in a shape, and it allows members of a shape to be backward
compatibly refactored and moved into a mixin or for a shape to remove a mixin
and replace it with members defined directly in the shape.

The above `C` structure is equivalent to the following flattened structure
without mixins:

```
structure C {
    a: String
    b: String
    c: String
}
```

Other shape types can be mixins and use mixins too.

```
@mixin
union UserActions {
    subscribe: SubscribeAction,
    unsubscribe: UnsubscribeAction,
}

union AdminActions with [UserAction] {
    banUser: BanUserAction,
    promoteToAdmin: PromoteToAdminAction
}
```


### Traits and mixins

Shapes that use mixins inherit the traits applied to their mixins, except for
the `@mixin` trait and *mixin local traits*. Traits applied directly to a
shape take precedence over traits applied to its mixins.

For example, the definition of `UserSummary` in the following model:

```
/// Generic mixin documentation.
@tags(["a"])
@mixin
structure UserInfo {
    userId: String
}

structure UserSummary with [UserInfo] {}
```

Is equivalent to the following flattened structure because it inherits the
traits of `UserInfo`:

```
/// Generic mixin documentation.
@tags(["a"])
structure UserSummary {
    userId: String
}
```

The definition of `UserSummary` in the following model:

```
/// Generic mixin documentation.
@tags(["a"])
@mixin
structure UserInfo {
    userId: String
}

/// Specific documentation
@tags(["replaced-tags"])
structure UserSummary with [UserInfo] {}
```

Is equivalent to the following flattened structure because it inherits the
traits of `UserInfo` and traits applied to `UserSummary` take precedence over
traits it inherits:

```
/// Specific documentation
@tags(["replaced-tags"])
structure UserSummary {
    userId: String
}
```

The order in which mixins are applied to a shape controls the inheritance
precedence of traits. For each mixin applied to a shape, traits applied
directly to the mixin override traits applied to any of its mixins. Traits
applied to mixins that come later in the list of mixins applied to a shape take
precedence over traits applied to mixins that come earlier in the list of
mixins. For example, the definition of `StructD` in the following model:

```
/// A
@foo(1)
@oneTrait
@mixin
structure StructA {}

/// B
@foo(2)
@twoTrait
@mixin
structure StructB {}

/// C
@threeTrait
@mixin
structure StructC with [StructA, StructB] {}

/// D
@fourTrait
structure StructD with [StructC] {}
```

Is equivalent to the following flattened structure:

```
// (1)
/// D
@fourTrait    // (2)
@threeTrait   // (3)
@foo(2)       // (4)
@twoTrait     // (5)
@oneTrait     // (6)
structure StructD {}
```

1. The `documentation` trait applied to `StructD` takes precedence over any
   inherited traits.
2. `fourTrait` is applied directly to `StructD`.
3. `threeTrait` is applied to `StructC`, `StructC` is a mixin of `StructD`,
    and `StructD` inherits the resolved traits of each applied mixin.
4. Because the `StructB` mixin applied to `StructC` comes after the `StructA`
   mixin in the list of mixins applied to `StructC`, `foo(2)` takes precedence
   over `foo(1)`.
5. `StructC` inherits the resolved traits of `StructB`
6. `StructC` inherits the resolved traits of `StructA`.


#### Mixin local traits

Sometimes it's necessary to apply traits to a mixin that are not copied onto
shapes that use the mixin. For example, if a mixin is an implementation detail
of a model, then it is recommended to apply the `@private` trait to the mixin
so that shapes outside of the namespace the mixin is defined within cannot
refer to the mixin. However, every shape that uses the mixin doesn't
necessarily need to be marked as `@private`. The `localTraits` property of
the `@mixin` trait can be used to ensure that a list of traits applied to
the mixin are not copied onto shapes that use the mixin (note that this has
no effect on the traits applied to members contained within a mixin).

Consider the following model:

```
namespace smithy.example

@private
@mixin(localTraits: [private])
structure PrivateMixin {
    foo: String
}

structure PublicShape with [PrivateMixin] {}
```

`PublicShape` is equivalent to the following flattened structure:

```
structure PublicShape {
    foo: String
}
```

The `PrivateMixin` shape can only be referenced from the `smithy.example`
namespace. Because `private` is present in the `localTraits` property of the
`@mixin` trait, `PublicShape` is not marked with the `@private` trait and
can be referred to outside of `smithy.example`.

> See *Trait definitions* for a full description of `localTraits`.


### Adding and replacing traits on copied members

The members and traits applied to members of a mixin are copied onto the target
shape. It is sometimes necessary to provide a more specific trait value for a
copied member or to add traits only to a specific copy of a member. Traits can
be added on to these members like any other member. Additionally, Traits can be
applied to these members in the JSON AST using the `apply` type and in the
Smithy IDL using `apply` statements.

> Note: Traits applied to shapes supersede any traits inherited from mixins.


#### Applying traits in the JSON AST

Traits can be applied to copies of mixin members using the `apply` type in
the JSON AST. The `apply` type allows traits to be added to a shape defined
elsewhere without defining the actual shape.

For example, the following model defines a mixin named `MyMixin` that is
applied to `MyStruct`. The `mixinMember` member of `MyMixin` has a
`documentation` trait of "Generic docs", but the copied `mixinMember` member
 on `MyStruct` replaces the `documentation` trait with "Specific docs".

```json
{
    "smithy": "1.1",
    "shapes": {
        "smithy.example#MyMixin": {
            "type": "structure",
            "members": {
                "mixinMember": {
                    "target": "smithy.api#String",
                    "traits": {
                        "smithy.api#documentation": "Generic docs"
                    }
                }
            },
            "traits": {
                "smithy.api#mixin": {}
            }
        },
        "smithy.example#MyStruct": {
            "type": "structure",
            "mixins": [
                {"target": "smithy.example#MyMixin"}
            ]
        },
        "smithy.example#MyStruct$mixinMember": {
            "type": "apply",
            "traits": {
                "smithy.api#documentation": "Specific docs"
            }
        }
    }
}
```

Traits can also be applied to copies of mixin members as if they were local
members.

```json
{
    "smithy": "1.1",
    "shapes": {
        "smithy.example#MyMixin": {
            "type": "structure",
            "members": {
                "mixinMember": {
                    "target": "smithy.api#String",
                    "traits": {
                        "smithy.api#documentation": "Generic docs"
                    }
                }
            },
            "traits": {
                "smithy.api#mixin": {}
            }
        },
        "smithy.example#MyStruct": {
            "type": "structure",
            "members": {
               "mixinMember": {
                  "target": "smithy.api#String",
                  "traits": {
                     "smithy.api#documentation": "Specific docs"
                  }
               }
           },
            "mixins": [
                {"target": "smithy.example#MyMixin"}
            ]
        }
    }
}
```

#### Applying traits in the IDL

The previous example can be defined in the Smithy IDL using an
`apply_statement` defined by the following ABNF:

```abnf
apply_statement = "apply" ws shape_id ws trait br
```

For example:

```smithy
$version: "1.1"
namespace smithy.example

@mixin
structure MyMixin {
    /// Generic docs
    mixinMember: String
}

structure MyStruct with [MyMixin] {}
apply MyStruct$mixinMember @documentation("Specific docs")
```

Alternatively, the member can be redefined if it targets the same shape:

```smithy
$version: "1.1"
namespace smithy.example

@mixin
structure MyMixin {
    /// Generic docs
    mixinMember: String
}

structure MyStruct with [MyMixin] {
    /// Specific docs
    mixinMember: String
}
```


### Mixins are not code generated

Mixins are an implementation detail of models and are only intended to reduce
duplication in Smithy structure and union definitions. Shapes marked with the
`@mixin` trait MUST NOT be code generated. Mixins _do not_ provide any kind of
runtime polymorphism for types generated from Smithy models. Generating code
from mixins removes the ability for modelers to introduce and even remove
mixins over time as a model is refactored.


### Mixins cannot be referenced other than as mixins to other shapes

To ensure that mixins are not code generated, mixins MUST NOT be referenced
from any other shapes except to mix them into structures and unions. Mixins
MUST NOT be used as operation input, output, or errors, and they MUST NOT be
targeted by members.

The following model is invalid because a structure member targets a mixin:

```
@mixin
structure GreetingMixin {
    greeting: String
}

structure InvalidStructure {
    notValid: GreetingMixin // <- this is invalid
}
```

The following model is invalid because an operation attempts to use a mixin
as input:

```
@mixin
structure InputMixin {}

operation InvalidOperation {
    input: InputMixin // <- this is invalid
}
```

### Mixins MUST NOT introduce cycles

Mixins MUST NOT introduce circular references. The following model is invalid:

```
@mixin
structure CycleA with [CycleB] {}

@mixin
structure CycleB with [CycleA] {}
```


### Mixin members MUST NOT conflict

The list of mixins applied to a structure or union MUST NOT attempt to define
members that use the same member name with different targets. The following model
is invalid:

```
@mixin
structure A1 {
    a: String
}

@mixin
structure A2 {
    a: Integer
}

structure Invalid with [A1, A2] {}
```

The following model is also invalid, but not specifically because of mixins.
This model is invalid because the member name `a` and `A` case insensitively
conflict.

```
@mixin
structure A1 {
    a: String
}

@mixin
structure A2 {
    A: Integer
}

structure Invalid with [A1, A2] {}
```

Members that are mixed into shapes MAY be redefined if and only if each
redefined member targets the same shape. Traits applied to redefined members
supersede any traits inherited from mixins.

```
@mixin
structure A1 {
    @private
    a: String
}

@mixin
structure A2 {
    @required
    a: String
}

structure Valid with [A1, A2] {}
```


### Mixins in the IDL

To support mixins, shape ABNF rules
will be updated to contain an optional `mixins` production
that comes after the shape name. Each shape ID referenced in
the `mixins` production MUST target a shape of the same type as the
shape being defined and MUST be marked with the `@mixin` trait.

```
simple_shape_statement = simple_type_name ws identifier [mixins]
list_statement = "list" ws identifier [mixins] ws shape_members
set_statement = "set" ws identifier [mixins] ws shape_members
map_statement = "map" ws identifier [mixins] ws shape_members
structure_statement = "structure" ws identifier [mixins] ws structure_members
union_statement = "union" ws identifier [mixins] ws union_members
service_statement = "service" ws identifier [mixins] ws node_object
operation_statement = "operation" ws identifier [mixins] ws node_object
resource_statement = "resource" ws identifier [mixins] ws node_object
mixins = sp "with" ws "[" 1*(ws shape_id) ws "]"
```


### Mixins in the JSON AST

Mixins are defined in the JSON AST using the `mixins` property of structure and
union shapes. The `mixins` property is a list of objects. To match every other
shape target used in the AST, the object supports a single member named
`target` which defines the absolute shape ID of a shape marked
with the `smithy.api#mixin` trait.

```json
{
    "smithy": "1.1",
    "shapes": {
        "smithy.example#GetCityInput": {
            "type": "structure",
            "mixins": [
                {
                    "target": "smithy.example#CityResourceInput"
                }
            ],
            "members": {}
        },
        "smithy.example#CityResourceInput": {
            "type": "structure",
            "traits": {
                "smithy.api#mixin": {}
            },
            "members": {
                "cityId": {
                    "target": "smithy.api#String",
                    "traits": {
                        "smithy.api#httpLabel": {},
                        "smithy.api#required": {}
                    }
                }
            }
        }
    }
}
```


### Mixins in selectors

A new relationship is introduced to selectors called `mixin` that traverses from
shapes to every mixin applied to them. The members of each
applied mixin are connected to the structure or union through a normal `member`
relationship.

Given the following model:

```
@mixin
structure PaginatedInput {
    nextToken: String,
    pageSize: Integer
}

structure ListSomethingInput with [PaginatedInput] {
    nameFilter: String
}
```

A `mixin` relationship exists from `ListSomethingInput` to `PaginatedInput`.

The following two selectors:

```
structure[id|name = ListSomethingInput] -[mixin]-> *
```

```
structure[id|name = ListSomethingInput] > structure
```

Both yield the following shapes:

- `smithy.example#PaginatedInput`

A `member` relationship exists from `ListSomethingInput` to the following
shapes, in any order:

- `smithy.example#ListSomethingInput$nameFilter`
- `smithy.example#ListSomethingInput$nextToken`
- `smithy.example#ListSomethingInput$pageSize`

The following two selectors:

```
structure[id|name = ListSomethingInput] > member
```

```
structure[id|name = ListSomethingInput] -[member]-> *
```

Both yield the following shapes, in any order:

- `smithy.example#ListSomethingInput$nameFilter`
- `smithy.example#ListSomethingInput$nextToken`
- `smithy.example#ListSomethingInput$pageSize`


### Mixins and member ordering

The order of structure and union members is important for languages like C
that require a stable ABI. Mixins provide a deterministic member ordering.
Members inherited from mixins come before members defined directly in the
shape.

Members are ordered in a kind of depth-first, preorder traversal of mixins
that are applied to a structure or union. To resolve the member order of a
shape, iterate over each mixin applied to the shape in the order in which they
are applied, from left to right. For each mixin, iterate over the mixins
applied to the mixin in the order in which mixins are applied. When the
evaluated shape has no mixins, the members of that shape are added to the
resolved list of ordered members. After evaluating all the mixins of a shape,
the members of the shape are added onto the resolved list of ordered members.
This process continues until all mixins and the members of the starting shape
are added to the ordered list.

Given the following model:

```
@mixin
structure FilteredByName {
    nameFilter: String
}

@mixin
structure PaginatedInput {
    nextToken: String,
    pageSize: Integer
}

structure ListSomethingInput with [
    PaginatedInput
    FilteredByName
] {
    sizeFilter: Integer
}
```

The members are ordered as follows:

- `nextToken`
- `pageSize`
- `nameFilter`
- `sizeFilter`


### Mixins on shapes with non-member properties

Some shapes don't have members, but do have other properties. Adding a mixin
to such a shape merges the properties of each mixin into the local shape. Only
certain properties may be defined in the mixin shapes. See the sections below
for which properties are permitted for each shape type.

Scalar properties defined in the local shape are kept, and non-scalar
properties are merged. When merging map properties, the values for local keys
are kept. The ordering of merged lists / sets follows the same ordering as
members.

#### Service mixins

Service shapes with the `@mixin` trait may define any property. For example,
in the following model:

```
operation OperationA {}

@mixin
service A {
    version: "A"
    operations: [OperationA]
}

operation OperationB {}

@mixin
service B with [A] {
    version: "B"
    rename: {
        "smithy.example#OperationA": "OperA"
        "smithy.example#OperationB": "OperB"
    }
    operations: [OperationB]
}

operation OperationC {}

service C with [B] {
    version: "C"
    rename: {
        "smithy.example#OperationA": "OpA"
        "smithy.example#OperationC": "OpC"
    }
    operations: [OperationC]
}
```

The `version` property of the local shape is kept and the `rename` and
`operations` properties are merged. This is equivalent to the following:

```
operation OperationA {}

operation OperationB {}

operation OperationC {}

service C {
    version: "C"
    rename: {
        "smithy.example#OperationA": "OpA"
        "smithy.example#OperationB": "OperB"
        "smithy.example#OperationC": "OpC"
    }
    operations: [OperationA, OperationB, OperationC]
}
```

#### Resource mixins

Resource shapes with the `@mixin` trait MAY NOT define any properties. This is
because every property of a resource shape is intrinsically tied to its set of
identifiers. Changing these identifiers would invalidate every other property
of a given resource.

Example:

```
@mixin
@internal
resource MixinResource {}

resource MixedResource with [MixinResource] {}
```

#### Operation mixins

Operation shapes with the `@mixin` trait MAY NOT define an `input` or `output`
shape other than `smithy.api#Unit`. This is because allowing input and output
shapes to be shared goes against the goal of the `@input` and `@output` traits.

Operation shapes with the `@mixin` trait MAY define the errors shape.

Example:

```
@mixin
operation MixinOperation {
    errors: [MixinError]
}

operation MixedOperation with [MixinOperation] {
    error: [MixedError]
}

@error("client")
structure MixinError {}

@error("client")
structure MixedError {}
```

### `@mixin` trait

The `@mixin` trait is a structured trait defined in the Smithy prelude as:

```
@trait(selector: ":not(member)")
structure mixin {
    localTraits: LocalMixinTraitList
}

@private
list LocalMixinTraitList {
    member: LocalMixinTrait
}

@idRef(
    selector: "[trait|trait]",
    failWhenMissing: true,
    errorMessage: """
            Strings provided to the localTraits property of a mixin trait
            must target a valid trait.""")
@private
string LocalMixinTrait
```

The `@mixin` trait has a single optional member named `localTraits` that
contains a list of shape IDs. Each shape ID MUST reference a valid trait that
is applied directly to the mixin. The traits referenced in `localTraits` are
not copied onto shapes that use the mixin. `localTraits` only affects traits
applied to the mixin container shape and has no impact on the members
contained within a mixin.

> Note: `smithy.api#mixin` is considered implicitly present in the
> `localTraits` property and does not need to be defined in the list of
> local traits.


### Reference implementation notes

Mixins will be implemented in Smithy's Java reference implementation in a way
that makes mixin members work just like normal structure and union members
(note that other implementations of Smithy are free to implement mixins
in whatever way works best). For example, when calling `Shape#getAllMembers`,
both mixin members and members local to the shape are returned. This reduces
the complexity of code generators and will prevent issues with code generators
forgetting to traverse mixins to resolve members.

The reference implementation will contain a model transformation that can
"flatten" mixins out of a model so that they do not need to be accounted for
in code generators and other tooling that performs exogenous model
transformations.

Smithy's diff implementation should be updated to understand the impact of
mixins on backward compatibility, especially mixins that are marked with the
`@private` trait.


### Converting to OpenAPI, JSON Schema, and other models

When converting Smithy models to OpenAPI, JSON Schema, and other models that
do not directly support the same mixin semantics as Smithy, tooling MUST
remove all traces of mixins by flattening mixins into their target shapes.
This removes the risk of consumers relying on mixins as normal types and still
retains the flexibility modelers need in order to refactor mixins in and out
of shapes.


## Alternatives and trade-offs


### Make mixin a type rather than a trait

A mixin is created in the current proposal by applying the `@mixin` trait to a
structure or union. An alternative approach is to introduce a new type called
`mixin` that does not require a trait. For example:

```
mixin CityResourceInput {
    @httpLabel
    @required
    cityId: String
}
```

This approach means that code generators do not need to check if a shape is a
mixin to determine whether to code generate a shape. However, many of Smithy's
code generators are implemented using a visitor pattern that dispatches based on
a shape's type, so adding a new mixin type to Smithy still requires a code
change to implement a new visitor method to handle mixins. This is also a
breaking change to any implementation of Smithy's visitor interfaces. One way to
mitigate the breaking change is to add a `default` implementation to Smithy
visitor interfaces for mixin shapes that just delegates to how structure shapes
are handled, though this would still require special-casing of mixins when
performing code generation.

Another issue with this approach is that selector-based validation would need to
be updated to either treat mixins as a subtype of structures _or_ require that
trait selectors explicitly support mixin members. For example, the `@required`
trait is defined as:

```
@trait(selector: "structure > member")
structure required {}
```

If a mixin is treated as a subtype of a structure, then the selector works
as-is, and new syntax would be needed to select only mixins. However, if
mixins are not treated as a subtype of structure, then the selector needs to
become:

```
@trait(selector: ":is(structure, mixin) > member")
structure required {}
```

Using a trait to define mixins allows Smithy to use mixins with both structures
and unions. If we wish to keep that functionality, treating a mixin as a subtype
of a structure could lead to subtle and hard to detect validation issues.
Consider the following example:

```
mixin UserActions {
    @required
    subscribe: SubscribeAction,
}

union AdminActions with [UserAction] {}
```

The `required` trait on `UserActions$subscribe` is valid, but it makes it so
that the `UserActions` mixin is impossible to merge into `AdminActions` since
the `required` trait is not permitted on union members.

One possible solution to make the intended type of a mixin explicit is to just
prefix a shape definition with the mixin keyword, though this is essentially the
same thing as a trait, and if-statements are still needed in code generators to
check if a shape is a mixin and should be code generated.

```
mixin structure CityResourceInput {
    @httpLabel
    @required
    cityId: String
}
```

Using the `@mixin` trait special-cases a shape as a mixin without significant
effort to model parsers, existing selector-based validation can be used as-is
without needing to account for mixins, and the intended type of the mixin is
defined through a normal shape definition. Using a trait also keeps the door
open for the possibility of using mixins with other shapes (for example,
service, resource, and operation shapes).


### Allow mixins to remove traits

Mixins allow traits to be added or replaced, but not removed. We could allow
mixins to remove traits from members by adding something like an `@override`
trait with an `omitTraits` property. For example:

```
@mixin
structure FooMixin {
    @required
    someMember: String

    otherMember: String
}

structure ApplicationOfFooMixin with [FooMixin] {
    // Remove the required trait from this member.
    @override([omitTraits: [required]])
    someMember: String
}
```

While this could work, it is not strictly required and presents two
tradeoffs: the application of the mixin has significantly more verbose, it
requires introducing an `@override` trait, and filtering traits adds more
complex requirements to Smithy implementations that resolve the traits of a
structure.

Alternatively, multiple levels of mixins can be used in many cases to allow
for reuse with more flexibility. For example:

```
@mixin
structure FooMixinOptional {
    someMember: String
    otherMember: String
}

@mixin
structure FooMixinRequired with [FooMixinOptional] {}
apply FooMixinRequired$someMember @required
```


### Codegenerate and expose mixins as interface types

In the current proposal, mixins are not code generated and provide no runtime
polymorphism. An alternative approach could expose mixins as interfaces in
programming languages, and any time a member refers to a mixin, structures that
use the mixin can be provided. However, to avoid the drawbacks of over-the-wire
polymorphism and because many programming languages don't support multiple
inheritance, providing a structure that uses a mixin to a member that expects a
mixin will destructively up-cast the value and lose any additional information-
that is, if a member asks for a `PaginatedInput` mixin, that's all it will ever
get.

This approach provides some abstraction in that wider types can be provided in
places that require a compatible narrower type. However, this approach might be
surprising to developers that expect to be able to losslessly serialize a
structure that uses a specific mixin implementation in the place of a mixin
interface.


### Add inheritance to Smithy

An alternative approach, though not mutually exclusive, to mixins is
inheritance (i.e., polymorphic subtyping). Inheritance provides similar
benefits that mixins provide and would also be exposed as a polymorphic
abstraction at runtime from code generators. For example, the following model
defines a structure that inherits from another structure.

```
structure PaginatedInput {
    nextToken: String,
    pageSize: Integer
}

structure ListSomethingInput extends PaginatedInput {}
```

Inheritance isn't supported because of the problems it has with distributed
systems and because it is not well-supported in every programming language,
including languages that are growing in popularity like Rust and Go. Emulating
inheritance over the wire introduces backward compatibility concerns that are
not encountered when working with polymorphism in code; code knows the entire
world of possible types, whereas client and server interactions often leave
clients with partial and outdated knowledge of the types supported by the
service. Instead of inheritance, Smithy provides several alternatives to
inheritance, including tagged unions for describing data that can take on one
of several known forms, document types to send and receive open content, and
the proposed mixins for structural reuse.

Clients of a service are not forced to update each time an update is made to a
service. This presents a challenge for trying to expose on-the-wire inheritance.
Specializing classes in a _programming language_ is generally a safe change
that won't break consumers. However, specializing structures can break clients.

Consider the following Smithy model:

```
structure OperationOutput {
    thing: Thing
}

structure Thing {
    id: Id
}
```

The client and server are in sync the day that the service launches. However,
eventually the service team needs to add a new subtype to the "Thing" structure
named "Appliance". This change is made to the model and pushed to production.

```
structure Appliance extends Thing {
    manufacturer: `ManufacturerId`
}
```

Customers using the generated client for the service do not yet know about the
newly introduced subtype because they have an older build of the client. What
happens when they receive this new type in a member reference? The client code
might see something like this over the wire:

```
{
    "thing": {
        "__type": "Appliance",
        "id": "1234",
        "manufacturer": "GE"
    }
}
```

The client code see that the "__type" is "Appliance". However, the client has
no idea what "Appliance" is, so it upcasts the value to the type that it
concretely knows it implements: "Thing". This has the effect of losing type
information in the client and drops the newly introduced "manufacturer" data.

Let's assume the client code is updated to now know about the "Appliance"
subtype. They also begin to add `instanceof` checks in their code to execute
different code paths based on the type of "Thing" they receive:

```
public void handleResponse(OperationOutput output) {
    Thing thing = output.getThing();
    if (thing instanceof Appliance) {
        System.out.println("Got Appliance: " + ((Appliance) thing).getManufacturer());
    } else {
        System.out.println("Got Thing");
    }
}
```

Their updated code now correctly prints out "Got Appliance: GE" when they
receive an "Appliance' shape.

Now the service team decides that they need to further specialize the
"Appliance" structure because they need to provide wattage information about
"Microwave" appliances:

```
structure Microwave extends Appliance {
    wattage: WattageInteger
}
```

The client code then begins seeing this specialized "Microwave" structure over
the wire:

```
{
    "thing": {
        "__type": "Microwave",
        "id": "1234",
        "manufacturer": "GE",
        "wattage": 100
    }
}
```

Because the client has no idea what a "Microwave" is, they downcast the value to
the type they do know about: "Thing". This loses all the data of not only the
"Microwave" but also the "Appliance" structure. Furthermore, it completely
breaks client code that was previously specialized for the "Appliance"
structure.

There are possible ways to deal with this, including sending the entire type
hierarchy over the wire. This way, clients could iterate over the hierarchy
until they encounter a type they understand:

```
{
    "thing": {
        "__types": [
            "smithy.example#Microwave",
            "smithy.example#Appliance",
            "smithy.example#Thing"
        ],
        "id": "1234",
        "manufacturer": "GE",
        "wattage": 100
    }
}
```

There are a few problems wit this approach. It becomes very chatty when the
same structure is serialized multiple times (for example, a list of 100
structures would repeat type information 100 times). More complex memoization
could be added to deserializers to remember the type information for things
the first time they see them, though this would add to (de)serialization
complexity, and isn't something that would be supported by serialization
formats that can skip over parsing entire portions of a document
(e.g., Amazon Ion). Smithy also needs to provide reasonable interoperability
with other models like OpenAPI, and it uses a single key-value pair to provide
a discriminator.

In summary, mixins do not introduce any kind of runtime polymorphism, so it
provides the benefits of structural model reuse, but avoids the downsides of
polymorphic subtyping.
