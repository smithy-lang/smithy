$version: "2.0"

namespace smithy.example

use aws.iam#actionName
use aws.iam#actionPermissionDescription
use aws.iam#iamAction
use aws.iam#requiredActions

@iamAction(
    name: "foo"
    documentation: "docs"
    requiredActions: ["foo:Bar"]
)
@actionName("foo")
@actionPermissionDescription("docs")
@requiredActions(["foo:Bar"])
operation Foo {}
