# Resource properties

This document proposes the addition of “properties” to Smithy resource shapes.

## Motivation

Smithy allows service teams to define resources, but does not provide any
validation or assurances of consistency of resource properties across operations
bound to a resource. The only consistency enforced by Smithy is that the
identifiers of a resource are present on the input of each instance operation
bound to the resource. Because of this, it’s very easy for a service team to
call a property “ids” in one operation and “idList” in another, despite these
members referring to the same thing. Modeling resource properties in Smithy
ensures a better developer experience and enables a clearer view of resource
structure when performing model transformations.

## Proposal

Resource shapes will support a new member named “properties” that defines the
properties that can be referred to in the top-level input and output shapes of
a resource’s instance operations and create operation.

```
resource Config {
    identifiers: {
        configId: ConfigId
    }
    properties: {
        configData: ConfigData
        name: ConfigName
        tags: TagList
        configArn: ConfigArg
        configType: ConfigType
    }
    create: CreateConfig
}
```

The following `create` operation for the resource is acceptable because it only
uses allowed property definitions:

```
operation CreateConfig {
    input := {
        @required
        name: ConfigName

        @required
        configData: ConfigData

        tags: TagList
    }
    output := {
        @required
        configArn: ConfigArn

        @required
        configId: ConfigId

        @required
        configType: ConfigType
    }
}
```

Introduced and changed validation:

* Identifiers of a resource MUST NOT be redefined in the properties of a resource.
* Top-level members of the input and output of resource instance operations MUST
  only use properties that resolve to declared resource properties except for
  members marked with the `@notProperty` trait or marked with traits marked
  with the `@notProperty` trait.
* Defined resource properties that do not resolve to any top-level input or
  output members are invalid.
* `@nestedProperties` trait pushes down the alignment of top-level properties
  to a member target shape from the top-level input or output shape. Members
  of the target shape can not use `@notProperty`.
* Members that provide a value for a resource property but use a different
  target shape are invalid.
* Members marked with a `@property` trait using a `name` that does not map to a
  declared resource property are invalid.
* @resourceIdentifier trait can also be applied to top level output shape members
    * Implicit ID binding behavior now applies to output shapes

### Excluding properties

Resource operations sometimes require inputs or outputs that aren’t resource
properties, but rather part of the framing of an API call. For example, the
following operation would fail validation because the `dryRun` property is not
a resource property:

```
operation UpdateConfig {
    input := {
        @required
        configId: ConfigId

        configData: ConfigData

        dryRun: IsDryRun
        // ^ ERROR: 'dryRun' is an undeclared resource property of Config.
    }
}
```

Top-level properties of operations that aren’t part of the resource but are
required to be sent by the client or server can be marked as `@notProperty`.
The above operation can be corrected by marking `dryRun` as `@notProperty`:

```
operation UpdateConfig {
    input := {
        @required
        configId: ConfigId

        configData: ConfigData

        @notProperty
        dryRun: IsDryRun
    }
}
```

#### `@notProperty` trait definition

The `@notProperty` trait is defined in Smithy’s prelude as:

```
@trait(selector: ":is(structure > member, [trait|trait])")
structure notProperty {}
```

#### `@notProperty` as a meta-trait

There are already many trait requirements in Smithy leading to verbose models
definitions, and having to mark every non-property member used in the inputs
and outputs of a resource with `@notProperty` makes the problem worse. Traits
like `@idempotencyToken` *already* communicate that the member they target is
not part of the resource. The `@notProperty` trait can be applied to traits,
making the applied trait cause members to be ignored as resource properties.
For example, consider the following trait definition:

```
@trait
@notProperty
structure idempotencyToken {}
```

Consider the following model that uses the above trait:

```
structure Foo {
    @idempotencyToken
    token: String
}
```

Even though `token` is not a resource property, the `token` member is now allowed
because `@idempotencyToken` is marked with the `@notProperty` trait.

#### Resource properties marked as `@notProperty`

Members marked with `@notProperty` carrying traits can still be used as resource
properties. Anything declared in the `properties` of a resource are always
considered resource properties regardless of if a trait marked with the
`@notProperty` trait is defined on a corresponding member.

In the following resource definition,`token` is a resource property of
`Tokenator`. Despite the `token` member of `CreateTokenator` being marked with
the `@idempotencyToken` trait, the token member is still considered a resource
property.

```
resource Tokenator {
    identifiers: {
        tokenName: TokenName
    }
    properties: {
        token: Token
    }
    create: CreateTokenator
}

operation CreateTokenator {
    input := {
        @idempotencyToken
        token: Token
    }
}
```

Members marked *directly* with the `@notProperty` trait that provide a value
for a corresponding resource property are invalid. The following `CreateTokenator`
operation would model will cause a validation error:

```
operation CreateTokenator {
    input := {
        // ERROR: Cannot mark a resource property with the notProperty trait
        @idempotencyToken
        @notProperty
        token: Token
    }
}
```

### Remapping input and output members to properties

Models that already released with discrepancies across properties in their
resources can associate a top-level input or output member of a resource
operation with a named property using the `@property` trait.

```
structure CreateConfigOutput {
    @required
    configArn: ConfigArn

    @required
    @property(name: "configId")
    configurationId: ConfigId

    @required
    configType: ConfigType
}
```

The property trait contains a single member, “name”, that is used to map the
member name to a property of the resource. When resolving the properties of the
`Config` object, `configurationId` is mapped to the `configId` property.

#### `@property` trait definition

The `@property` trait is defined in Smithy’s prelude as:

```
@trait(selector: "structure > member")
structure property {
    @required
    name: String
}
```

## Addressing properties root misalignment with operation input/output

#### `@nestedProperties` trait definition

The `@nestedProperties` trait is defined in Smithy’s prelude as:

```
@trait(
    selector: "structure > member :test(> structure)",
    structurallyExclusive: "member"
)
structure nestedProperties { }
```

It “pushes down” the mapping of resource properties into the member’s target
structure as illustrated below.

```
resource Pipeline {
...
     properties: {
        name: String
        rank: Integer
    },
    create: CreatePipeline
    ...
}

operation CreatePipeline {
    input := CreatePipelineInput
}

structure CreatePipelineInput {
    @nestedProperties
    pipeline: PipelineDescription
}

structure PipelineDescription {
    name: String
    foo: Baz
    ranking: Integer
 }
```

#### @nestedProperties added validation

* Trait can only be applied to members of a top-level member of a resource
  instance operation or create operation.
* All other top-level input/output members must have @notProperty trait directly
  or indirectly applied. For example @resourceIdentifier, @idempotencyToken)
* Member must target a structure shape, and members of that structure shape may
  not use @notProperty

## FAQ

### Why do we need to do this?

Resources released using Smithy often use inconsistent names across operation
inputs and outputs. This is confusing for customers, makes it impossible for
CloudFormation to take existing Smithy models and generate resource schemas,
and it requires work for CloudControl to implement JSON Patch updates to resources.

### Why do we need to support remapping?

1. Service teams that already shipped with inconsistent resource properties
   might want to add more rigor into their modeling going forward, so they need
   a way to address existing inconsistencies.
2. CloudFormation wants to support existing APIs that already shipped with
   inconsistent property naming across resources, so they need a way to map the
   existing service API to the declared properties of a resource.

### Why allow `@notProperty` to be a meta trait?

It makes models less verbose because traits like `@idempotencyToken` already
imply that they’re transient.

### What if a member for a resource property is marked with the `@notProperty`
trait directly?

Marking a resource property member directly with the `@notProperty` trait is an
error.

### What is the relationship to existing CloudFormation traits?

A good outcome is that new services only need to apply `@notProperty` traits.
`@property`, `@cfnExcludeProperty`, and `@cfnName` should only be needed to map
existing services to Smithy.

1. While the `@cfnExcludeProperty` trait has some overlap with `@notProperty`,
   they do have different use cases.
    1. Top-level members of the input or output of an operation marked with
       `@cfnExclude` or `@notProperty` will both be removed from the output of
       converting Smithy to a CloudFormation schema.
    2. `@notProperty` only has an effect on top-level resource properties, whereas
       `@cfnExcludeProperty` works on shapes nested in the input, output, or errors
       of an operation.
    3. It's possible that something is a resource property that isn't transient
       and is also not supported in CloudFormation (for example, not yet supported
       in CloudFormation, or an existing CloudFormation schema already shipped
       with a schema that differs from the API).
2. `@property` and `@cfnName` are also similar, but have different uses:
    1. `@property` is used to map a top-level member name of the *API* to a
       resource property name, whereas `@cfnName` is used to map a member name
       to a CloudFormation schema property name.
    2. New schemas SHOULD use the same name in both the API and CloudFormation.
       A DANGER event will be emitted when these two traits differ.
    3. `@property` is used in the Smithy to CloudFormation conversion, but
       `@cfnName` takes precedence over `@property`. `@property` and `@cfnName`
       set on the same member to the same value will emit a WARNING that it’s
       unnecessary.
    4. `@property` only affects top-level input, output, and error members while
    `@cfnName` can alter nested properties.

### Should tags be modeled through resource properties?

Yes. However, normal property constraints apply and tags must appear in at least
one resource lifecycle operation’s input or output.

## Delivery

**Property consistency:**

* New properties field in resource definition
* @notProperty trait (previously @transient)
* Property validation at build time
* @nestedProperties to support top level request/response members not lining up with resource properties
* Implicit and explicit identifier bindings (@resourceIdentifier) added for operation output
* Design intent/approach for tag property and instance operation modeling, ensuring backwards compatibility

## Alternative approaches

### (`@notProperty` alternative) Add `ignoredProperties` to resource

Rather than define the `@notProperty` trait on members, it could be defined on
the resource directly:

```
resource Config {
    identifiers: {
        configId: ConfigId
    }
    properties: {
        configData: ConfigData
        name: ConfigName
        tags: TagList
        configArn: ConfigArg
        configType: ConfigType
    }
    ignoredProperties: {
        token: String
    }
    create: CreateConfig
}
```

This could be made more succinct with mixins:

```
@mixin
resource BaseResourceMixin {
    ignoredProperties: {
        token: String
    }
}

resource Config with [BaseResourceMixin] {
    identifiers: {
        configId: ConfigId
    }
    properties: {
        configData: ConfigData
        name: ConfigName
        tags: TagList
        configArn: ConfigArg
        configType: ConfigType
    }
    create: CreateConfig
}
```

Pros:

* Remove the need to add property traits to members spread across the resource.

Cons:

* The resource shape becomes verbose. It leaks details of create and instance
  operation input and output members that need to be filtered out.
* More difficult to process ignoredProperties when model undergoes transformations
  that may filter the operations on a service or resource. Operation inclusion
  or exclusion would impact the contents of ignoredProperties.

### (`@nestedProperties` alternative): Associating top level resource properties to structures with `@associatedResource`

In order to support existing services migration to resource properties we could
introduce a new trait named `@associatedResource` to associate a structure shape
with the top level properties of a specific resource instead of a top level
lifecycle operation input or output. This trait can only be allowed to structure
shapes one below the input or output of a resource lifecycle operation.

```
resource Pipeline {
...
     properties: {
        name: String
        foo: Baz
        rank: Integer
    },
    create: CreatePipeline
    ...
}

operation CreatePipeline {
    input := CreatePipelineInput
}

structure CreatePipelineInput {
     pipeline: PipelineDescription
}

@associatedResource("Pipeline")
structure PipelineDescription {
     name: String
    foo: Baz
    @property(name:"rank")
    ranking: Integer
 }
```

#### `@associatedResource` trait definition

The `@associatedResource` trait is defined in Smithy’s prelude as:

```
@trait(selector: "structure") //TODO: How to force off of top level input/output
string associatedResource
```

Pros:

* Directly associates the resource with the shape to find the properties
  regardless of how deeply nested it is.

Cons:

* Adds a lot of complexity around finding and validating properties when they
  can be sourced at multiple levels of an input or output shape.
* Decreases readability of resource properties overall. Readers must examine
  all members to understand the mapping structure.
* Should not allow nesting of properties beyond 1 level deep.
* Service author may incidentally associate more than one resource in a input
  or output shape hierarchy.
