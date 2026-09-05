# Stage 2 Architecture (Hub Service)

## Purpose

The **Hub Service** provides hub, province, and sorting-center information to
the other LogisticsConnect services. It receives cleaned hub data from the
Ingestion Service and keeps the records in memory while the service is running.

The Hub Service must not read or clean `hubs-global.csv` itself. The Ingestion
Service remains responsible for producing clean hub data.

---

## Data Flow

```text
Ingestion Service
GET http://localhost:7050/hubs
        │
        ▼
IngestionClient
        │
        ▼
List<Hub>
        │
        ▼
HubRepository
        │
        ▼
HubServiceApp
        │
        ├── GET /hubs
        ├── GET /hubs/{hubId}
        ├── GET /provinces
        └── GET /sorting-centers
```

---

## Class Responsibilities

### HubServiceApp

**Purpose**

This is the application's entry point.

**Responsibilities**

* Creates the Ingestion Service client.
* Loads hub records when the application starts.
* Creates the repository used by the HTTP routes.
* Registers the REST endpoints.
* Starts the Javalin server on port **7051**.
* Returns suitable JSON responses and HTTP status codes.

**Should NOT**

* Read or clean the CSV file.
* Contain HTTP client logic.
* Contain hub-search or filtering logic.

---

### Hub

**Purpose**

Represents one cleaned hub received from the Ingestion Service.

**Responsibilities**

Stores:

* Hub ID
* Province
* Sorting center
* Active status

The field names must match the JSON returned by `GET :7050/hubs` so Jackson can
convert the response into `Hub` objects.

**Should NOT**

* Call another service.
* Clean data.
* Search or store other hub records.

---

### IngestionClient

**Purpose**

Loads cleaned hub data from the Ingestion Service over HTTP.

**Responsibilities**

* Sends `GET http://localhost:7050/hubs`.
* Checks that the response has a successful HTTP status.
* Converts the JSON response into a `List<Hub>`.
* Reports a useful error when the Ingestion Service is unavailable or returns
  invalid data.

**Should NOT**

* Read the CSV file directly.
* Clean or remove hub records.
* Define Hub Service routes.

---

### HubRepository

**Purpose**

Stores the cleaned hubs in memory and provides lookup operations.

**Responsibilities**

* Returns all hubs.
* Finds a hub by its hub ID.
* Returns distinct province names.
* Returns distinct sorting-center names.
* Keeps the HTTP route logic separate from data lookup logic.

**Should NOT**

* Call the Ingestion Service.
* Read or clean CSV data.
* Create HTTP responses.

---

## REST Endpoints

### GET /health

Confirms that the Hub Service is running.

**Successful response**

```text
200 OK
```

---

### GET /hubs

Returns all hubs loaded from the Ingestion Service.

**Successful response**

```text
200 OK
```

```json
[
  {
    "hubId": "H-500",
    "province": "Gauteng",
    "sortingCenter": "Johannesburg Central",
    "active": true
  }
]
```

---

### GET /hubs/{hubId}

Returns the matching hub. Hub IDs should be matched without casing or outer
whitespace differences.

**Successful response**

```text
200 OK
```

**Unknown hub response**

```text
404 Not Found
```

```json
{
  "error": "Hub not found"
}
```

---

### GET /provinces

Returns a distinct list of province names represented by the loaded hubs.

---

### GET /sorting-centers

Returns a distinct list of sorting-center names represented by the loaded hubs.

---

## Startup and Failure Handling

The Ingestion Service must be running before the Hub Service starts. The Hub
Service loads the cleaned hub records once during startup and keeps them in
memory.

If the Ingestion Service cannot be reached, returns a non-successful response,
or returns invalid JSON, the Hub Service should stop startup and display a clear
error. It should not start with an empty list because that could make valid hub
IDs appear to be missing.

---

## Folder Structure

```text
hub-service/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── co/
                └── wethinkcode/
                    └── logisticsconnect/
                        ├── architecture.md
                        ├── HubServiceApp.java
                        ├── Hub.java
                        ├── IngestionClient.java
                        └── HubRepository.java
```

---

## Stage 2 Hub Service Goal

The Hub Service is complete when it:

* Loads cleaned hub data from the Ingestion Service over HTTP.
* Stores the records in memory.
* Returns all hubs through `GET /hubs`.
* Returns a single hub through `GET /hubs/{hubId}`.
* Returns `404` for an unknown hub ID.
* Provides province and sorting-center lists.
* Uses clear class responsibilities and reasonable HTTP responses.
