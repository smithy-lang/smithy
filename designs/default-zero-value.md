# Default zero values

This document describes a fundamental change to the way Smithy describes
structure member nullability. By adding a `@default` trait to structure members,
code generators can generate non-null accessors (or getters) for members marked
as `@default` or `@required` (with some caveats for backward compatibility).
When implemented, this proposal will convert, at minimum, 18,000 currently
nullable property accessors to non-nullable in the Rust, Kotlin, and Swift
AWS SDKs.

In order to implement these changes, Smithy will need a 2.0 bump of the IDL.


## Motivation

Most accessors, or "getters", generated from Smithy models return nullable
values. As new languages like Rust and Kotlin that explicitly model nullability
(or optionality) in their type systems adopt Smithy, excessive nullability in
generated code becomes burdensome to end-users because they need to call methods
like `.unwrap()` on everything. Generating every structure member as nullable
makes it hard for customers to know when it is safe to dereference a value and
when it will result in an error. Adding the ability to control when a value is
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
2. List and map values by default do not contain null values. The `@sparse`
   trait applied to list and map shapes allow them to contain null entries.
   These kinds of collections rarely need to contain nullable members, so this
   proposal does not change this behavior.
3. The nullability of structure members in Smithy today is resolved using the
   following logic:
   1. If the member is marked with `@box`, it's nullable.
   2. If the shape targeted by the member is marked with `@box`, it's nullable.
   3. If the shape targeted by the member is a byte, short, integer, long,
      float, double, or boolean, it's non-null.
   4. All other members are considered nullable.


### Why the `@required` trait was unreliable for codegen

Code generated from Smithy has historically never used the `@required` trait
to influence code generation, and it was treated mostly as validation. Service
teams consider removing the required trait from a member a backward compatible
change because it is loosening a restriction. This gives service teams the
flexibility to add or remove the required trait as needed without breaking
generated client code. The `@required` trait might be removed if new use cases
emerge where a member is only conditionally required, and more rarely, it might
be added if the service team accidentally omitted the trait when the service
initially launched.

As of May 2021, the `@required` trait has been removed from a structure member
in 105 different AWS services across 618 different members. Encoding the
`@required` trait into generated types would have made changing a member from
required to optional a breaking change to generated code, which is something
we try to avoid given its frequency and the cost of shipping a major version
of a service and client.


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
However, if `title` is now omitted, validation for the type will _not_ fail
because `title` has a default, zero value of `""`. The trade-off to this change
is that it is impossible to tell if `title` was omitted or explicitly provided.


## Proposal overview

This proposal will be implemented in two phases. The rules around nullability
will be altered in Smithy IDL 1.0, and then changed in a backward incompatible
way to simplify nullability rules in Smithy IDL 2.0. These breaking changes to
the IDL will be made in such a way that 1.0 and 2.0 models can be loaded
simultaneously without a major version bump in Smithy's Java libraries. This
phased approach was chosen because we believe that the number of models and
tooling yet to be written for Smithy far exceeds the current number of models
already written for Smithy. We'll provide a path for existing 1.0 models to more
easily and confidently upgrade to IDL 2.0, and leave behind the more confusing
nullability semantics of IDL 1.0 going forward as 2.0 becomes the default.


### In Smithy IDL 1.0

1. Add the `@default` trait to structure members. See _`@default` trait_ for
   details.
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

1. Remove the `@box` trait from the Smithy prelude. IDL 2.0 models will fail if
   they use the `@box` trait.
2. Remove the `PrimitiveBoolean`, `PrimitiveShort`, `PrimitiveInteger`,
   `PrimitiveLong`, `PrimitiveFloat`, and `PrimitiveDouble` shapes from the
   Smithy prelude. IDL 2.0 models will fail if they target these shapes.
3. Update the Smithy IDL 2.0 model loader implementation to be able to load
   Smithy 1.0 models alongside Smithy 2.0 models.
   1. Warn when a Smithy 1.0 model file is loaded.
   2. Inject the `@default` trait on structure members when needed.
   3. Drop the `@box` trait from the model.


## `@default` trait

The `@default` trait can be applied to structure members to indicate that the
targeted shape has a default, zero value and cannot be set explicitly to null.
The `@default` trait *does not* allow for a custom default value.

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
       structure > member :not(> :is(structure, union))""",
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
members with the above default values MUST continue to represent those members
as nullable.

- Boolean: boolean false (`false`)
- Numbers: numeric zero (`0`)
- String: an empty string (`""`). Strings with the enum trait also have a
  default value of "".
- Blob: an empty blob. This includes blob shapes marked as `@streaming`.
- Timestamp: zero seconds since the epoch (`0`, or `1970-01-01T00:00:00Z`).
- Document: a null document value.
- List: an empty list (`[]`).
- Set: an empty set (`[]`).
- Map: an empty map (`{}`).
- Structure: no zero value. Structures are always nullable and never have a
  default value. In order for a structure to have a default value, all members
  of the structure would need to be optional. Because it is a backward
  compatible change to remove the `@required` trait from a structure member,
  whether a structure has a zero value or not is too volatile to rely on for
  code generation.
- Union: no zero value. Unions are always nullable, MUST be set to one of its
  variants for it to be valid, and have no default variant. Adding some kind of
  special "default" or "unset" value to unions in code to account for default
  values would ultimately require _more_ special-case code in generated code to
  use than just dealing with nullable unions.


### Impact on API design

Members marked with the `@default` trait cannot differentiate between whether a
property was set to its zero value or if a property was omitted. This makes the
`@default` trait a poor choice for Update/Patch-style APIs where it is critical
to differentiate between omitted values and explicitly set values.

To help guide API design, built-in validation will be added to Smithy to detect
and warn when `@default` members are detected in the top-level input of
operations that start with `Update`, operation bound to the `update` lifecycle
of a resource, or operations that use the `PATCH` HTTP method.

For example, the following model:

```
$version: "2.0"
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


### Impact on constraint trait validation

Modeled `@length`, `@pattern`, and `@range` traits need to account for zero
values introduced by `@default` traits. Smithy will detect when a constraint
can never be satisfied by a zero value and fail during model validation.

For example, consider the following structure:

```
structure Foo {
    @required
    @length(min: 1, max: 100)
    baz: String
}
```

If this structure is later updated so that `baz` is no longer required but has
the `@default` trait, then the following constraint trait can never hold true
when `baz` uses the zero value:

```
structure Foo {
    @default
    @length(min: 1, max: 100) // <-- ERROR: default value can never satisfy min > 1
    baz: String
}
```

With the updated member, it is now impossible to satisfy the `@length` trait
constraint trait when the zero value is used. Smithy will detect this when
validating the model and fail. To fix this, the `@length` trait needs to remove
the `min` constraint:

```
structure Foo {
    @default
    @length(max: 100) // Now it works!
    baz: String
}
```

This path was chosen rather than to special-case zero values in constraint
traits because it's simpler to implement, test, and is compatible with other
ecosystems that don't understand Smithy's zero value semantics like OpenAPI.


### Backward compatibility of the `@default` and `@required` trait

Backward compatibility rules of the `@default` and `@required` traits are as
follows:

- The `@default` trait can never be removed from a member.
- The `@default` trait can only be added to a member if the member was
  previously marked as `@required`.
- The `@required` trait can only be removed from a member if it is replaced with
  the `@default` trait. Removing `@required` and not adding `@default` would
  transition the member from non-nullable to nullable in generated code, so
  that is not considered backward compatible.
- The `@required` trait cannot be added to a member. This would transition the
  member from nullable to non-nullable in generated code.

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

Backward compatibility guarantees of the `@default` and `@required` traits will
be enforced in tools like smithy-diff.


### `@default` trait syntactic sugar

In Smithy IDL 2.0, we will add syntactic sugar for attaching the `@default`
trait to a member to make the importance of the `@default` trait more apparent,
and to make it easier to find all structure members that are initialized to a
default value. A member type followed by `= default` will be syntactic sugar
for applying the `@default` trait to the member.

The following structure:

```
structure Message {
    title: String = default
}
```

Is exactly equivalent to the following structure:

```
structure Message {
    @default
    title: String
}
```

This pairs with the `@required` trait syntactic sugar that is also being added
to Smithy IDL 2.0 (that is, appending `!` to a structure member target):

```
structure Message {
    title: String = default
    message: String!
}
```

Note that the following structure member definition is not valid because a
member cannot be marked as both `@required` and `@default`:

```
structure Message {
    title: String! = default // not valid syntax
}
```


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


## New guidance for code generators

Code generated types for structures SHOULD use the `@default` trait and
`@required` traits to provide member accessors that always return non-null
values.

- When the `@default` trait is present, the corresponding accessor always
  returns a non-null value by defaulting missing members with their zero values.
- When the `@required` trait is present on a member that targets a shape that
  has a zero value, the corresponding accessor always returns a non-null value.
- Smithy implementations in languages like TypeScript that do not provide a kind
  of constructor or builder to create structures may not be able to set default
  values, precluding them from being able to treat `@required` and `@default`
  members as non-null.


### Serialization

Members marked with the `@default` trait that are set to their default zero
values SHOULD NOT be serialized.

- Deserializers MUST tolerate receiving a serialized default value. Failing here
  would leave end-users with no recourse other than to wait for a client or
  server to update and fix their implementation. This also accounts for older
  clients that think a structure member is required, but the service has since
  transitioned the member to use the `@default` trait.
- Serializing zero values can cause unintended information disclosure like
  revealing internal-only members (this is the same reason that null values
  SHOULD NOT be serialized).


### Deserialization

Structure members marked as `@required` are not in and of themselves a guarantee
that they will be serialized. For example, if a service migrates a member from
`@required` to `@default`, then older clients MUST gracefully handle the zero
value of the member being omitted on the wire. In this case, rather than
failing, a client MUST set the member value to its default zero value. Failing
to deserialize the structure is a bad outcome because what the service
perceived as a backward compatible change (i.e., removing the `@required`
trait) could break previously generated clients. The only recourse for the
client is to tolerate the missing member and fill in the deterministic default
value.


## Alternatives and trade-offs

## A note on added complexity in Smithy 1.x

Nullability rules in Smithy 1.x were already complex, and we're making them more
complex in this proposal. Before this proposal, a member was considered nullable
if it was marked with the `@box` trait or if it targeted a shape marked with the
`@box` trait, or anything other than a boolean or number. With this proposal, we
are also introducing the `@default` trait, further complicating nullability. We
will address this added complexity if we release a Smithy 2.x that removes the
`@box` trait.

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


## FAQ


### What's the biggest risk of this proposal?

Service teams not understanding the traits, and then accidentally making
breaking changes to Smithy generated SDKs. Or they potentially need to make a
breaking change in SDKs because they didn't understand the traits. We can
mitigate this somewhat through validation and backward compatibility checks.


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
JavaScript can't set a custom default value when a type is initialized. This
means that these languages either need to forbid creating structs through normal
assignment and provide factory methods, they need to forgo non-nullable members
altogether, or they need to require end-users to assign default values when
creating the type (i.e., to make TypeScript structural typing happy).

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
the default value is variable, then a client can't reasonably populate a valid
default value and would need to assume the zero value is the default. This works
for deserializing the structure, but if the structure were to be round-tripped
and sent back to the service, the client would see that the member is
`@required`, and it would send the assumed zero value instead of the actual
default value of the member. This is the primary reason why allowing custom
default values that are only honored by the service and not by clients isn't
possible to support.


### How will Smithy code generators use these traits?

Code generators SHOULD be updated to make eligible accessors for properties
marked as `@required` or `@default` non-null. Smithy code generators that use
the
[NullableIndex](https://github.com/awslabs/smithy/blob/main/smithy-model/src/main/java/software/amazon/smithy/model/knowledge/NullableIndex.java)
will likely need to make very few changes because nullability calculations are
centralized in this `KnowledgeIndex`.


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


### Why do structures not have a zero value?

Because the zero value of a structure is conditional based on the members in the
structure, and backward compatible changes to the structure can cause backward
incompatible changes to members that reference the structure.

Despite the inability to code generate non-null structures, this proposal will
reduce member nullability of members marked as `@required` in AWS SDKs by 90%
(As of 2021-03-18, There are 4,805 structure members used in output marked as
required, of which 458 target structures; there are 3,382 members used in input
marked as required, of which 367 target structures).

Assume that members that target structures followed the same nullability rules
as other members, and consider the following model on _day 1_:

```
structure Foo {
    // Nullable because Baz has no zero value due to a required member.
    // Only members with a zero value can be non-null because we must
    // support the ability to remove the required trait by replacing
    // it with the @default trait.
    @required
    baz: Baz
}

structure Baz {
    @required
    bar: String
}
```

Consider if the model was changed to this on _day 2_:

```
structure Foo {
    // Non-nullable because Baz now has a zero value. A breaking change!
    @required
    baz: Baz
}

structure Baz {
    @default
    bar: String
}
```

In this example, `Baz` evolved backward compatibly, but Foo did not because
`Foo$baz` is now non-null.

There are a few ways to address this:

1. We don't ever treat required members that reference structures as non-null.
   This is the option chosen for this proposal.

2. We update `Foo$baz` to have a required trait like `@required(nullable: true)`
   to force nullability. However, this won't work because the team that owns
   `Baz` would need to coordinate their change with every team that references
   their structure to let them know they need to add the `nullable` attribute to
   required traits.

```
structure Foo {
    @required(nullable: true)
    baz: Baz
}
```

3. When `Baz` is updated on _day 2_, the `@box` trait is added to `Baz` to
   prevent any structure member from ever using a non-null accessor with it.
   This would almost certainly be forgotten by modelers, and would all but
   require smithy-diff to point out the compatibility issue.

```
@box
structure Baz {
    @default
    bar: String
}
```


### How many members used in output marked as required?

As of March 17, 2021, 4,805 members.


### Can we disallow the box trait on members in 1.0 right now?

Not backward compatibly for AWS. It's used on 271 members in AWS services. This
includes the following services: acmpca, appconfig, appmesh (Smithy-based),
codestar, codestarnotifications, cognitoidentity, cognitoidentityprovider,
connect, connectparticipant, costexplorer, glue, imagebuilder, iot,
iotanalytics, iotsecuretunneling, pricing, quicksight, route53resolver,
s3control, sagemaker, sagemakera2iruntime, secretsmanager,
servicecatalogappregistry, sfn, ssm, timestreamwrite.


### How often has the `@required` trait been removed from members in AWS?

The required trait has been removed from a structure member 618 different times
across 105 different services. Of those removals, 75 targeted structures, 350
targeted strings, and the remainder targeted lists, maps, and other simple
shapes.


### How often has the `@required` trait been added to members in AWS?

The required trait has been added to a member 9 different times across 5
different services.
