------------------------
Using the Semantic Model
------------------------

The Java reference implementation of Smithy provides various
abstractions to interact with the in-memory semantic model. This
document provides a kind of "cookbook" for achieving various tasks with
the Smithy model.


Traversing the model
====================

Each of the following examples assume a variable named ``model`` is
defined that is a ``software.amazon.smithy.model.Model``.


Iterate over all shapes
-----------------------

``Model#toSet`` is a cheap operation that just provides a ``Set``
view over a model.

.. code-block:: java

    for (Shape shape : model.toSet()) {
        // ...
    }


Iterate over all shapes of a specific type
------------------------------------------

Each type of shape in Smithy has a dedicated ``Model#getXShapes``
method. These methods are cheap to invoke. They just provide a
filtered ``Set`` view over a model.

.. code-block:: java

    for (ServiceShape shape : model.getServiceShapes()) {
        // ...
    }

    for (StructureShape shape : model.getStructureShapes()) {
        // ...
    }

    for (MemberShape shape : model.getMemberShapes()) {
        // ...
    }

    // etc...


Iterate over all shapes with a specific trait
---------------------------------------------

``Model#getShapesWithTrait`` returns shapes that have a specific trait.
This is a cheap method to call and uses caches internally. The provided
trait class can be retrieved from each returned shape. The following
example uses ``DeprecatedTrait`` but any trait class can be used.

.. code-block:: java

    for (Shape shape : model.getShapesWithTrait(DeprecatedTrait.class)) {
        DeprecatedTrait trait = shape.expectTrait(DeprecatedTrait.class);
    }


Iterate over shapes of a specific type with a specific trait
------------------------------------------------------------

``Model#getXShapesWithTrait`` returns shapes of type ``X`` that have a
specific trait. Each type of shape has a dedicated ``Model#getXShapesWithTrait``
method. This is a cheap method to call and uses caches internally.
The provided trait class can be retrieved from each returned shape. The
following example uses ``SensitiveTrait`` but any trait class can be used.

.. code-block:: java

    for (StructureShape shape : model.getStructureShapesWithTrait(SensitiveTrait.class)) {
        SensitiveTrait trait = shape.expectTrait(SensitiveTrait.class);
    }

    for (StringShape shape : model.getStringShapesWithTrait(SensitiveTrait.class)) {
        SensitiveTrait trait = shape.expectTrait(SensitiveTrait.class);
    }

    // etc..


Stream over all shapes
----------------------

.. code-block:: java

    Stream<StringShape> strings = model.shapes(StringShape.class)
        .filter(shape -> shape.getId().getNamespace().equals("foo.bar"));

.. tip::

    In general, prefer the named methods that convert ``Model`` to a set.
    However, it's sometimes useful to break down complicated pipeline
    style transformations into streams.


Traversing the members of a shape
---------------------------------

.. code-block:: java

    StructureShape struct;

    for (MemberShape member : struct.members()) {
        // Get the shape targeted by the member.
        Shape target = model.expectShape(member.getTarget());
        System.out.println(member.getMemberName() + " targets " + target);

        // Get that container of the member.
        Shape container = model.expectShape(member.getContainer());
    }

.. note::

   - Members are ordered based on the order given in the Smithy model
   - You can order the members differently if needed (for example sorting
     them using a ``TreeMap``).
   - The above code works the same way for any shape, whether it's a
     structure, union, list, or map.
   - By the time a code generator is running, the model has been
     thoroughly validated. You should use the various methods that start
     with ``expect`` to more easily interact with shapes.


Visiting shapes
---------------

Smithy often relies on *visitors* to dispatch to different typed methods
for handling different kinds of shapes.

.. code-block:: java

    // Silly example that returns the numbers of members a shape has.
    ShapeVisitor<Integer> visitor = new ShapeVisitor.Default<Integer>() {
        @Override
        protected Integer getDefault(Shape shape) {
            return 0;
        }

        @Override
        public Integer listShape(ListShape shape) {
            return 1;
        }

        @Override
        public Integer mapShape(MapShape shape) {
            return 2;
        }

        @Override
        public Integer structureShape(StructureShape shape) {
            return shape.members().size();
        }

        @Override
        public Integer unionShape(UnionShape shape) {
            return shape.members().size();
        }
    };

    StringShape string = exampleThatGetsString();
    int count = string.accept(visitor);
    assert(count == 0);

.. note::

   - The ``accept`` method of a shape is used to apply a visitor to the
     shape.
   - You should typically use the ``Visitor.Default`` implementation to
     implement a visitor.
   - A simpler way to get the answer of the above example is to just call
     ``shape.members().size()``.


Knowledge Indexes
=================

Smithy provides various knowledge index implementations that are used to
break down more complex tasks into easily queried, pre-computed data
stores. These knowledge indexes are also cached on a ``Model`` object,
making them cheaper to use than recomputing information multiple times
across things like validators.


Get every operation in a service or resource
--------------------------------------------

Service shapes can contain resources which can contain operations.
``TopDownIndex`` will walk the service/resource to find all contained
operations.

.. code-block:: java

    TopDownIndex index = TopDownIndex.of(model);
    index.getContainedOperations(serviceShape);


Get every resource in a service or resource
-------------------------------------------

Service shapes can contain resources which can themselves contain
resources. ``TopDownIndex`` will walk the service/resource to find
all contained operations.

.. code-block:: java

    TopDownIndex index = TopDownIndex.of(model);
    index.getContainedResources(serviceShape);


Determine if a member is nullable
---------------------------------

Taking the version of the Smithy IDL into account when computing
the nullability of a member can be complex. ``NullableIndex``
hides all of this complexity by providing a simple boolean result
for a given member shape.

.. code-block:: java

    NullableIndex index = NullableIndex.of(model);

    if (index.isMemberNullable(someMemberShape)) {
        // nullable
    }


Get pagination information about an operation
---------------------------------------------

Resolving information about paginated operations in Smithy requires
some bookkeeping. ``PaginatedIndex`` tries to consolidate all the
information you might need when interacting with paginated traits.

.. code-block:: java

    PaginatedIndex index = PaginatedIndex.of(model);

    index.getPaginationInfo(service, operation).ifPresenet(info -> {
        // method invoked if the operation is paginated.
        System.out.println("Service shape: " + info.getService());
        System.out.println("Operation shape: " + info.getOperation());
        System.out.println("Input shape: " + info.getInput());
        System.out.println("Output shape: " + info.getOutput());
        System.out.println("Paginated trait: " + info.getPaginatedTrait());
        System.out.println("Input token member: " + info.getInputTokenMember());
        System.out.println("Output token membber: " + info.getOutputTokenMemberPath());
        // etc...
    });


Get the HTTP binding response status code of an operation
---------------------------------------------------------

The ``HttpBindingIndex`` can provide all kinds of information about
the HTTP bindings of an operation, including the response status
code.

.. code-block:: java

    HttpBindingIndex index = HttpBindingIndex.of(model);
    int code = index.getResponseCode(operationShape);


Get the request content-type of an operation
--------------------------------------------

``HttpBindingIndex`` can attempt to resolve the Content-Type header
of a request. The content-type might not be statically known by
the model and might rely on protocol-specific information.

.. code-block:: java

    HttpBindingIndex index = HttpBindingIndex.of(model);

    String defaultPayloadType = "application/json";
    String eventStreamType = "application/vnd.amazon.eventstream");
    String contentType = index
        .determineRequestContentType(operation, defaultPayloadType, eventStreamType)
        .orElseNull();


Get the response content-type of an operation
---------------------------------------------

``HttpBindingIndex`` can attempt to resolve the Content-Type header
of a response. The content-type might not be statically known by
the model and might rely on protocol-specific information.

.. code-block:: java

   HttpBindingIndex index = HttpBindingIndex.of(model);

   String defaultPayloadType = "application/json";
   String eventStreamType = "application/vnd.amazon.eventstream");
   String contentType = index
       .determineResponseContentType(operation, defaultPayloadType, eventStreamType)
       .orElseNull();


Get HTTP binding information of an operation
--------------------------------------------

.. code-block:: java

   HttpBindingIndex index = HttpBindingIndex.of(model);
   var requestBindings = index.getRequestBindings(operationShape);
   var responseBindings = index.getResponseBindings(operationShape);

   // This loop works the same way for request or response bindings.
   for (var entry : requestBindings.entrySet()) {
       String memberName = entry.getKey();
       HttpBinding binding = entry.getValue();
       System.out.println("Member: " + memberName);
       System.out.println("Member shape: " + binding.getMember());
       System.out.println("Location: " + binding.getLocation());
       System.out.println("Location name: " + binding.getLocationName());
       binding.getBindingTrait().ifPresent(trait -> {
           System.out.println("Binding trait: " + trait);
       });
   }


Get the timestamp format used for a specific HTTP binding
---------------------------------------------------------

.. code-block:: java

   // Determine the format used for members bound to HTTP labels.
   HttpBindingIndex index = HttpBindingIndex.of(model);
   var formatUsedInPayloads = TimestampFormatTrait.Format.EPOCH_SECONDS;
   var format = index.determineTimestampFormat(
       member, HttpBinding.Location.LABEL, formatUsedInPayloads);


Get members that have specific HTTP bindings
--------------------------------------------

.. code-block:: java

   // Find every member in the input of the operation bound to an HTTP label.
   HttpBindingIndex index = HttpBindingIndex.of(model);
   var locationTypeToFind = HttpBinding.Location.LABEL;
   var result = index.getRequestBindings(operation, locationTypeToFind);


.. _codegen-transforming-the-model:

Transforming the model
======================

It's often necessary to transform a Smithy model prior to code
generation. For example, you might need to remove operations that use
unsupported features, remove shapes that aren't in the closure of a
service, or add traits to shapes that are specific to your code
generator. Smithy provides a model transformation abstraction in
``ModelTransformer``. ``ModelTransformer`` provides various methods for
transforming a model, some of which are documented below.


Remove deprecated operations
----------------------------

``ModelTransformer`` will remove any broken relationships when a
shape is removed. If you remove an operation from the model, it's
removed from any service or resource.

.. code-block:: java

    model = ModelTransformer.create().removeShapesIf(shape -> {
        return shape.isOperationShape() && shape.hasTrait(DeprecatedTrait.class);
    )};


Add a trait to every shape
--------------------------

.. code-block:: java

    model = ModelTransformer.create().mapShapes(shape -> {
        return Shape.shapeToBuilder(shape).addTrait(new MyCustomTrait()).build();
    });

.. tip::

    You can convert any shape to a builder using the static method
    ``Shape#shapeToBuilder``


.. _codegen-flattening-mixins:

Flattening mixins
-----------------

Mixins are used to share shape definitions across a model. They're
essentially build-time copy and paste, and they have no meaningful
impact on generated code. For example, the following model uses mixins:

.. code-block:: smithy

    @mixin
    structure HasUsername {
        @required
        username: String
    }

    structure UserData with [HasUserName] {
        isAdmin: Boolean
    }

Code generators should flatten mixins out of a model before generating
code, allowing them to more easily generate code without needing to
implement special handling for mixins. This can be done using a Smithy
model transformation:

.. code-block:: java

    ModelTransformer transformer = ModelTransformer.create();
    Model transformedModel = transformer.flattenAndRemoveMixins(model);

After flattening mixins, the above model is equivalent to:

.. code-block:: smithy

    structure UserData with [HasUserName] {
        @required
        username: String

        isAdmin: Boolean
    }


.. _codegen-copying-errors-to-service:

Copying service errors to operation errors
------------------------------------------

Service shapes can define a set of errors that can be returned from any
operation. While this is great for modeling a service, it makes code
generation harder.

For example:

.. code-block:: smithy

    service MyService {
        operations: GetSomething
        errors: [ValidationError]
    }

    operation GetSomething {
        input := {}
        output := {}
    }

Code generators can flatten these errors using a model transformer:

.. code-block:: java

    ModelTransformer transformer = ModelTransformer.create();
    Model transformed = transformer.copyServiceErrorsToOperations(model, service);

After flattening the error hierarchy, the above model is equivalent to:

.. code-block:: smithy

    service MyService {
        operations: GetSomething
    }

    operation GetSomething {
        input := {}
        output := {}
        errors: [ValidationError]
    }


Remove shapes not in the closure of a service
---------------------------------------------

Smithy models can contain multiple services and shapes that aren't connected
to any service. Code generation is often easier if you remove shapes from the
model that are not connected to the service being generated.

.. code-block:: java

    Walker walker = new Walker(someModel);
    Set<Shape> closure = walker.walkShapes(someService);
    model = ModelTransformer.create().removeShapesIf(shape -> !closure.contains(shape));

.. _codegen-selectors:

Selectors
=========

Selectors are used to find shapes in the model that match a query. While
you should typically not need selectors when writing Java code, they can
sometimes make getting the desired set of shapes far simpler than
writing complex loops and conditionals. Selectors have similar caveats
as regular expressions: selectors are slower than handwritten code, and
sometimes handwritten code is easier to understand than the DSL. Whether
a selector is appropriate for a given use case will mostly depend on the
complexity of the query and if there's already a built-in abstraction
for what you're trying to do.


Creating Selectors
------------------

Let's say you want to find something complex, like every operation that
has a :ref:`streaming-trait` member in its input. This can be achieved through
the following selector:

.. code-block:: java

    Selector selector = Selector.parse("operation :test(-[input]-> structure > member > [trait|streaming])");


Finding shapes that match a selector
------------------------------------

``Selector#select`` finds every matching shape and put them in a ``Set``.

.. code-block:: java

    Set<Shape> matches = selector.select(model);


Iterate over shapes that match a selector
-----------------------------------------

If the result set does not need to be loaded into memory, then using
``shapes()`` is cheaper than using ``select()``.

.. code-block:: java

    selector.shapes().forEach(shape -> {
        // do something with each shape
    });


Reuse parsed ``Selector``\ s
----------------------------

Be sure to use a previously parsed selector if a selector will be used
repeatedly. For example don't do this:

.. code-block:: java

    // ❌ DON'T DO THIS ❌

    for (var shape : model.getServiceShapes()) {
        // This is bad! Reuse Selector instances!
        // This has to parse the selector in each iteration of the loop.
        Selector selector = Selector.parse(String.format(
            "[id=%s] -> structure > member[trait|required]",
            shape.getId()));

        selector.shapes(model).forEach(match -> {
            // do something with each found shape
        });
    }

Instead, do this:

.. code-block:: java

    // ✅ DO THIS

    Selector selector = Selector.parse(String.format(
        "[id=%s] -> structure > member[trait|required]",
        shape.getId()));

    for (var shape : model.getServiceShapes()) {
        selector.shapes(model).forEach(match -> {
            // do something with each found shape
        });
    }
