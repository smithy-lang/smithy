======================
Full Stack Application
======================

Overview
========
In this tutorial, imagine we own a coffee shop. We would like to create a website for our customers to place an order online, and be able to grab their coffee on the go. This application should show the available coffees, and allow the customer to order a coffee.

To build this application, we will walk you through using Smithy to define a model for the coffee service, generate code for a client and a server, and implement a front-end and back-end for the service. 

.. tip:: 
    This tutorial does not assume you are an expert in Smithy, but you may find it helpful to work through the
    :doc:`../quickstart` before beginning this tutorial.

Setting Up the Project
======================
This application will consist of a 4 major components:

1. The model
2. The server
3. The client
4. The web application

--------------
Pre-requisites
--------------
To follow this tutorial, you will need to install a few tools:

* :doc:`Smithy CLI <../guides/smithy-cli/cli_installation>`
* `Node.js (>= 16) <https://nodejs.org/en/download>`_ and `yarn <https://yarnpkg.com/getting-started/install>`_

------
Set up
------
Once you have these installed, the CLI has a useful command to easily bootstrap projects from
`the repository of example smithy projects <https://github.com/smithy-lang/smithy-examples>`_. Execute the following
command to set up the initial project:

.. code-block:: sh
    :caption: ``/bin/sh``

    smithy init -t full-stack-application

After running this command, you should have the project in the ``full-stack-application`` directory. You will find a ``README`` containing important information about the project, like how it is laid out and how to build or run it. In this tutorial, we will show you which commands to run and when.

.. TODO: Provide the skeleton template or a git patch?

Modeling Our Service
====================
With the basic framework for the project established, let's walk through how to model our coffee service.

As discussed above, the coffee service should:

* provide a menu of coffees
* provide the ability to order a coffee
* provide the ability to check the status of an order

------------------
Adding a Service
------------------
The service shape is the entry-point of our API, and is where we define the operations our service exposes to a
consumer. First and foremost, let's define the initial service shape without any operations:

.. code-block:: smithy
    :caption: ``main.smithy``

    $version: "2.0"

    namespace com.example

    use aws.protocols#restJson1

    /// Allows users to retrieve a menu, create a coffee order, and
    /// and to view the status of their orders
    @title("Coffee Shop Service")
    @restJson1
    service CoffeeShop {
        version: "2024-04-04"
    }

We apply the ``@restJson1`` protocol trait to the service to indicate the service supports the
:doc:`../aws/protocols/aws-restjson1-protocol`. Protocols define the rules and conventions for how data is serialized and deserialized when communicating between client and server. Protocols are a highly complex topic, so they will not be discussed any further in this tutorial.

-------------
Modeling Data
-------------
Let's create basic representations of our data in Smithy. We will further refine our data model using
:ref:`traits <traits>`. Open the file titled ``coffee.smithy``. We will use it to write our definitions of coffee-related structures:

.. _full-stack-tutorial-operations:

.. code-block:: smithy
    :caption: ``coffee.smithy``

    $version: "2.0"

    namespace com.example

    /// A enum describing the types of coffees available
    enum CoffeeType {
        DRIP
        POUR_OVER
        LATTE
        ESPRESSO
    }

    /// A structure which defines a coffee item which can be ordered
    structure CoffeeItem {
        @required
        type: CoffeeType

        @required
        description: String
    }

    /// A list of coffee items
    list CoffeeItems {
        member: CoffeeItem
    }

-------------------
Modeling Operations
-------------------
With the shapes defined above, let's create an operation on our service for returning a menu to the consumer:

.. code-block:: smithy
    :caption: ``main.smithy`` 

    ...
    service CoffeeShop {
       version: "2024-04-04"
       operations: [
            GetMenu
       ]
    }

    /// Retrieve the menu
    @http(method: "GET", uri: "/menu")
    @readonly
    operation GetMenu {
        output := {
            items: CoffeeItems
        }
    }

The operation is named ``GetMenu``. It does not define an input, and models its output as a structure with a single
member, ``items``, which contains ``CoffeeItems`` (a shape we defined :ref:`above <full-stack-tutorial-operations>`). With the ``restJson1`` protocol, a potential response would be serialized like so:

.. TODO: Add info on http trait?

.. code-block:: json
    :caption: ``GetMenuResponse (json)``

    {
      "items": [
        {
          "type": "LATTE",
          "description": "A creamier, milk-based drink made with espresso"
        }
      ]
    }

-------------------
Representing Orders
-------------------
At this point, we still need to model the ordering functionality of our service. Let's create a new file,
``order.smithy``, which will hold definitions related to ordering. First, let's consider the following when
modeling an order:

1. an order needs a unique identifier
2. an order needs to have a status (such as "in-progress" or "completed"), and
3. an order needs to hold the coffee information (``CoffeeType``)

With these requirements in mind, let's create the underlying data model:

.. code-block:: smithy
    :caption: ``order.smithy``

    $version: "2.0"

    namespace com.example

    /// A unique identifier to identify an order
    @length(min: 1, max: 128)
    @pattern("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$")
    string Uuid

    /// An enum describing the status of an order
    enum OrderStatus {
        IN_PROGRESS
        COMPLETED
    }

A universally unique identifier (or `"UUID" <https://en.wikipedia.org/wiki/Universally_unique_identifier>`_) should be
more than sufficient for our service. The order status is ``IN_PROGRESS`` (when the order is submitted) or
``COMPLETED`` (when the order is ready). The information about what kind of coffee was ordered will be represented by the ``CoffeeType`` shape we defined earlier.

Let's compose these shapes together to create our representation of an order:

.. code-block:: smithy
    :caption: ``order.smithy``

    /// An Order, which has an id, a status, and the type of coffee ordered
    structure Order {
        id: Uuid,
        coffeeType: CoffeeType,
        status: OrderStatus
    }

We're making great progress. However, if we think about an order and its `potential` set of operations
(`creating, reading, updating, deleting <https://en.wikipedia.org/wiki/Create,_read,_update_and_delete>`_ an order),
there is tight relationship between the "state" of an order and its operations. Creating an order "begins" its
lifecycle, while deleting an order would "end" it. In Smithy, we encapsulate the relationship between an entity
and its operations with :ref:`resources <resource>`. Instead of the structure above, let's define an order "resource":

.. code-block:: smithy
    :caption: ``order.smithy``

    /// An Order resource, which has a unique id and describes an order by the type of coffee
    /// and the order's status
    resource Order {
        identifiers: { id: Uuid }
        properties: { coffeeType: CoffeeType, status: OrderStatus }
        read: GetOrder // <--- we will create this next!
        create: CreateOrder  // <--- we will create this next!
    }

With a resource, we attach an identifier, which uniquely identifies an instance of the resource. Properties are
used for representing the state of an instance. In our case, we will only define a subset of the
:ref:`lifecycle operations <lifecycle-operations>` to keep it simple (``create`` and ``read``). Let's define those now:

.. code-block:: smithy
    :caption: ``order.smithy``

    /// Create an order
    @idempotent
    @http(method: "PUT", uri: "/order")
    operation CreateOrder {
        input := for Order {
            @required
            $coffeeType
        }

        output := for Order {
            @required
            $id

            @required
            $coffeeType

            @required
            $status
        }
    }

    /// Retrieve an order
    @readonly
    @http(method: "GET", uri: "/order/{id}")
    operation GetOrder {
        input := for Order {
            @httpLabel
            @required
            $id
        }

        output := for Order {
            @required
            $id

            @required
            $coffeeType

            @required
            $status
        }

        errors: [
            OrderNotFound // <--- we will create this next!
        ]
    }

Since we are defining operations for a resource, we use :ref:`target elision <idl-target-elision>` by prefixing
members corresponding to the resource with ``$``. This reduces the amount of repetition when defining the input and
output shapes of an operation for a resource.

When we define an operation which may return an explicit error, we should model it using the
:ref:`error trait <error-trait>`. Additionally, to refine our error, we will add the
:ref:`httpError trait <httpError-trait>` to set a specific HTTP response status code is set when the error
is returned:

.. code-block:: smithy
    :caption: ``order.smithy``

    /// An error indicating an order could not be found
    @httpError(404)
    @error("client")
    structure OrderNotFound {
        message: String
        orderId: Uuid
    }

Now that we have defined an order resource and its operations, we need to attach the resource to the service:

.. code-block:: smithy
    :caption: ``main.smithy``

    ...
    service CoffeeShop {
        ...
        resources: [
            Order
        ]
    }

Finally, you might be wondering why we didn't model our coffee or menu as a resource. For our service, we are not exposing any functionality related to the *lifecycle* of these entities. However, let's describe a hypothetical example.
We decide a coffee has properties like origin, roast, and tasting notes. Also, we choose to expose operations for adding, updating, and removing coffees. In this case, coffee would be a prime candidate for modeling as a resource.

Building the Model
==================
The model for our coffee service is complete. Before we build the model, let's take a moment and learn how we configure
a build. The :ref:`smithy-build.json configuration file <smithy-build-json>` is how we instruct Smithy to build the
model. A :ref:`projection <projections>` is a version of a model based on a set of :ref:`transformations <transforms>` and :ref:`plugins <plugins>`. For our model, we won't configure any explicit projections, since Smithy will always build the ``source`` projection. The ``source`` projection is the model as it is defined, and includes the artifacts of plugins applied at the root. To build the model, run:

.. code-block:: sh
    :caption: ``/bin/sh``

    smithy build model/

Building the model will render artifacts under the ``build/smithy`` directory. Under it, The ``source`` directory
corresponds to the build artifacts of the ``source`` projection. With the current configuration, Smithy will produce the
model in its :ref:`JSON AST representation <json-ast>`, and a ``sources`` directory which contains the model files used
in the build. Additional artifacts are produced by configuring plugins, and
:doc:`code-generators <../guides/using-code-generation/index>` are prime examples of this.

Generating the Server SDK
=========================
The server SDK is a code-generated component which provides built-in serialization, request-handling, and
scaffolding (or "stubs") for our service as it is modeled. It facilitates the implementation of the service by
providing these things, and allowing the implementer to focus on the business logic. Let's generate the server SDK
for our service by adding the following build configuration:

.. code-block:: json
    :caption: ``smithy-build.json``

    {
        "version": "1.0",
        "maven": {
            "dependencies": [
                "software.amazon.smithy:smithy-aws-traits:1.50.0",
                "software.amazon.smithy:smithy-validation-model:1.50.0",
                "software.amazon.smithy.typescript:smithy-aws-typescript-codegen:0.22.0"
            ]
        },
        "plugins": {
            "typescript-ssdk-codegen": {
                "package" : "@com.example/coffee-service-server",
                "packageVersion": "0.0.1"
            }
        }
    }

Run the build:

.. code-block:: sh
    :caption: ``/bin/sh``

    smithy build model/

The will should fail for the following reason:

.. code-block:: text
    :caption: ``failure message``

    Projection source failed: software.amazon.smithy.codegen.core.CodegenException:
        Every operation must have the smithy.framework#ValidationException error attached
            unless disableDefaultValidation is set to 'true' in the plugin settings.
        Operations without smithy.framework#ValidationException errors attached:
            [com.example#CreateOrder, com.example#GetMenu, com.example#GetOrder]


The server SDK validates inputs by default, and enforces each operation has the ``smithy.framework#ValidationException`` attached to it. We will fix this issue by attaching the error
to our service, meaning all operations in the service may return it. Let's do this now:

.. TODO: does this need a better explanation?

.. code-block:: smithy
    :caption: ``main.smithy``

    use aws.protocols#restJson1
    use smithy.framework#ValidationException

    ...
    service CoffeeShop {
        ...
        errors: [
            ValidationException
        ]
    }


After fixing this and running the build, the TypeScript code-generator plugin will have created a new
artifact under ``build/smithy/source/typescript-ssdk-codegen``. This artifact contains the generated server SDK (SSDK), which we will use in our back-end.

.. TODO: Not sure if we should, but a brief look at the generated code might be good?

Implementing the Server
=======================
For this tutorial, we have included a ``Makefile``, which simplifies the process of building and running the
application. To use it, make sure to run ``make`` from the root of the application directory (where the ``Makefile``
lives). Let's try it now:

.. code-block:: sh
    :caption: ``/bin/sh``

    make build-server

This command will run the code-generation for the server SDK, and then build the server implementation (which uses
the server SDK). The server package is simple, and contains only two files under ``src/``:

* ``index.ts``: entry-point of the backend application, where our server is initialized
* ``CoffeeShop.ts``: implementation of a `CoffeeShopService` from the generated server SDK

The ``ssdk/`` directory is a link to our generated server SDK, which is an output of the smithy build. This is where
the server imports the generated code from. Let's take a look at the core of the coffee shop implementation:

.. code-block:: TypeScript
    :caption: ``CoffeeShop.ts``

    // An implementation of the service from the SSDK
    export class CoffeeShop implements CoffeeShopService<Context> {
        ...

        CreateOrder = async (input: CreateOrderServerInput): Promise<CreateOrderServerOutput> => {
            console.log("received an order request...")
            return;
        }

        GetMenu = async (input: GetMenuServerInput): Promise<GetMenuServerOutput> => {
            console.log("getting menu...")
            return;
        }

        GetOrder = async (input: GetOrderServerInput): Promise<GetOrderServerOutput> => {
            console.log(`getting an order (${input.id})...`)
            return;
        }

        ...
    }

These three methods are how we implement the business logic of the service, and are exposed by the
``CoffeeShopService`` interface exported by the server SDK. This file already contains some of the underlying logic
for how our implementation will run: there is an orders queue, an orders map, and a order-handling procedure
(``handleOrders``). We will use these to implement the operations for our service. Let's start with the simplest
operation, ``GetMenu``:

.. code-block:: TypeScript
    :caption: ``CoffeeShop.ts``

        GetMenu = async (input: GetMenuServerInput): Promise<GetMenuServerOutput> => {
            console.log("getting menu...")
            return {
                items: [
                    {
                        type: CoffeeType.DRIP,
                        description: "A clean-bodied, rounder, and more simplistic flavour profile.\n" +
                            "Often praised for mellow and less intense notes.\n" +
                            "Far less concentrated than espresso."
                    },
                    {
                        type: CoffeeType.POUR_OVER,
                        description: "Similar to drip coffee, but with a process that brings out more subtle nuances in flavor.\n" +
                            "More concentrated than drip, but less than espresso."
                    },
                    {
                        type: CoffeeType.LATTE,
                        description: "A creamier, milk-based drink made with espresso.\n" +
                            "A subtle coffee taste, with smooth texture.\n" +
                            "High milk-to-coffee ratio."
                    },
                    {
                        type: CoffeeType.ESPRESSO,
                        description: "A highly concentrated form of coffee, brewed under high pressure.\n" +
                            "Syrupy, thick liquid in a small serving size.\n" +
                            "Full bodied and intensly aromatic."
                    }
                ]
            }
        }

For our menu, we have added a distinct item for each of our coffee enumerations (``CoffeeType``), as well as a
description. With our menu complete, let's implement order submission, ``CreateOrder``:

.. code-block:: TypeScript
    :caption: ``CoffeeShop.ts``

        CreateOrder = async (input: CreateOrderServerInput): Promise<CreateOrderServerOutput> => {
            console.log("received an order request...")
            const order = {
                orderId: randomUUID(),
                coffeeType: input.coffeeType,
                status: OrderStatus.IN_PROGRESS
            }

            this.orders.set(order.orderId, order)
            this.queue.push(order)

            console.log(`created order: ${JSON.stringify(order)}`)
            return {
                id: order.orderId,
                coffeeType: order.coffeeType,
                status: order.status
            }
        }

For ordering, we will maintain an ``orders`` map to simulate a database where historical order information is stored,
and an orders queue to keep track of in-flight orders. The ``handleOrders`` method will process in-flight orders
and update this queue. Once an order is submitted, it should be able to be retrieved, so let's implement ``GetOrder``:

.. code-block:: TypeScript
    :caption: ``CoffeeShop.ts``

        GetOrder = async (input: GetOrderServerInput): Promise<GetOrderServerOutput> => {
            console.log(`getting an order (${input.id})...`)
            if (this.orders.has(input.id)) {
                const order = this.orders.get(input.id)
                return {
                    id: order.orderId,
                    coffeeType: order.coffeeType,
                    status: order.status
                }
            } else {
                console.log(`order (${input.id}) does not exist.`)
                throw new OrderNotFound({
                    message: `order ${input.id} not found.`
                })
            }
        }
.. TODO: above snippet may need to be updated
.. TODO: add instruction on using the dev* targets

With these operations implemented, our server is fully implemented. Let's build and run it:

.. code-block:: sh
    :caption: ``/bin/sh``

    make run-server

This command will build and run the server. You should see the following output:

.. code-block:: text
    :caption: output

    Started server on port 3001...
    handling orders...

With the server running, let's test it by sending it requests. Open a new terminal and send a request to the ``/menu`` route using ``cURL``:

.. code-block:: sh
    :caption: ``/bin/sh``

    curl localhost:3001/menu

You should see the output of the ``GetMenu`` operation we implemented. You may stop the server by terminating it
in the terminal where it is running with ``CTRL + C``. With the server implemented, we will move on to the client.

Generating the Client
=====================

To run the code-generation for a client, we will add another plugin to the ``smithy-build.json`` configuration file:

.. code-block:: json
    :caption: ``smithy-build.json``

    {
        // ...
        "plugins": {
            // ...
            "typescript-client-codegen": {
                "package": "@com.example/coffee-service-client",
                "packageVersion": "0.0.1"
            }
        }
    }

Run the build:

.. code-block:: sh
    :caption: ``/bin/sh``

    smithy build model/

Similar to the server SDK, the TypeScript client artifacts will be written to the
``build/smithy/source/typescript-client-codegen`` directory. We will use this client to make calls to our backend
service.

Using the Client
================
Like with the server, there is a make target for generating and building the TypeScript client. Let's try it now:

.. code-block:: sh
    :caption: ``/bin/sh``

    make build-client

This command will code-generate the client with Smithy, and then build the generated TypeScript package. The client
will be then be linked in the project root under ``client/sdk``. To use the client ad-hoc, run the following command:

.. code-block:: sh
    :caption: ``/bin/sh``

    make repl-client

This command launches a TypeScript `REPL <https://en.wikipedia.org/wiki/Read%E2%80%93eval%E2%80%93print_loop>`_ with
the generated client installed. Before we use the generated client, we must run the server. Without the server running, the client will not be able to connect. In another terminal, launch the server with the following command:

.. code-block:: sh
    :caption: ``/bin/sh``

    make run-server

With the server running, we will instantiate and use the client. In the terminal running the REPL, insert and run the
following:

.. code-block:: TypeScript
    :caption: ``repl``

    import { CoffeeShop } from '@com.example/coffee-service-client'

    const client = new CoffeeShop({ endpoint: { protocol: 'http', hostname: 'localhost', port: 3001, path: '/' } })

    await client.getMenu()

Like when we tested the server with ``cURL``, you should see the output of the ``GetMenu`` operation we implemented.
Let's try submitting an order:

.. code-block:: TypeScript
    :caption: ``repl``

    await client.createOrder({ coffeeType: "DRIP" })

After creating the order, you should get response like:

.. code-block:: typescript
  :caption: response

    {
      '$metadata': {
        // metadata, such as response code, added by the client
      },
      coffeeType: 'DRIP', // <--- the type of coffee we ordered
      id: 'ee97e900-d8dd-4770-904c-3d175cda90c3',  // <--- the order id
      status: 'IN_PROGRESS' // <--- the order status
    }

The order should be ready by the time you submit this next command. Let's retrieve the order:

.. code-block:: TypeScript
    :caption: ``repl``

    await client.getOrder({ id: '<PUT YOUR ORDER-ID HERE!>' }) // <--- make sure to replace with your id

Once you execute the command, you should see your order information:

.. code-block:: typescript
  :caption: response

    {
      '$metadata': {
        // ...
      },
      coffeeType: 'DRIP', // <--- the type of coffee we ordered
      id: 'ee97e900-d8dd-4770-904c-3d175cda90c3',  // <--- the order id
      status: 'COMPLETED' // <--- the order status, which should be 'COMPLETED'
    }

You may terminate the REPL and the server (with ``CTRL + C`` in the respective terminals). We have
tested each operation we implemented in the server using the generated client, and verified both the client
and server communicate with each other.

Running the Application
=======================
Since we know how to generate and use the client and server, let's put it all together to use with a browser-based web application. The web application exists under the ``app/`` directory, and is built using the ``build-app`` make target. The application will run when using the ``run-app`` target. Since this application uses the generated client to make requests, the server must be ran alongside the app. For convenience, you may run both
the web-application and the server in the same terminal:

.. code-block:: sh
    :caption: ``/bin/sh``

    make run

While running the application in this way is convenient, it will intertwine the output of the web-app and server. If
you would like to keep them separate, you should run the other targets (`run-server` and `run-app`). Using the method
of your choice, launch the server and the application.

.. TODO: Add snippets for how we are calling the service with the client

While this application is incredibly simple, it shows how to integrate a smithy-generated client with an
application running in the browser.
.. TODO: maybe another sentence on takeaways

Making a Change (Optional)
==========================
Now, say we would like to add a new coffee to our menu. The new menu item should have the following details:

* type: COLD_BREW
* description: A high-extraction and chilled form of coffee that has been cold-pressed.
    Different flavor profile than other hot methods of brewing.
    Smooth and slightly more caffeinated as a result of its concentration.

.. note:: Before you proceed to the solution, try making the changes needed by yourself.

.. raw:: html

   <details>
       <summary>Solution</summary>

To add a new coffee, we will first make a change to our model. We need to add a new value for the ``CoffeeType`` enum:

.. code-block:: smithy
    :caption: ``coffee.smithy``

    /// A enum describing the types of coffees available
    enum CoffeeType {
        DRIP
        POUR_OVER
        LATTE
        ESPRESSO
        COLD_BREW
    }

Next, we need to update the server code to add a new item to the menu. First, we should build the model and run the
code-generation for the server SDK, so the new types are generated. Run ``make build-ssdk``. After re-generating the server SDK, we will make the change to our implementation of ``GetMenu``. We will use the new enum value and format the description from above to add a new item:

.. code-block:: TypeScript
    :caption: ``CoffeeShop.ts``

        GetMenu = async (input: GetMenuServerInput): Promise<GetMenuServerOutput> => {
            console.log("getting menu...")
            return {
                items: [
                    ...
                    {
                        type: CoffeeType.COLD_BREW,
                        description: "A high-extraction and chilled form of coffee that has been cold-pressed..\n" +
                            "Different flavor profile than other hot methods of brewing.\n" +
                            "Smooth and slightly more caffeinated as a result of its concentration."
                    }
                ]
            }
        }

Finally, we will run the whole application to see our change (``make run``). After you run it, you should see
the new menu item in the web application, and should be able to order it.

.. raw:: html

   </details>

Wrapping Up
===========
In this tutorial, you used Smithy to build a full-stack application for a simple coffee shop. You wrote a Smithy model for a service based on a list of requirements. Afterwards, you configured builds using the ``smithy-build.json`` configuration, which you set up to code-generate a TypeScript server SDK and a TypeScript client. You implemented the service using the generated server SDK, and then made requests to it using the generated client. Finally, you used
the client in a web application to make requests from within the browser to our service.

.. TOOO: what else?

---------
What now?
---------
We covered several topics in this tutorial, there is still so much to learn! Please check out the
following resources:

* `awesome-smithy <https://github.com/smithy-lang/awesome-smithy>`_: A list of projects based in the smithy ecosystem
* `smithy-examples <https://github.com/smithy-lang/smithy-examples>`_: A repository of example smithy projects