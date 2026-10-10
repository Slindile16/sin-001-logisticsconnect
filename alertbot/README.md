# AlertBotApp

## Overview

Subscribes to delay-stage updates and simulates posting a public transit alert when
a hub enters a high-delay stage.

Part of the [LogisticsConnect](../README.md) project — its alerting service.
Independent Maven module, no parent pom.

The alert threshold is stage 5. AlertBot remembers each hub's latest stage and
logs a simulated public notification when its stage moves from below 5 to 5 or
higher. Moving back below 5 resets that hub so a later threshold crossing can
raise another alert. No external social media service is contacted.

This service subscribes to the ActiveMQ topic
`package-status-topic` — see [`../common/`](../common). Broker URL and topic name
come from the common `co.wethinkcode.logisticsconnect.mq.MqConfig` class alongside
it in this module.

## Project structure

```
alertbot/
├── pom.xml
└── src/main/java/co/wethinkcode/logisticsconnect/
    ├── AlertBotApp.java
    └── mq/
        ├── AlertThresholdTracker.java
        ├── ActiveMqAlertSubscriber.java
        ├── MqConfig.java
        └── StageChangedEvent.java
```

The threshold tracker has unit tests for crossing and resetting the threshold,
hub ID normalization, and invalid updates. Run them from this module directory
with `mvn test`.

## Build

```
mvn package
```

## Run

```
java -jar target/alertbot.jar
```

Listens on port `7054`.

Start the ActiveMQ broker first using `docker compose up -d` in `common/`, then
start the other services and AlertBot in separate terminals. To verify the alert,
raise a hub's delay stage to 5 or higher through Delay Service:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:7052/delay-stage/H-500 `
  -ContentType "application/json" -Body '{"stage":5}'
```

AlertBot's terminal should log a simulated alert for H-500. Lower the stage below
5 and raise it again to see another alert. Check `http://localhost:7054/health` for
the health endpoint.

## Verification

The health endpoint returns `OK` at `http://localhost:7054/health`. The threshold
behavior was verified live by changing a hub's stage through Delay Service and
confirming the simulated alert in the AlertBot terminal.
