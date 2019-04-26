# Smithy AWS core traits

Provides traits and validators that are used by most AWS services.

See the Smithy specification for details on how these traits are used.

## `aws.api` Traits

* `aws.api#service`: Configures a Smithy service as an AWS service.
* `aws.api#arn`: Defines the ARN template of a Smithy resource.
* `aws.api#arnReference`: Indicates that a string shape contains an ARN.
* `aws.api#unsignedPayload`: Configures that request payload of an
  operation as unsigned.

## `aws.iam` Traits

* `aws.iam#actionPermissionDescription`: Defines the description of what
  providing access to an operation entails.
* `aws.iam#conditionKeys`: Applies condition keys to an operation or resource.
* `aws.iam#defineConditionKeys`: Defines the condition keys used in
  a service.
* `aws.iam#disableConditionKeyInference`: Disables the automatic inference of
  condition keys of a resource.
* `aws.iam#requiredActions`: Defines the actions that a principal must be
  authorized to invoke in addition to the targeted operation order to invoke
  an operation

## Example usage

Example usage:

```smithy
$version:1.0
namespace ns.foo

@aws.api#service(sdkId: "Some Value")
service SomeService {
    version: "2018-03-17",
    resources: [SomeResource, RootArnResource, AbsoluteResource],
}

// This resource has an ARN, but no identifier.
@aws.api#arn(template: "rootArnResource")
resource RootArnResource {}

// This resource has an ARN and MUST provide a placeholder for its
// identifier.
@aws.api#arn(template: "someresource/{someId}")
resource SomeResource {
    identifiers: {
        someId: SomeResourceId,
        childId: ChildResourceId,
    },
    resources: [ChildResource],
}

// This resource has an ARN and MUST provide placeholders for all of its
// identifiers. This relative ARN does not include a region or account ID.
@aws.api#arn(
        template: "someresource/{someId}/{childId}",
        noRegion: true,
        noAccount: true)
resource ChildResource {
    identifiers: {
        someId: SomeResourceId,
        childId: ChildResourceId,
    },
    resources: [AnotherChild],
}

resource AnotherChild {
    identifiers: {
        someId: SomeResourceId,
        childId: ChildResourceId,
    },
}

// This resource uses an ARN as its identifier, so its ARN template is absolute.
@aws.api#arn(
        template: "{arn}",
        absolute: true)
resource AbsoluteResource {
    identifiers: {
        arn: AbsoluteResourceArn
    },
}

@aws.api#arnReference(
        type: "AWS::SomeService::AbsoluteResource",
        service: 'ns.foo#SomeService',
        resource: 'ns.foo#AbsoluteResource')
string AbsoluteResourceArn

string SomeResourceId
string ChildResourceId
```
