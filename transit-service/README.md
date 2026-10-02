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
- `GET /eta/{hubId}` fetches hub details from Hub Service and combines them with the latest delay stage received from `package-status-topic`.

The illustrative estimate is 60 minutes plus 30 minutes per delay stage. The
response includes the hub, stage, estimated minutes, and an estimated arrival
timestamp. The service starts a topic subscriber at startup and uses stage `0`
until it receives an update for a hub. Configure the Hub Service address with
`HUB_SERVICE_URL` (default: `http://localhost:7051`). Start the ActiveMQ broker
before Transit Service using `../common/docker-compose.yml`.

## Verify manually

Start Hub Service and Delay Stage Service first, then:

```
curl http://localhost:7053/health
curl http://localhost:7053/eta/H-500
```
