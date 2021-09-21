# Default zero values

* **Author**: Michael Dowling
* **Created**: 2021-08-24
* **Last updated**: 2021-12-16


## Abstract

This document describes a fundamental change to the way Smithy describes
structure member nullability (also known as optionality). By adding a
`@default` trait to structure members, code generators can generate non-null
accessors (or getters) for members marked as `@default` or `@required`
(with some caveats for backward compatibility). While this proposal is not
AWS-specific, the practical impact of it is that when implemented, it will
convert thousands of optional property accessors to non-optional in the
Rust, Kotlin, and Swift AWS SDKs.

In order to implement these changes, Smithy will need a 2.0 bump of the IDL.


## Terms used in this proposal

* null: This document uses the term null, nullable, and nullability to refer
  to members that optionally have a value. Some programming languages don't
  have a concept of `null` and need to map this to whatever abstraction is
  appropriate for that programming language (for example, `Option` in Rust
  or `Maybe` in Haskell).
* accessor, getter: In this proposal, the terms "accessor" and "getter" should
  be considered generic terms that translate to however a programming language
  exposes structure member values. For example, some programming languages like
  Java often use getter methods while others might expose struct style
  properties directly.


## Motivation

Most structure member accessors generated from Smithy models return nullable
values. As new languages like Rust and Kotlin that explicitly model nullability
in their type systems adopt Smithy, excessive nullability in generated code
becomes burdensome to end-users because they need to call methods like
`.unwrap()` on everything. Generating every structure member as nullable makes
it hard for customers to know when it is safe to dereference a value and when
it will result in an error. Adding the ability to control when a value is
nullable vs when a value is always present provides an ergonomic and safety
benefit to end users of code generated types.

For example, after this proposal is implemented, the following Rust code:

```rust
println!("Lexicons:");
let lexicons = resp.lexicons.unwrap_or_default();
for lexicon in &lexicons {
    println!(
        "  Name:     {}",
        lexicon.name.as_deref().unwrap_or_default()
    );
    println!(
        "  Language: {:?}\n",
        lexicon
            .attributes
            .as_ref()
            .map(|attrib| attrib
                .language_code
                .as_ref()
                .expect("languages must have language codes"))
            .expect("languages must have attributes")
    );
}
println!("\nFound {} lexicons.\n", lexicons.len());
```

Can be simplified to:

```rust
println!("Lexicons:");
for lexicon in &resp.lexicons {
    println!(" Name:     {}",  lexicon.name);
    println!(" Languages: {:?}\n",
        lexicon.attributes.map(|attrib| attrib.language_code)
    );
}
println!("\nFound {} lexicons.\n", resp.lexicons.len());
```


## Background

Nullability in Smithy IDL 1.0 is controlled in the following ways:

1. Map keys, values in sets, and unions cannot contain null values. This
   proposal does not change this behavior.
2. List and map values cannot contain null values by default. The `@sparse`
   trait applied to list and map shapes allow them to contain null entries.
   These kinds of collections rarely need to contain nullable members. This
   proposal does not change this behavior.
3. The nullability of structure members in Smithy today is resolved using the
   following logic:
   1. If the member is marked with `@box`, it's nullable.
   2. If the shape targeted by the member is marked with `@box`, it's nullable.
   3. If the shape targeted by the member is a byte, short, integer, long,
      float, double, or boolean, it's non-null.
   4. All other members are considered nullable.


### Why the `@required` trait was unreliable for codegen

Removing the `@required` trait from a structure member has historically been
considered backwards compatible in Smithy because it is loosening a restriction.
The `@required` trait in Smithy 1.0 is categorized as a constraint trait and
only used for validation rather than to influence generated types. This gives
service teams the flexibility to remove the required trait as needed without
breaking generated client code. The `@required` trait might be removed if new
use cases emerge where a member is only conditionally required, and more
rarely, it might be added if the service team accidentally omitted the trait
when the service initially launched.

For context, as of May 2021, the `@required` trait has been removed from a
structure member in 105 different AWS services across 618 different members.
Encoding the `@required` trait into generated types would have made changing a
member from required to optional a breaking change to generated code, which is
something most services try to avoid given its frequency and the cost of
shipping a major version of a service and client. While this data is
AWS-specific, it's indicative of how often disparate teams that use Smithy
(or its predecessor in of Amazon) sought to update their APIs so that members
can become optional.


## Goals and non-goals

**Goals**

1. Reduce the amount of nullability in code generated from Smithy models.
2. Maintain similar backward compatibility guarantees that exist today like
   being able to backward compatibly remove the `@required` trait from a
   structure member without breaking previously generated clients.
3. Maintain Smithy's protocol-agnostic design. Protocols should never influence
   how types are generated from Smithy models; they're only intended to change
   how types are serialized and deserialized. In practice, this means that
   features of a protocol that extend beyond Smithy's metamodel and definitions
   of structure member nullability should not be exposed in code generated
   types that represent Smithy shapes.
4. We should be able to use the same code-generated types for clients, servers,
   and standalone type codegen.

**Non-goals**

1. Mandate how protocols are designed. This proposal intends to define
   requirements and affordances about how nullability is defined the Smithy
   metamodel, and in turn, in code generated from Smithy models.


## High-level summary

This proposal introduces a `@default` trait to structure members that
initializes structure members with a default, zero value.

```
structure Message {
    @required
    title: String

    @default
    message: String
}
```

In the above example:

- `title` is marked as `@required`, so it is non-null in generated code.
- `message` is also non-null in generated code and is assigned a default, zero
   value of `""` when not explicitly provided. This makes it easier to use
   `title` and `message` in code without having to first check if the value is
   `null`. For example, you can call `message.message.size()` without first
   checking if the value is non-null.

If the service ever needed to make `title` optional, they can replace the
`@required` trait with the `@default` trait:

```
structure Message {
    @default
    title: String

    @default
    message: String
}
```

With the above change, codegen remains the same: both values are non-null.
However, if `title` is not set in client code, server-side validation for the
type will _not_ fail because `title` has a default, zero value of `""`. The
trade-off to this change is that it is impossible to tell if `title` was
omitted or explicitly provided.


## Proposal overview

This proposal will be implemented in two phases. The rules around nullability
will be extended in Smithy IDL 1.0, and then changed in a backward incompatible
way to simplify nullability rules in Smithy IDL 2.0. These breaking changes to
the IDL will be made in such a way that 1.0 and 2.0 models can be loaded
simultaneously without a major version bump in Smithy's Java libraries. This
phased approach was chosen because we know that the number of models and
tooling yet to be written for Smithy far exceeds the current number of models
already written for Smithy. We'll provide a path for existing 1.0 models to more
easily and confidently upgrade to IDL 2.0, and leave behind the more confusing
nullability semantics of IDL 1.0 going forward as 2.0 becomes the default.


### In Smithy IDL 1.0

1. Add the `@default` trait to structure members.
2. Warn when a structure member has non-nullable semantics but is not marked as
   `@default`.
3. Warn when `@box` is used on structure members.
4. Warn when any byte, short, integer, long, float, double, or boolean shape is
   _not_ marked with the `@box` trait. We want all shapes to be considered
   nullable by default, and for structure members to opt-in to non-nullable
   semantics through the `@default` trait or `@required` trait.
5. Deprecate the `PrimitiveBoolean`, `PrimitiveShort`, `PrimitiveInteger`,
   `PrimitiveLong`, `PrimitiveFloat`, and `PrimitiveDouble` shapes from the
   Smithy prelude. Instead, target their corresponding prelude shapes like
   `Boolean`, `Short`, `Integer`, etc.
6. Create new guidance for code generators that allows them to generate
   non-nullable accessors for structure members marked as `@required` or
   `@default`.


### In Smithy IDL 2.0

1. Remove the `@box` trait from the Smithy 2.0 prelude. IDL 2.0 models will
   fail if they use the `@box` trait.
2. Remove the `PrimitiveBoolean`, `PrimitiveShort`, `PrimitiveInteger`,
   `PrimitiveLong`, `PrimitiveFloat`, and `PrimitiveDouble` shapes from the
   Smithy 2.0 prelude. IDL 2.0 models will fail if they target these shapes.
3. Update the Smithy IDL 2.0 model loader implementation to be able to load
   Smithy 1.0 models alongside Smithy 2.0 models.
   1. Warn when a Smithy 1.0 model file is loaded.
   2. Inject the `@default` trait on structure members when needed.
   3. Remove the `@box` trait from the model.
   4. Rewrite members that target one of the removed Primitive* shapes to
      target the corresponding non-primitive shape in the prelude (for example,
      `PrimitiveInteger` is rewritten to target `Integer`).


## `@default` trait

The `@default` trait can be applied to structure members to indicate that the
targeted shape has a default, zero value. The `@default` trait *does not* allow
for a custom default value.

The following example defines a structure with a `@default` "title" member that
has a default zero value:

```
structure Message {
    @default
    title: String // defaults to ""
}
```

The `@default` trait is defined in Smithy as:

```
@trait(selector: """
       "structure > member
        :not(> :test(union, structure > :test([trait|required])))"""",
       conflicts: [required, box])
structure default {}
```

The `@default` trait conflicts with the `@box` trait and `@required` trait. The
following model is invalid:

```
structure Message {
    @default    // ERROR: this trait conflicts with @required.
    @required
    title: String
}
```


### **Default zero values**

The following list describes the default zero value of each kind of shape.
Programming languages and code generators that cannot initialize structure
members with the following default values SHOULD continue to represent those
members as nullable as this is semantically equivalent to the default zero
value.

- Boolean: boolean false (`false`)
- Numbers: numeric zero (`0`)
- String: an empty string (`""`). Strings with the enum trait also have a
  default value of "".
- Blob: an empty blob. This includes blob shapes marked as `@streaming`.
- Timestamp: zero seconds since the epoch (for example, `0`, or
  `1970-01-01T00:00:00Z`).
- Document: a null document value.
- List: an empty list (`[]`).
- Set: an empty set (`[]`).
- Map: an empty map (`{}`).
- Structure: an empty structure. Structures only have a default zero value if
  none of the members of the structure are marked with the `@required` trait.
- Union: no default zero value. A union MUST be set to one of its variants for
  it to be valid, and unions have no default variant.


### Impact on API design

Members marked with the `@default` trait cannot differentiate between whether a
property was set to its zero value or if a property was omitted. Default zero
values are part of the type system of Smithy and not specific to any particular
serialization of a shape. This makes the `@default` trait a poor choice for
partial updates or patch style APIs where it is critical to differentiate
between omitted values and explicitly set values.

To help guide API design, built-in validation will be added to Smithy to detect
and warn when `@default` members are detected in the top-level input of
operations that start with `Update`, operation bound to the `update` lifecycle
of a resource, or operations that use the `PATCH` HTTP method.

For example, the following model:

```
$version: "2"
namespace smithy.examnple

operation UpdateUser {
    input: UpdateUserInput
}

structure UpdateUserInput {
    @default
    username: String
}
```

Would emit a warning similar to the following:

```
WARNING: smithy.example#UpdateUserInput$userName (DefaultOnUpdate)
     @ /path/to/main.smithy
     |
 4   | operation UpdateUser {
     | ^
     = This update style operation has top-level input members marked with the
       @default trait. It will be impossible to tell if the member was omitted
       or explicitly provided. Affected members: [UpdateUserInput$username].
```


### Constraint trait validation

Constraint traits are not evaluated on structure members marked with the
`@default` trait when the value of the member is the default value.

Consider the following structure:

```
structure Foo {
    @default
    @range(min: 5, max: 10)
    number: Integer
}
```

The ``Foo$number`` member is marked with the `@range` trait, requiring a value
between 5 and 10 inclusive. Because it is marked with the `@default` trait,
the default value, `0`, is also permitted.

A member marked with the `@default` trait implicitly adds the default value
of the member to its set of permitted values. This is equivalent to how
optional members that are not marked with `@default` or `@required` implicitly
allow for null or omitted values.


### Backward compatibility of the `@default` and `@required` trait

Backward compatibility rules of the `@default` and `@required` traits are as
follows:

- The `@default` trait can never be removed from a member.
- The `@default` trait can only be added to a member if the member was
  previously marked as `@required`.
- The `@required` trait cannot be added to a member. This would transition the
  member from nullable to non-nullable in generated code.
- The `@required` trait can only be removed under the following conditions:
  - It is replaced with the `@default` trait
  - The containing structure is marked with the `@input` trait, meaning it is
    only used as the input of a single operation. This affordance is only given
    to top-level members of structures marked with the `@input` trait, and it
    does not apply to nested input members.

For example, if on _day-1_ the following structure is released:

```
structure Message {
    @required
    title: String

    message: String
}
```

On _day-2_, the service team realizes Message does not require a `title`, so
they add the `@default` trait and remove the `@required` trait. The member will
remain non-nullable because clients, servers, and any other deserializer will
now provide a default value for the `title` member if a value is not provided by
the end user.

```
structure Message {
    @default
    title: String

    message: String
}
```

Backward compatibility guarantees of the `@default` and `@required` traits
SHOULD be enforced by tooling. For example, smithy-diff in Smithy's reference
implementation will be updated to be aware of these additional backward
compatibility rules.


#### Special casing for @input structures

The `@input` trait special-cases a structure as the input of a single
operation. `@input` structures cannot be referenced in any other place in the
model. Structures marked with the `@input` trait have more relaxed backward
compatibility guarantees. It is backward compatible to remove the `@required`
trait from top-level members of structures marked with the `@input` trait, and
the `@required` trait does not need to be replaced with the `@default` trait.
This gives service teams the ability to remove the `@required` trait from
top-level input members without risking breaking previously generated clients.


## Deprecations in Smithy IDL 1.0 and removals in IDL 2.0

To simplify member nullability, the `@box` trait will be deprecated in Smithy
1.0 for structure members, and removed entirely in Smithy 2.0. Instead of
relying on the `@box` trait, Smithy validation will encourage modelers to
target nullable shapes rather than make a non-nullable shape null. This
validation will make the transition for models from Smithy 1.x to Smithy 2.x
easier because the removal of the `@box` trait will not impact a model. The
Smithy Java implementation's `NullableIndex` will account for the new and
existing nullability rules.

The following shapes in the prelude will be marked as deprecated in Smithy 1.0,
and removed in Smithy 2.0: `PrimitiveBoolean`, `PrimitiveShort`,
`PrimitiveInteger`, `PrimitiveLong`, `PrimitiveFloat`, and `PrimitiveDouble`.
Instead of referring to these shapes, refer to their non-primitive shape
variants (for example, `Integer`, `Boolean`) and apply the `@default` trait to
structure members. A warning will be emitted when models refer to these shapes,
which encourages models to update to use shapes that are forward compatible
with an eventual Smithy 2.0.

Smithy 2.0 IDL support will be added to the 1.x series of Smithy's Java tooling
so that both 1.0 and 2.0 models can be loaded, simultaneously, and automatically
converted from 1.0 to 2.0.


## Guidance on code generation

Code generated types for structures SHOULD use the `@default` trait and
`@required` traits to provide member accessors that always return non-null
values.

- When the `@default` trait is present on a member, the corresponding accessor
  SHOULD always return a non-null value by defaulting missing members with
  their zero values.
- When the `@required` trait is present on a member, the corresponding accessor
  SHOULD always return a non-null value.
- Smithy implementations in languages like TypeScript that do not provide a kind
  of constructor or builder to create structures may not be able to set default
  values, precluding them from being able to treat `@required` and `@default`
  members as non-null.
- Because the `@required` trait can be backward-compatibly removed from members
  of structures marked with the `@input` trait (that is, the input of an
  operation), code generators MUST generate code that does not break if the
  required trait is removed from these members. For example, this could mean
  generating these shapes as a kind of builder pattern or using all optional
  members.


### Guidance on protocol design

Protocols MAY choose if and how the `@default` trait impacts serialization and
deserialization. However, this proposal offers the following general
best-practices for protocol designers:

1. Serializing the default zero value of a member marked with the `@default`
   trait can lead to unintended information disclosure. For example, consider
   a newly introduced structure member marked with the `@default` trait that is
   only exposed to customers of a service that are allowlisted into a private
   beta. Serializing the zero values of these members could expose the feature
   to customers that are not part of the private beta because they would see
   the member serialized in messages they receive from the service.
2. Protocol deserialization implementations SHOULD tolerate receiving a
   serialized default zero value. This also accounts for older clients that
   think a structure member is required, but the service has since transitioned
   the member to use the `@default` trait.
3. Client implementations SHOULD tolerate structure members marked as
   `@required` that have no serialized value. For example, if a service
   migrates a member from `@required` to `@default`, then older clients SHOULD
   gracefully handle the zero value of the member being omitted on the wire.
   In this case, rather than failing, a client SHOULD set the member value to
   its default zero value. Failing to deserialize the structure is a bad
   outcome because what the service perceived as a backward compatible change
   (i.e., replacing the `@required` trait with the `@default` trait) could
   break previously generated clients.


## AWS specific changes

### AWS Protocols

All existing AWS protocols (that is, `aws.protocols#restJson1`,
`aws.protocols#awsJson1_0`, `aws.protocols#awsJson1_1`,
`aws.protocols#restJson1`, `aws.protocols#restXml`, `aws.protocols#awsQuery`,
and `aws.protocols#ec2Query`) MUST adhere to the following requirements, and
all future AWS protocols SHOULD adhere to the following:

1. To avoid information disclosure, serializers SHOULD omit the default zero
   value of structure members marked with the `@default` trait.
2. Deserializers MUST tolerate receiving the default zero value of a structure
   member marked with the `@default` trait.
3. Client deserializers MUST fill in a default zero value for structure members
   marked with the `@required` trait that have no serialized value, and the
   targeted shape supports zero values. This prevents older clients from
   failing to deserialize structures at runtime when the `@required` trait is
   replaced with the `@default` trait.


### Balancing old and new assumptions

Smithy comes from a long line of service frameworks built inside Amazon dating
back nearly 20 years. Until this proposal, the required trait was only treated
as server-side input validation. This causes two major issues that impacts the
AWS SDK team's ability to fully use the required trait for code generation:

1. Many teams did not consider the required trait as something that impacts
   output. This means that there are many members in AWS that should be
   marked as required but are not, and there are members that are required in
   input, but not always present in output.
2. Most teams designed their APIs under the guidance that the required trait
   can be removed without breaking their end users.

We will work to improve the accuracy and consistency of the required trait in
AWS API models where possible, but it will take significant time and effort
from hundreds of different teams within AWS. To accommodate this shift in
modeling changes, we will introduce AWS-specific traits to influence code
generation and provide AWS SDK specific code generation recommendations.


#### `aws.api#clientOptional` trait

The `aws.api#clientOptional` trait is used to indicate that a structure member
SHOULD be unconditionally generated as optional regardless of if the member
targets a shape with a default value, or if the member is marked with th
 `@default` trait, or if the member is marked with the `@required` trait.
This trait allows documentation generators to indicate that a member is
required, even if it is not reflected in generated code.

The `aws.api#clientOptional` trait is defined in Smithy as:

```
$version: "2"
namespace aws.api

@trait(selector: "structure > member")
structure clientOptional {}
```

Consider the following model:

```
$version: "2"
namespace smithy.examnple

use aws.api#clientOptional

structure ProductData {
    @clientOptional
    @required
    description: String
}
```

When generating an AWS SDK client for this shape, the `ProductData$description`
member MUST be generated as an optional member rather than always present.


##### Backfilling the clientOptional trait

The `aws.api#clientOptional` trait will be backfilled onto AWS models as
needed. Some more problematic services will use this trait with every required
structure member, whereas others will only use it on structure members that
target structure or union shapes that have no default zero value. Over time, as
models are audited and corrected, we can remove the `@clientOptional` trait
and release improved AWS SDKs.

Backfilling this trait on AWS models is admittedly be an inconvenience for
developers, but it is less catastrophic than previously generated client code
failing at runtime when deserializing the response of an operation, and it
matches the current reality of how AWS was modeled. While it's true client code
might start failing anyways if a service stops sending output members that were
previously marked as required, it would only fail when the client explicitly
attempts to dereference the value, and this kind of change is generally only
made when a client opts-into some new kind of functionality or workflow. If an
SDK fails during deserialization because a previously required output member
is missing, then customer applications would be completely broken until they
update their SDK, regardless of if the member is used in client code.


## Alternatives and trade-offs

## A note on added complexity in Smithy 1.x

Nullability rules in Smithy 1.x are already complex, and we're making them more
complex in this proposal. Before this proposal, a member was considered nullable
if it was marked with the `@box` trait or if it targeted a shape marked with the
`@box` trait, or anything other than a boolean or simple number. With this
proposal, we are also introducing the `@default` trait, further complicating
nullability. We will address this added complexity by releasing a 2.0 of the
Smithy IDL that removes the `@box` trait.

This proposal means the following unfortunate and confusing scenarios are
possible:

```
structure Example {
    // non-null because it defaults to false when not set
    @default
    a: Boolean

    // non-null because it's an unboxed boolean reference
    // We would emit a warning and ask that the @default trait be added.
    b: PrimitiveBoolean

    // non-null because it has the @default trait
    @default
    c: PrimitiveBoolean

    // non-null because it's @required
    @required
    d: Boolean

    // non-null because it's @required
    @required
    e: PrimitiveBoolean

    // nullable because it targets a boxed boolean
    f: Boolean

    // nullable because it boxes a primitive boolean
    // We would emit a warning and ask that the member target a nullable shape
    // instead of using the box trait on a member.
    @box
    g: PrimitiveBoolean

    // Nullable because it targets a shape with no zero value
    h: SomeStructure
}
```


### Don't do anything

We could keep things as-is, which means we avoid a major version bump in the
Smithy IDL, and we don't require any kind of campaign to apply missing
`@required` traits. This option is low-risk, but does mean that we'll continue
to expose an excessive amount of null values in Smithy codegen. This may become
more problematic as teams are beginning to use Smithy to model data types
outside of client/server interactions.


### Remove the `@required` trait

Another alternative for Smithy is to remove the `@required` trait and make
every structure member nullable. This is essentially how many Smithy code
generators function today, and removing the `@required` trait would codify this
in the specification. However, the `@required` trait still provides value to
services because they have perfect knowledge of what is and is not required,
and therefore can automatically enforce that required properties are sent from
a client. It also provides value in documentation because it defines at that
point in time which members a caller must provide and which members they can
expect in a response.


## FAQ


### What's the biggest risk of this proposal?

Service teams not understanding the traits, and then accidentally making
breaking changes to Smithy generated SDKs. Or they potentially need to make a
breaking change in SDKs because they did not understand the traits. We can
mitigate this somewhat through validation and backward compatibility checks.


### Is the `@default` trait only used to remove the `@required` trait?

No. It can be used any time the zero value of a structure member is a
reasonable default or can be treated the same as an omitted value. For example,
if `0` and `null` could communicate the same thing to a service, then using the
`@default` trait makes it easier to use the value because a null check isn't
needed before doing something with the value.


### Why not allow for custom default values?

Allowing a custom default value couples clients and servers and limits a
service's ability to change defaults. It takes a long time for customers to
update to newer versions of generated clients, and some customers never update.
Populating default values in clients would mean that it could take years for a
service team to migrate customers from one default to another. For example, it
might be reasonable to set a default `pageSize` for paginated operations to
1000, but the service team might later realize that the default should have been
500 (for whatever reason). However, only new customers and customers that update
to the newer version of the client will use the updated default.

Another issue with custom default values is that languages that do not provide
constructors or Smithy implementations that do not have constructors like
JavaScript cannot set a custom default value when a type is initialized. This
means that these languages either need to forbid creating structs through normal
assignment and provide factory methods, they need to forgo non-nullable members
altogether, or they need to require end-users to assign default values when
creating the type (i.e., to satisfy TypeScript structural typing).

An alternative approach is to allow custom default values, but default values
are never serialized. This allows defaults to evolve on the service without
updating clients. However, this presents inconsistent views between clients,
servers, and other parties as to what a structure member contains. For example,
if a service sets the default value of a string to "A" on day 1, and the client
also sees that the default value is "A", omitting "A" on the wire works and
there are no surprises. However, if the service updates the default value to "B"
on day 2, and the client has not yet updated, the service will omit "B" on the
wire, the client will assume the value is "A". If the client then sends this
structure back to the service, it will omit "A", and the service will assume
"B".

One final issue with custom default values: removing the `@required` trait and
replacing it with the `@default` trait works because when a client is
deserializing a structure, if what it assumed is a `@required` member is
missing, the client knows that the member was transitioned by the service to
optional and it can set the member to a deterministic default, zero value. If
the default value is variable, then a client cannot reasonably populate a valid
default value and would need to assume the zero value is the default. This works
for deserializing the structure, but if the structure were to be round-tripped
and sent back to the service, the client would see that the member is
`@required`, and it would send the assumed zero value instead of the actual
default value of the member. This is the primary reason why allowing custom
default values that are only honored by the service and not by clients isn't
possible to support.


### How will this impact smithy-openapi?

We will start using the `default` property of OpenAPI and JSON Schema. For
example:

```
{
  "components": {
    "schemas": {
      "Cat": {
          {
            "type": "object",
            "properties": {
              "huntingSkill": {
                "type": "string",
                "default": ""
              }
            },
            "required": [
              "huntingSkill"
            ]
          }
        ]
      }
    }
  }
}
```


### How many AWS members used in output marked as required?

As of March 17, 2021, 4,805 members.


### Can we disallow the box trait on members in 1.0 right now?

Not backward compatibly. For example, in AWS, it's used on 271 members.


### How often has the `@required` trait been removed from members in AWS?

The required trait has been removed from a structure member 618 different times
across 105 different services. Of those removals, 75 targeted structures, 350
targeted strings, and the remainder targeted lists, maps, and other simple
shapes.


### How often has the `@required` trait been added to members in AWS?

The required trait has been added to a member 9 different times across 5
different services.


### What are the `@input` and `@output` traits?

See https://github.com/awslabs/smithy/blob/main/designs/operation-input-output-and-unit-types.md
