$version: "2.0"

namespace smithy.example

use aws.iam#iamAction

@iamAction(name: "foo"
    documentation: "docs"
    relativeDocumentation: "page.html#actions"
    requiredActions: ["iam:PassRole", "ec2:RunInstances"]
    resources: {
        required: {
            "bar": {
                conditionKeys: ["foo:asdf"]
            }
            "bap": {
                conditionKeys: ["foo:zxcv", "foo:hjkl"]
            }
        }
        optional: {
            "baz": {}
        }
    }
    createsResources: ["kettle"]
)
operation Foo {}
