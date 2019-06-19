namespace example.namespace

trait customTrait {}

// Overrides the prelude documentation shape.
trait documentation {
  selector: "*",
  shape: smithy.api#String,
}

// Uses a forward reference to a prelude shape.
trait numeric {
  selector: "*",
  shape: Integer,
}

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
