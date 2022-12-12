namespace example.namespace

@trait
structure customTrait {

}

// Overrides the prelude documentation shape.
@trait(selector: "*")
string documentation

@trait
integer numeric

@customTrait
@documentation("foo")
string MyString1

@example.namespace#customTrait
@smithy.api#documentation("foo")
string MyString2

@smithy.api#documentation("foo")
@documentation("baz")
string MyString3

@numeric(10)
string MyString4

