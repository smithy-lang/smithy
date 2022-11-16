$version: "2.0"

namespace smithy.example

service FooService {
  version: "2017-02-11"
  operations: [GetFoo PublishMessages ]
}

operation PublishMessages {
    input: PublishMessagesInput
}

@input
structure PublishMessagesInput {
    room: String,
    messages: PublishEvents,
}

@streaming
union PublishEvents {
    message: Message,
    leave: LeaveEvent,
}

structure Message {
    message: String,
}

structure LeaveEvent {}


operation GetFoo {
  input: FooStructInput
  output: FooStructOutput
}

enum FooEnum {
  @tags(["alpha"])
  FOO
  @tags(["beta"])
  BAR
  @tags(["gamma"])
  BAZ
}

union FooUnion {
  fooString: String
  fooInteger: Integer
  fooEnum: FooEnum
}

@range(min: 10, max: 30)
integer FooInteger

structure FooStructInput {
  stringVal: String
  intVal: Integer
}

structure FooStruct {
  stringVal: String
  intVal: Integer
  unionVal: FooUnion
}

structure FooStructOutput {
  stringVal: String
  intVal: FooInteger
  unionVal: FooUnion
}

service BarService {
  version: "2017-02-11"
  operations: [GetBar]
}

operation GetBar {
  input: BarStructInput
  output: BarStructOutput
}

enum BarEnum {
  @tags(["alpha"])
  FOO
  @tags(["beta"])
  BAR
  @tags(["gamma"])
  BAZ
}

union BarUnion {
  fooString: String
  fooInteger: Integer
  fooEnum: BarEnum
}

@range(min: 10, max: 30)
integer BarInteger

structure BarStructInput {
  stringVal: String
  intVal: Integer
}

structure BarStruct {
  stringVal: String
  intVal: Integer
  unionVal: BarUnion
}

structure BarStructOutput {
  stringVal: String
  intVal: BarInteger
  unionVal: BarUnion
}
