===============
Java Quickstart
===============

This guide introduces `Smithy Java <https://github.com/smithy-lang/smithy-java>`_ with a simple working example of a
generated server and client.

For this example, imagine that you are the proud owner of a coffee shop.
Your API allows your customers to order some *java* from their Java applications.
Users can use your SDK to list available coffees, order a coffee, and track the status of their order.

.. admonition:: Review
    :class: tip

    If you are new to Smithy or just need a refresher on the basics, you may find it helpful to work through the
    Smithy :doc:`/quickstart`.

-------------
Prerequisites
-------------

* Ensure you have `JDK 17 <https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html>`_ or higher
  installed. Older Java versions are not supported by Smithy Java.
* Ensure you have the the Smithy CLI installed. Run ``smithy --version`` to confirm the CLI is correctly installed.
  If you need to install the CLI, see :doc:`Smithy CLI Installation </guides/smithy-cli/cli_installation>`.

------
Set Up
------

Clone the `quickstart example template <https://github.com/smithy-lang/smithy-examples/tree/main/smithy-java-examples/quickstart-java>`_
using the Smithy CLI ``init`` command and change to the cloned directory:

.. code-block:: sh

    smithy init -t smithy-java-quickstart && cd smithy-java-quickstart

The directory tree of the project should look like:

.. code-block:: sh

    smithy-java-quickstart/
    ├── smithy
    │   ├── ...
    ├── client
    │   ├── ...
    ├── server
    │   ├── ...
    ├── README.md
    └── ...

The example project contains a number of packages:

* ``smithy/``: Common package for the service API model. Used by both client and server packages.
* ``server/``: Code generated Server that implements stubbed operations code-generated from the service model.
* ``client/``: Code generated client that can call the server.

From the root of the example project, build the project using Gradle:

.. code-block:: sh

    ./gradlew clean build

-------------
Service Model
-------------

The Smithy API model for our service can be found in the model package. This model defines the interface of our service and
guides the generation of client and server code.

The service provides a few capabilities:

* Get a menu of available coffees with descriptions
* The ability to order a coffee.
* The ability to check the status of an order.

The service has the ``@restJson1`` protocol trait applied indicating that the service supports the :ref:`AWS restJson1 protocol <aws-restjson1-protocol>`.

.. code-block:: smithy
    :caption: smithy/model/weather.smithy

    /// Allows users to retrieve a menu, create a coffee order, and
    /// and to view the status of their orders
    @title("Coffee Shop Service")
    @restJson1
    service CoffeeShop {
        ...
    }

Protocols define the rules and conventions for serializing and de-serializing data when communicating between
client and server.

Services can support multiple protocols at once.

-------------------
Running the project
-------------------

First, start the coffee shop service by executing the following command:

.. code-block:: sh

    ./gradlew :server:run

This will start the coffee shop server on port ``8888`` and log the following to the console:

.. code-block:: sh
    :caption: terminal output

    INFO: Starting server...
    INFO: Started listening on http://localhost:8888

.. note::

    Timestamps are omitted from console logs for the sake of brevity.


To confirm the service is working, request the menu:

.. code-block:: sh

    curl localhost:8888/menu

This will return a JSON-formatted menu of coffee types that can be ordered from our cafe:

.. code-block:: json

    {
      "items": [
        {
          "type": "DRIP",
          "description": "A clean-bodied, rounder, and more simplistic flavour profile.\nOften praised for mellow and less intense notes.\nFar less concentrated than espresso.\n"
        },
        {
          "type": "POUR_OVER",
          "description": "Similar to drip coffee, but with a process that brings out more subtle nuances in flavor.\nMore concentrated than drip, but less than espresso.\n"
        },
        {
          "type": "LATTE",
          "description": "A creamier, milk-based drink made with espresso.\nA subtle coffee taste, with smooth texture.\nHigh milk-to-coffee ratio.\n"
        },
        {
          "type": "ESPRESSO",
          "description": "A highly concentrated form of coffee, brewed under high pressure.\nSyrupy, thick liquid in a small serving size.\nFull bodied and intensely aromatic.\n"
        }
      ]
    }

.. tip::

    Use the ``jq``` command line utility to pretty-print the output of the ``curl`` command above.

You may stop the server with ``CTRL + C``` in the terminal where it is running.
With the server running, we can now call it with our client application.
In a separate terminal, execute the client application:

.. code-block:: sh

    ./gradlew :client:run

The client application will use a code-generated Java SDK for the coffee shop service to:

1. Create a new coffee order for a refreshing COLD_BREW coffee,
2. Wait a few seconds for the order to complete, and
3. Call the service again to get the order.

The client terminal will print the following to the console (your order ID will differ):

.. code-block:: sh
    :caption: terminal output

    INFO: Created request with id = f526ddca-105c-4f89-a754-a10ea542c84b
    INFO: Got order with id = f526ddca-105c-4f89-a754-a10ea542c84b
    INFO: Waiting for order to complete....
    INFO: Completed Order :GetOrderOutput[id=f526ddca-105c-4f89-a754-a10ea542c84b, coffee

----------------------------
Make a change to the service
----------------------------

In this section, you will update the Coffee shop server application to support additional functionality.
We would like to add a new operation to our service that allows users to get the hours of our cafe.

The new operation, ``GetHours``, should be bound directly to our service shape, take no input, and should return an output
with both the opening and closing times. We will host this operation on the route ``/hours`` , and the reported hours
will be expressed in hours using 24hr time (i.e. 1PM is 13).

Model Update
============

First, the new operation must be added to our service model in the smithy package:

.. code-block:: diff
    :caption: smithy/model/main.smithy

    service CoffeeShop {
        version: "2024-08-23"
        operations: [
            GetMenu,
    +       GetHours
        ]
        resources: [
            Order
        ]
    }

    + /// Retrieve the coffee shop hours.
    + @http(method: "GET", uri: "/hours")
    + @readonly
    + operation GetHours{
    +    output := {
    +        opensAt: Hour
    +        closesAt: Hour
    +     }
    + }

    + // Hours for a day expressed in 24hr time
    + @range(min: 0, max: 24)
    + integer Hour

Server Update
=============

With our service model updated, we need to add the new functionality to our server. First, rebuild the project:

.. code-block:: sh

    ./gradlew clean build

This will fail with a compilation error:

.. code-block:: sh
    :caption: ``build`` output

    error: cannot find symbol
    ...
            .addGetMenuOperation(new GetMenu())
                                    ^
      symbol:   method addGetMenuOperation(GetMenu)
      location: interface GetHoursStage

Smithy Java **requires** that an implementation of a generated operation interface be registered with the server for
every operation defined in service model. Let’s add the required implementation:

.. code-block:: java
    :caption: server/src/main/java/io/smithy/java/server/example/GetHoursOperation.java

    final class GetHours implements GetHoursOperation {
        @Override
        public GetHoursOutput getHours(GetHoursInput input, RequestContext context) {
            return GetHoursOutput.builder()
                    .opensAt(9) // Opens at 9AM
                    .closesAt(16) // Closes at 4PM
                    .build();
        }
    }

Next, register this operation with our service:

.. code-block:: diff
    :caption: server/src/main/java/io/smithy/java/server/example/CafeService.java

    CoffeeShop.builder()
            .addCreateOrderOperation(new CreateOrder())
    +       .addGetHoursOperation(new GetHours())
            .addGetMenuOperation(new GetMenu())
            ...

Now, re-start our server

.. code-block:: sh

    ./gradlew server:run

Finally, we can test the new operation using curl:

.. code-block:: sh

    curl localhost:8888/hours

Which will return the hours of our Cafe:

.. code-block:: java
    :caption: ``curl`` output

    {"opensAt":9,"closesAt":16}

Client Update
=============

What if we want to call our new operation from our client application?
The client code generator will automatically add the ``getHours`` operation to the generated client,
we just need to call it in our client application:

.. code-block:: diff
    :caption: client/src/main/java/io/smithy/java/client/example/Main.java

    public static void main(String[] args) throws InterruptedException {
    +   LOGGER.info(client.getHours(GetHoursInput.builder().build()).toString());

With the server still running, call our client one more time:

.. code-block:: sh

    ./gradlew client:run

A new log line will now appear, listing the cafe’s hours:

.. code-block:: sh
    :caption: terminal output

    INFO: GetHoursOutput[opensAt=9, closesAt=16]

----------
Next steps
----------

* Explore other examples: `Smithy Java example projects <https://github.com/smithy-lang/smithy-java/tree/__smithy_java_version__/examples>`_
* Discover the Smithy ecosystem: `Awesome-Smithy <https://github.com/smithy-lang/awesome-smithy>`_
