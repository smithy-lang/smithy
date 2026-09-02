$version: "2.0"

namespace example.catalog

service CatalogService {
    version: "2026-09-02"
    resources: [
        Product
        Supplier
    ]
}

resource Product {
    identifiers: {
        productId: String
    }
    properties: {
        displayName: String
    }
    read: GetProduct
    list: ListProducts
}

resource Supplier {
    identifiers: {
        supplierId: String
    }
    properties: {
        contactEmail: String
    }
    read: GetSupplier
    list: ListSuppliers
}

@readonly
operation GetProduct {
    input := {
        @required
        productId: String
    }
    output := {
        displayName: String
    }
}

@readonly
operation GetSupplier {
    input := {
        @required
        supplierId: String
    }
    output := {
        contactEmail: String
    }
}

@readonly
operation ListProducts {
    output := {
        items: SearchResults
    }
}

@readonly
operation ListSuppliers {
    output := {
        items: SearchResults
    }
}

list SearchResults {
    member: SearchResult
}

structure SearchResult {
    displayName: String
}
