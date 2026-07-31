$version: "2"
namespace smithy.example

use smithy.api#required
use smithy.api#documentation
use smithy.api#paginated
use smithy.api#readonly
use smithy.api#idempotent
use smithy.api#http
use smithy.api#httpLabel
use smithy.api#httpQuery
use smithy.api#error
use smithy.api#retryable
use smithy.api#sensitive
use smithy.api#deprecated

@paginated(inputToken: "nextToken", outputToken: "nextToken", pageSize: "maxResults")
service RealisticService {
    version: "2024-01-01"
    operations: [
        ListThings
        GetThing
        CreateThing
        UpdateThing
        DeleteThing
        BatchGetThings
        SearchThings
    ]
    resources: [ThingResource, WidgetResource]
}

resource ThingResource {
    identifiers: { thingId: ThingId }
    properties: { name: String, status: ThingStatus }
    read: GetThing
    create: CreateThing
    update: UpdateThing
    delete: DeleteThing
    list: ListThings
    operations: [ArchiveThing, RestoreThing]
}

resource WidgetResource {
    identifiers: { widgetId: WidgetId }
    properties: { label: String, size: Integer }
    read: GetWidget
    create: CreateWidget
    update: UpdateWidget
    delete: DeleteWidget
    list: ListWidgets
}

string ThingId
string WidgetId

enum ThingStatus {
    ACTIVE
    INACTIVE
    ARCHIVED
}

// --- Thing operations ---

@readonly
@http(uri: "/things/{thingId}", method: "GET")
operation GetThing {
    input := {
        @required @httpLabel
        thingId: ThingId
    }
    output := {
        @required
        thingId: ThingId
        @required
        name: String
        status: ThingStatus
        metadata: ThingMetadata
        tags: TagList
        createdAt: Timestamp
    }
    errors: [ThingNotFound, AccessDenied]
}

@http(uri: "/things", method: "POST")
operation CreateThing {
    input := {
        @required
        name: String
        status: ThingStatus
        tags: TagList
        @sensitive
        secret: String
        metadata: ThingMetadata
    }
    output := {
        @required
        thingId: ThingId
        @required
        name: String
    }
    errors: [ValidationError, ConflictError, AccessDenied]
}

@idempotent
@http(uri: "/things/{thingId}", method: "PUT")
operation UpdateThing {
    input := {
        @required @httpLabel
        thingId: ThingId
        name: String
        status: ThingStatus
        tags: TagList
    }
    output := {
        @required
        thingId: ThingId
        name: String
        status: ThingStatus
    }
    errors: [ThingNotFound, ValidationError, AccessDenied]
}

@idempotent
@http(uri: "/things/{thingId}", method: "DELETE")
operation DeleteThing {
    input := {
        @required @httpLabel
        thingId: ThingId
    }
    errors: [ThingNotFound, AccessDenied]
}

@readonly
@paginated
@http(uri: "/things", method: "GET")
operation ListThings {
    input := {
        @httpQuery("maxResults")
        maxResults: Integer
        @httpQuery("nextToken")
        nextToken: String
        @httpQuery("status")
        statusFilter: ThingStatus
    }
    output := {
        @required
        items: ThingList
        nextToken: String
    }
}

@readonly
@http(uri: "/things/batch", method: "POST")
operation BatchGetThings {
    input := {
        @required
        ids: ThingIdList
    }
    output := {
        @required
        items: ThingList
        failedIds: ThingIdList
    }
}

@readonly
@http(uri: "/things/search", method: "POST")
operation SearchThings {
    input := {
        @required
        query: String
        maxResults: Integer
        nextToken: String
        filters: FilterMap
    }
    output := {
        @required
        items: ThingList
        nextToken: String
        totalCount: Long
    }
}

@http(uri: "/things/{thingId}/archive", method: "POST")
operation ArchiveThing {
    input := {
        @required @httpLabel
        thingId: ThingId
        reason: String
    }
    errors: [ThingNotFound, AccessDenied]
}

@http(uri: "/things/{thingId}/restore", method: "POST")
operation RestoreThing {
    input := {
        @required @httpLabel
        thingId: ThingId
    }
    errors: [ThingNotFound, AccessDenied]
}

// --- Widget operations ---

@readonly
@http(uri: "/widgets/{widgetId}", method: "GET")
operation GetWidget {
    input := {
        @required @httpLabel
        widgetId: WidgetId
    }
    output := {
        @required
        widgetId: WidgetId
        @required
        label: String
        size: Integer
        tags: TagList
    }
    errors: [WidgetNotFound]
}

@http(uri: "/widgets", method: "POST")
operation CreateWidget {
    input := {
        @required
        label: String
        size: Integer
        tags: TagList
    }
    output := {
        @required
        widgetId: WidgetId
    }
    errors: [ValidationError]
}

@idempotent
@http(uri: "/widgets/{widgetId}", method: "PUT")
operation UpdateWidget {
    input := {
        @required @httpLabel
        widgetId: WidgetId
        label: String
        size: Integer
    }
    output := {
        @required
        widgetId: WidgetId
        label: String
    }
    errors: [WidgetNotFound, ValidationError]
}

@idempotent
@http(uri: "/widgets/{widgetId}", method: "DELETE")
operation DeleteWidget {
    input := {
        @required @httpLabel
        widgetId: WidgetId
    }
    errors: [WidgetNotFound]
}

@readonly
@paginated
@http(uri: "/widgets", method: "GET")
operation ListWidgets {
    input := {
        @httpQuery("maxResults")
        maxResults: Integer
        @httpQuery("nextToken")
        nextToken: String
    }
    output := {
        @required
        items: WidgetList
        nextToken: String
    }
}

// --- Shared shapes ---

structure ThingMetadata {
    region: String
    account: String
    @deprecated
    legacyField: String
}

list ThingList {
    member: ThingSummary
}

list ThingIdList {
    member: ThingId
}

list WidgetList {
    member: WidgetSummary
}

list TagList {
    member: Tag
}

map FilterMap {
    key: String
    value: FilterValues
}

list FilterValues {
    member: String
}

structure ThingSummary {
    @required
    thingId: ThingId
    @required
    name: String
    status: ThingStatus
}

structure WidgetSummary {
    @required
    widgetId: WidgetId
    @required
    label: String
}

structure Tag {
    @required
    key: String
    value: String
}

// --- Errors ---

@error("client")
@retryable
structure ThingNotFound {
    @required
    message: String
    thingId: ThingId
}

@error("client")
structure WidgetNotFound {
    @required
    message: String
    widgetId: WidgetId
}

@error("client")
structure ValidationError {
    @required
    message: String
    fieldList: ValidationFieldList
}

list ValidationFieldList {
    member: ValidationField
}

structure ValidationField {
    @required
    path: String
    @required
    message: String
}

@error("client")
structure ConflictError {
    @required
    message: String
}

@error("client")
structure AccessDenied {
    @required
    message: String
}
