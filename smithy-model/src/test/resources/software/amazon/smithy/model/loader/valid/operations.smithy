namespace com.foo

// All of these operations are valid.
operation A()
operation B() errors [Error1]
operation C() errors [Error1, Error2]
operation D(Input) errors [Error1, Error2]
operation E (Input) errors[Error1, Error2]
operation F (Input) -> Output errors[Error1, Error2]
operation G (Input) -> Output

operation H (Input)
    -> Output

operation I // Add lots of interspersed comments...
// Comment
(Input)// Comment
// Comment
    ->    // Comment
// Comment
    Output // Comment
// Comment
operation
J
(Input)
->
Output
errors
[
Error1
,
  Error2
]

operation K() errors []

structure Input {}
structure Output {}

@error(client)
structure Error1 {}

@error(client)
structure Error2 {}
