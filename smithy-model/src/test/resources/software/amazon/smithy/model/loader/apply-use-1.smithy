namespace smithy.example

use smithy.example.b#baz

// This applies smithy.example#bar to smithy.example#Foo
apply Foo @bar("hi")

// This applies smithy.example.b#baz to smithy.example#Foo
apply Foo @baz("hi")

// This applies smithy.api#deprecated to smithy.example#Foo
apply Foo @deprecated

// This applies smithy.api#sensitive to smithy.example#Foo
apply Foo @smithy.api#sensitive
