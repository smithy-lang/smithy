$version: "2.0"

 namespace smithy.example

 structure TestShape {
     /// foo
     @required
     foo: Boolean = false
     /// bar
     @required
     bar: MyString = "bar"

     /// baz
     @required
     baz: Integer

     /// This doc comment will be dropped
 }

string MyString
