# Stage 1 Architecture (Ingestion Service)

## Purpose

The **Ingestion Service** is the first service in the LogisticsConnect system. Its responsibility is to read the `hubs-global.csv` file, clean the data, and expose the cleaned records through a REST API for the other services to consume.

At the end of Stage 1, this service will provide a single source of clean hub data.

---

# Data Flow

```text
hubs-global.csv
        │
        ▼
CsvReader
        │
        ▼
List<Hub>
        │
        ▼
DataCleaner
        │
        ▼
Clean List<Hub>
        │
        ▼
IngestionServiceApp
        │
        ▼
GET /hubs
        │
        ▼
JSON Response
```

---

# Class Responsibilities

## IngestionServiceApp

**Purpose**

This is the application's entry point.

**Responsibilities**

* Starts the Javalin web server on port **7050**.
* Loads the CSV data when the application starts.
* Calls the CSV reader and data cleaner.
* Stores the cleaned hub records in memory.
* Exposes REST endpoints:

    * `GET /health`
    * `GET /hubs`

**Should NOT**

* Parse CSV files.
* Contain data-cleaning logic.
* Perform duplicate detection.

---

## Hub

**Purpose**

Represents one hub record from the CSV file.

**Responsibilities**

Stores the cleaned values for:

* Hub ID
* Province
* Sorting Centre
* Active status

This class is simply a data model and should not contain business logic.

---

## CsvReader

**Purpose**

Reads the raw CSV file.

**Responsibilities**

* Opens `hubs-global.csv`.
* Reads every row.
* Converts each row into a `Hub` object.
* Returns a list of hub records.

**Should NOT**

* Trim values.
* Correct spelling.
* Remove duplicates.
* Validate data.

It is responsible only for reading the file.

---

## DataCleaner

**Purpose**

Converts raw hub data into consistent, reliable data.

**Responsibilities**

* Trim leading and trailing whitespace.
* Remove extra spaces inside text.
* Normalize casing.
* Standardize province names.
* Normalize boolean values.
* Handle missing or placeholder values.
* Remove or merge duplicate records.

Returns a cleaned list of hub records.

---

# REST Endpoints

## GET /health

Returns a simple response confirming the service is running.

Example response:

```text
OK
```

---

## GET /hubs

Returns all cleaned hub records as JSON.

Example:

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

# Folder Structure

```text
ingestion-service/
└── src/
    └── main/
        ├── java/
        │   └── co/
        │       └── wethinkcode/
        │           └── logisticsconnect/
        │               ├── IngestionServiceApp.java
        │               ├── Hub.java
        │               ├── CsvReader.java
        │               └── DataCleaner.java
        └── resources/
            └── hubs-global.csv
```

---

# Stage 1 Goal

By the completion of Stage 1, the Ingestion Service should:

* Read the CSV file.
* Clean and standardize the data.
* Store the cleaned records in memory.
* Provide the cleaned data through the `GET /hubs` endpoint.
* Act as the trusted source of hub information for the remaining LogisticsConnect services.
