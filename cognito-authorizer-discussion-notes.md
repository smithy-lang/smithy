# Cognito Provider ARNs: `@cognitoUserPools` vs `@authorizers`

## The Problem

Two ways to specify Cognito provider ARNs in Smithy today. Both produce the same `x-amazon-apigateway-authorizer` output:

1. `aws.auth#cognitoUserPools` puts provider ARNs in the auth scheme trait
2. `aws.apigateway#authorizers` puts them on the authorizer definition (our PR #3085)

Option 2 is better separation of concerns. The auth trait should declare *what* auth mechanism a service uses. The authorizer definition should configure *how* API Gateway sets it up. That's where `type`, `uri`, `credentials` already live, and `providerARNs` fits naturally alongside them.

`cognitoUserPools` mixes both: it says "use Cognito" and "here are the pool ARNs for the API Gateway authorizer" in the same place. Auth scheme selection and deployment config end up coupled.

---

## Option 1: `aws.auth#cognitoUserPools`

```smithy
@cognitoUserPools(
    providerArns: ["arn:aws:cognito-idp:us-east-1:123:userpool/123"])
service FooBaz { ... }
```

- `@authDefinition` trait, participates in auth scheme resolution
- `providerArns` is required, can't use the trait without it
- `CognitoUserPoolsConverter` hardcodes `type: "cognito_user_pools"`, reads ARNs from trait, emits the full extension

---

## Option 2: `aws.apigateway#authorizers` with `providerARNs`

```smithy
@cognitoUserPools(providerArns: ["arn:placeholder"])  // still needed for auth scheme
@authorizer("my-cognito")
@authorizers("my-cognito": {
    scheme: cognitoUserPools
    type: "cognito_user_pools"
    providerARNs: ["arn:aws:cognito-idp:us-east-1:123:userpool/real-pool"]
})
service Weather { ... }
```

- `providerARNs` is optional, only add when needed
- All API Gateway config fields live together: `type`, `uri`, `credentials`, `providerARNs`
- Supports named/multiple authorizers
- `AddAuthorizers` mapper overlays extension fields onto the base security scheme

---

## What API Gateway Actually Expects

From the REST API and CloudFormation specs, `ProviderARNs` lives alongside `AuthorizerUri`, `AuthorizerCredentials`, and `Type`. All deployment config. This matches option 2.

---

## The Overlap Problem

When both traits are applied with different ARNs:
- `CognitoUserPoolsConverter` emits scheme with ARNs from `@cognitoUserPools`
- `AddAuthorizers` creates a new named scheme with ARNs from `@authorizers`
- `RemoveUnusedComponents` deletes the original
- Result: `@authorizers` wins silently. The required `providerArns` on `@cognitoUserPools` was dead weight.

---

## Recommendation

`@authorizers` is the right place for `providerARNs`. We should deprecate `@cognitoUserPools` carrying ARNs.

Why:
1. API Gateway's own data model puts `ProviderARNs` next to `AuthorizerUri`, `Credentials`, `Type`. It's deployment config.
2. SigV4 (the other `@authDefinition`) doesn't carry deployment config. Only `name`. `cognitoUserPools` is the odd one out.
3. `@authorizers` already handles every other authorizer config field. `providerARNs` just fills the gap.
4. Using both traits is a footgun. `providerArns` on `@cognitoUserPools` is required but silently ignored when `@authorizers` is present.

Plan:
1. Ship PR #3085 as-is.
2. Document `@authorizers` as the canonical way to configure Cognito authorizers.
3. Add a conflict validator: WARNING when both traits have different ARN values.
4. Deprecate `providerArns` on `@cognitoUserPools`. Future major version makes it annotation-only.
5. No breakage for existing models. If no `@authorizers` is present, `CognitoUserPoolsConverter` keeps working.

---

## Questions for the Smithy Team

1. Are we good with deprecating `providerArns` on `@cognitoUserPools`? Timeline?
2. Should `@cognitoUserPools` become annotation-only in the next major, or do we introduce a new `@cognitoAuth` annotation and deprecate the whole thing?
3. Should `AddAuthorizers` explicitly override `CognitoUserPoolsConverter` output when both are present, or is the current cleanup behavior acceptable?
4. Any case where someone would intentionally want different ARNs in `@cognitoUserPools` vs `@authorizers`?
