namespace smithy.example

use smithy.example.b#baz

// This applies smithy.example#bar to smithy.example#Foo
apply Foo @bar("hi")

// This applies smithy.example.b#baz to smithy.example#Foo
apply Foo @baz("hi")
