$version: "2.0"

namespace smithy.example

use aws.cloudformation#cfnResource

@cfnResource(primaryIdentifier: "fooArn")
resource FooResource {
    identifiers: {
        fooId: String
    }
    properties: {
        fooArn: String
    }
    create: CreateFoo
    read: GetFoo
}

operation CreateFoo {
    input := {}
    output := {
        @required
        fooId: String
        fooArn: String
    }
}

@readonly
operation GetFoo {
    input := {
        @required
        fooId: String
    }
    output := {
        @required
        fooId: String
        fooArn: String
    }
}

@cfnResource(primaryIdentifier: "barArn")
resource BarResource {
    identifiers: {
        barId: String
    }
    create: CreateBar
    read: GetBar
}

operation CreateBar {
    input := {}
    output := {
        @required
        barId: String
        barArn: String
    }
}

@readonly
operation GetBar {
    input := {
        @required
        barId: String
    }
    output := {
        @required
        barId: String
        barArn: String
    }
}

@cfnResource(primaryIdentifier: "bazArn")
resource BazResource {
    identifiers: {
        bazId: String
    }
    properties: {
        bazArn: Integer
    }
    create: CreateBaz
    read: GetBaz
}

operation CreateBaz {
    input := {}
    output := {
        @required
        bazId: String
        bazArn: Integer
    }
}

@readonly
operation GetBaz {
    input := {
        @required
        bazId: String
    }
    output := {
        @required
        bazId: String
        bazArn: Integer
    }
}
