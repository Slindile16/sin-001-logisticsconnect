# TransitServiceApp

## Overview

Calculates estimated arrival windows based on hub and delay stage.

Part of the [LogisticsConnect](../README.md) project. Independent Maven module, no
parent pom.

MQ: this service subscribes to the ActiveMQ topic `package-status-topic` — see [`../common/`](../common). Broker URL and topic name come from the common `co.wethinkcode.logisticsconnect.mq.MqConfig` class alongside it in this module.

## Project structure

```
transit-service/
├── pom.xml
├── README.md
└── src/
    ├── main/java/co/wethinkcode/logisticsconnect/
    │   ├── TransitServiceApp.java
    │   └── mq/MqConfig.java
    └── test/java/co/wethinkcode/logisticsconnect/
        └── TransitServiceAppTest.java
```

## Build

```
mvn package
```

## Run

```
java -jar target/transit-service.jar
```

Listens on port `7053`.

## Endpoint

- `GET /health` confirms the service is running.
- `GET /eta/{hubId}` fetches hub details from Hub Service and the current delay stage from Delay Stage Service, then returns an ETA response.

The illustrative estimate is 60 minutes plus 30 minutes per delay stage. The
response includes the hub, stage, estimated minutes, and an estimated arrival
timestamp. Configure the upstream addresses with `HUB_SERVICE_URL` and
`DELAY_STAGE_SERVICE_URL` (defaults: `http://localhost:7051` and
`http://localhost:7052`).

## Verify manually

Start Hub Service and Delay Stage Service first, then:

```
curl http://localhost:7053/health
curl http://localhost:7053/eta/H-500
```
