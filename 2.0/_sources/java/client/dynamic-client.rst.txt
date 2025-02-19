==============
Dynamic Client
==============

The dynamic client is used to interact with services without a code-generated client.
The dynamic client loads Smithy models at runtime, converting them to a schema-based client.
Users can call a modeled service using document types as input and output.

.. warning::

    The dynamic client does not currently support streaming or event streaming.

Usage
-----

Add the dynamic-client module as a dependency of your project:

.. code-block:: kotlin
   :caption: build.gradle.kts

    dependencies {
        implementation("software.amazon.smithy.java:dynamicclient:__smithy_java_version__")
    }

Then, load a Smithy model:

.. code-block:: java

    import software.amazon.smithy.java.dynamicclient
    import software.amazon.smithy.model.Model;
    ...

    var model = Model.assembler()
        .addImport("/path/to/model.json")
        .assemble()
        .unwrap();

Then, select a service shape in the loaded model to call. In this case, the ``CoffeeShop`` service:

.. code-block:: java

    var shapeId = ShapeId.from("com.example#CoffeeShop");

Now, create the ``DynamicClient`` instance for this model and service:

.. code-block:: java

    var client = DynamicClient.builder()
        .service(shapeId)
        .model(model)
        .protocol(new RestJsonClientProtocol(shapeId))
        .transport(new JavaHttpClientTransport())
        .endpointResolver(EndpointResolver.staticEndpoint("https://api.cafe.example.com"))
        .build();

.. admonition:: Important
    :class: note

    If an explicit protocol and transport are not provided to the builder, the builder will attempt to find protocol
    and transport implementations on the classpath that match the protocol traits attached to the service.

To call the service, create an input using a ``Document`` that mirrors what's defined in the Smithy model.
The ``Document.createFromObject`` method can create a ``Document`` from a map:

.. code-block:: java

    var input = Document.createFromObject(Map.of("coffeeType", "COLD_BREW"));
    var result = client.call("CreateOrder", input).get();
    System.out.println(result);
