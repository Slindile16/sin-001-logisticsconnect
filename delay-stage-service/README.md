# DelayStageServiceApp

## Overview

Tracks the Transit Delay Stage (0-8, e.g. weather shutdowns).

Part of the [LogisticsConnect](../README.md) project. Independent Maven module, no
parent pom.

MQ: this service publishes to the ActiveMQ topic `package-status-topic` — see [`../common/`](../common). Broker URL and topic name come from the common `co.wethinkcode.logisticsconnect.mq.MqConfig` class alongside it in this module.

## Project structure

```
delay-stage-service/
├── pom.xml
├── README.md
└── src/
    ├── main/java/co/wethinkcode/logisticsconnect/
    │   ├── DelayStageServiceApp.java
    │   └── mq/MqConfig.java
    └── test/java/co/wethinkcode/logisticsconnect/
        └── DelayStageServiceAppTest.java
```

## Build

```
mvn package
```

## Run

```
java -jar target/delay-stage-service.jar
```

Listens on port `7052`.

## Endpoints

- `GET /health` confirms the service is running.
- `GET /delay-stage/{hubId}` returns the current stage, defaulting to `0` for a hub with no recorded stage.
- `POST /delay-stage/{hubId}` updates the stage. Send JSON such as `{"stage":3}`; valid stages are integers from `0` to `8`.

Stage values are held in memory and reset when the service restarts. Stage 3
will publish updates to the ActiveMQ topic.

## Verify manually

```
curl http://localhost:7052/health
curl http://localhost:7052/delay-stage/H-500
curl -X POST http://localhost:7052/delay-stage/H-500 -H "Content-Type: application/json" -d '{"stage":3}'
```
