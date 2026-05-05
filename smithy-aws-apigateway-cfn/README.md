# Smithy AWS API Gateway CloudFormation JSON Plugin

This module provides a `smithy-build` plugin that serializes a Smithy model to
JSON AST with CloudFormation `Fn::Sub` intrinsic function wrapping. The output
is intended for use as the `Body` property of an `AWS::ApiGateway::RestApi`
CloudFormation resource, enabling direct Smithy model import without OpenAPI
conversion.

## Usage

Add the following to your `smithy-build.json`:

```json
{
    "version": "1.0",
    "plugins": {
        "smithy-cfn-json": {
            "service": "com.example#MyService"
        }
    }
}
```

### Configuration

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `service` | ShapeId | Yes | — | The service shape to export |
| `disableCloudFormationSubstitution` | boolean | No | `false` | Disable `Fn::Sub` wrapping |

### Output

The plugin writes `{ServiceName}.smithy.json` to the build output directory.

## CloudFormation Substitution

String values containing `${...}` variable syntax at the following trait paths
are automatically wrapped in `{"Fn::Sub": "..."}` objects:

- `aws.apigateway#integration` → `uri`, `credentials`, `connectionId`, `integrationTarget`
- `aws.apigateway#authorizers` → `*/uri`, `*/credentials`
- `aws.auth#cognitoUserPools` → `providerArns/*`

### Example

Input (Smithy IDL):
```smithy
@integration(
    type: "aws_proxy"
    uri: "${MyLambdaFunction.Arn}"
    httpMethod: "POST"
    credentials: "${ApiGatewayRole.Arn}"
)
```

Output (in the generated JSON AST):
```json
"aws.apigateway#integration": {
    "type": "aws_proxy",
    "uri": {"Fn::Sub": "${MyLambdaFunction.Arn}"},
    "httpMethod": "POST",
    "credentials": {"Fn::Sub": "${ApiGatewayRole.Arn}"}
}
```

CloudFormation resolves `Fn::Sub` at deploy time before passing the body to
the API Gateway SmithyImporter.
