# HubServiceApp

## Overview

Serves provinces and sorting centers (place-name source of truth).

Part of the [LogisticsConnect](../README.md) project. Independent Maven module, no
parent pom.

## Project structure

```
hub-service/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   └── java/co/wethinkcode/logisticsconnect/
    │       ├── architecture.md
    │       ├── Hub.java
    │       ├── HubRepository.java
    │       ├── HubServiceApp.java
    │       └── IngestionClient.java
    └── test/
        └── java/co/wethinkcode/logisticsconnect/
            ├── HubRepositoryTest.java
            ├── HubServiceAppTest.java
            ├── HubTest.java
            └── IngestionClientTest.java
```

## Build

```
mvn package
```

## Run

```
java -jar target/hub-service.jar
```

Listens on port `7051`.

## Endpoints

- `GET /health` confirms the service is running.
- `GET /hubs` returns all cleaned hubs loaded from Ingestion Service.
- `GET /hubs/{hubId}` returns one hub (IDs are case-insensitive); unknown IDs return `404` with a JSON error.
- `GET /provinces` returns distinct province names.
- `GET /sorting-centers` returns distinct sorting-center names.

The service loads its hub data once at startup. Ingestion Service must be
running first. Set `INGESTION_SERVICE_URL` to override its default address
(`http://localhost:7050`).

## Verify manually

With Ingestion Service and Hub Service running:

```
curl http://localhost:7051/health
curl http://localhost:7051/hubs
curl http://localhost:7051/hubs/H-500
curl http://localhost:7051/provinces
curl http://localhost:7051/sorting-centers
```
