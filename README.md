# Distributed Modular Monolith — Point Of Sale (POS) System (Java Vert.x)

A production-grade, highly resilient, and fully observable **modular-monolith point of sale (POS) backend** built in **Java 21** using the **Eclipse Vert.x** reactive framework. Designed around domain-driven service boundaries following Clean Architecture and CQRS principles, it retains the operational and deployment simplicity of a single deployment unit while maintaining logical isolation typical of microservices.

Each point of sale (POS) business domain — Users, Roles, Banners, Carts, Categories, Merchants, Merchant Awards, Merchant Businesses, Merchant Details, Merchant Policies, Orders, Order Items, Products, Reviews, Review Details, Shipping Addresses, Sliders, Transactions — lives in its own self-contained Maven module. These modules communicate synchronously via high-performance **gRPC** protocols and asynchronously using **Apache Kafka** event propagation, exposing a unified reactive entry point through a **REST API Gateway** powered by the Eclipse Vert.x HTTP Router.

The platform is fortified with a **comprehensive observability suite** (Prometheus, Grafana, Loki, Jaeger, OpenTelemetry, Pyroscope), robust connection pooling via **PgBouncer**, **distributed Redis Cluster caching** with custom telemetry for each service, and Kubernetes configurations ready for production auto-scaling.

---

## Key Features

| Domain | Capabilities |
| :--- | :--- |
| **Auth & Users** | Secure registration, multi-factor login, stateless JWT access/refresh token lifecycle, password reset workflows, OTP email verification, and `/me` profile REST endpoint. |
| **Roles & RBAC** | Custom permission configuration, granular access control matrices, and sub-second permission evaluation cached via Redis. |
| **Catalog & Products** | Full CRUD for products & categories, promo banners, and home slider carousels. |
| **Cart & POS** | Add-to-cart, checkout workflows, order lifecycle management, order-item decomposition, and shipping address details. |
| **Merchants** | Fully featured merchant onboarding, profile details management, business data registration, policies, and merchant awards. |
| **Transactions** | Centralized financial audit ledger collecting transaction and payment events across the system, global search filters, and status tracking. |
| **Reviews** | Product ratings & detailed review submissions post-purchase. |
| **Email Worker** | Kafka-driven asynchronous worker dispatching critical notification emails (OTPs, login alerts, merchant onboarding notices, and transaction invoices) via SMTP. |
| **Observability** | Multi-dimensional metrics (Prometheus + Grafana), log aggregation (Loki + Logback), end-to-end distributed tracing (Jaeger + OpenTelemetry), continuous CPU/Memory profiling (Pyroscope), and resource monitors (Node, Kafka, Postgres Exporters). |
| **Deployment** | Local orchestration using Docker Compose (featuring a 6-node Redis Cluster and PgBouncer), and auto-scaling Kubernetes manifests configured with Horizontal Pod Autoscalers (HPA). |

---

## Architecture Overview

The platform implements a **Distributed Modular Monolith** architecture. Each business service is logical, decoupled, and self-contained inside its own Maven submodule, possessing its own independent gRPC boundary. A **Vert.x REST API Gateway** acts as the unified edge router, transforming client HTTP REST requests into fast gRPC downstream communications via Vert.x gRPC clients.

### Core Architecture Principles

- **Domain-Driven Boundary Isolation**: Every service owns its database access, caching layers, and service logic, strictly forbidding cross-boundary database sharing.
- **Clean Architecture & CQRS**: Separation of concerns using `Handler (gRPC) → Service (Command/Query) → Repository (Command/Query)` layers ensures business logic remains clean, performant, and framework-agnostic.
- **Reactive execution**: Powered entirely by the non-blocking Eclipse Vert.x event loop, enabling high throughput with minimal resource footprints.
- **PgBouncer Pooling**: Employs connection pooling to avoid PostgreSQL socket exhaustion across the multiple concurrent modular services.
- **Event-Driven Resilience**: Apache Kafka decouples transaction events, ensuring side effects like email billing remain completely non-blocking.
- **OTel Telemetry Integration**: Standardized OpenTelemetry middleware injects trace IDs across gRPC boundaries, allowing seamless trace propagation from the client REST gateway down to postgres operations.

```mermaid
graph TB
    classDef client fill:#0f172a,stroke:#38bdf8,color:#e0f2fe,stroke-width:2px,font-weight:bold
    classDef gateway fill:#1e293b,stroke:#22d3ee,color:#cffafe,stroke-width:2px,font-weight:bold
    classDef domain fill:#1e1b4b,stroke:#818cf8,color:#e0e7ff,stroke-width:1.5px
    classDef infra fill:#172554,stroke:#60a5fa,color:#dbeafe,stroke-width:1.5px
    classDef obs fill:#052e16,stroke:#4ade80,color:#dcfce7,stroke-width:1.5px
    classDef event fill:#431407,stroke:#fb923c,color:#fed7aa,stroke-width:1.5px

    Client["Client Applications<br/>(Web / Mobile / API)"]:::client

    subgraph APIGateway["API Gateway — NGINX + Vert.x REST Gateway"]
        direction LR
        REST["REST API Route Handler<br/>Port :5000"]
        AuthMW["JWT Auth & Role<br/>Middleware"]
    end
    class APIGateway gateway

    Client -->|HTTP REST| APIGateway

    subgraph BusinessServices["Business Domain Services (Java Vert.x)"]
        direction TB

        subgraph IdentityDomain["Identity & Access"]
            AUTH["Auth Service<br/>JWT & BCrypt Server"]
            USER["User Service<br/>Profile Management"]
            ROLE["Role Service<br/>RBAC & Permissions"]
        end

        subgraph MerchantDomain["Merchant Management"]
            MERCH["Merchant Service"]
            MDETAIL["Merchant Detail"]
            MBIZ["Merchant Business"]
            MPOL["Merchant Policy"]
            MAWARD["Merchant Award"]
        end

        subgraph CatalogDomain["Catalog & Inventory"]
            PROD["Product Service"]
            CAT["Category Service"]
            BANNER["Banner Service"]
            SLIDER["Slider Service"]
        end

        subgraph POSDomain["POS & Checkout"]
            CART["Cart Service"]
            ORDER["Order Service"]
            OITEM["Order Item Service"]
            TXN["Transaction Service"]
            SHIP["Shipping Address Service"]
        end

        subgraph FeedbackDomain["Customer Feedback"]
            REVIEW["Review Service"]
            RDETAIL["Review Detail Service"]
        end
    end
    class BusinessServices domain

    APIGateway -->|"Vert.x gRPC Client"| BusinessServices

    subgraph Infrastructure["Infrastructure Layer"]
        direction LR
        PGBOUNCER["PgBouncer<br/>Connection Pooler :6432"]
        PG[("PostgreSQL<br/>POINT_OF_SALE DB")]
        REDIS[("Redis Cluster<br/>6-Node Distributed Cache")]
        KAFKA[("Kafka Broker<br/>Event Bus")]
        PYRO["Pyroscope<br/>Continuous Profiler"]
    end
    class Infrastructure infra

    BusinessServices -->|"Reactive SQL Client"| PGBOUNCER
    PGBOUNCER --> PG
    BusinessServices -->|"Vert.x Redis API"| REDIS
    BusinessServices -->|"Publish Events"| KAFKA
    BusinessServices -.->|"Profile Data"| PYRO

    subgraph EventConsumers["Event-Driven Consumers"]
        EMAIL["Email Service<br/>SMTP Notification Worker"]
    end
    class EventConsumers event

    KAFKA -->|"Consume Events"| EMAIL

    subgraph Observability["Observability Stack"]
        direction LR
        PROM["Prometheus<br/>Metrics Engine"]
        LOKI["Loki<br/>Log Aggregator"]
        JAEGER["Jaeger<br/>Distributed Traces"]
        GRAFANA["Grafana<br/>Unified Dashboards"]
        OTEL["OTel Collector<br/>Telemetry Pipeline"]
        PROMTAIL["Promtail<br/>Log Shipper"]
        NODEX["Node Exporter"]
        KAFKAX["Kafka Exporter<br/>Broker Metrics"]
        PGX["Postgres Exporter<br/>DB Performance"]
    end
    class Observability obs

    BusinessServices -.->|"/metrics"| PROM
    BusinessServices -.->|"OTLP Spans"| OTEL
    OTEL -.-> JAEGER
    PROMTAIL -.-> LOKI
    NODEX -.-> PROM
    KAFKAX -.-> PROM
    PGX -.-> PROM
    PROM -.-> GRAFANA
    LOKI -.-> GRAFANA
    JAEGER -.-> GRAFANA
```

---

## Service Catalog

The modular architecture consists of **22 logical micro-applications** plus supporting database and migrations:

```mermaid
graph LR
    classDef svc fill:#1e1b4b,stroke:#a78bfa,color:#ede9fe,stroke-width:1px,rx:8
    classDef gw fill:#1e293b,stroke:#22d3ee,color:#cffafe,stroke-width:2px,rx:8,font-weight:bold
    classDef support fill:#172554,stroke:#60a5fa,color:#dbeafe,stroke-width:1px,rx:8

    subgraph Gateway
        API["API Gateway<br/>Vert.x REST Router"]:::gw
    end

    subgraph Identity["Identity & Access (3)"]
        A1["auth"]:::svc
        A2["user"]:::svc
        A3["role"]:::svc
    end

    subgraph Merchant["Merchant Suite (5)"]
        M1["merchant"]:::svc
        M2["merchant_detail"]:::svc
        M3["merchant_business"]:::svc
        M4["merchant_policy"]:::svc
        M5["merchant_award"]:::svc
    end

    subgraph Catalog["Catalog (4)"]
        C1["product"]:::svc
        C2["category"]:::svc
        C3["banner"]:::svc
        C4["slider"]:::svc
    end

    subgraph POS["POS (5)"]
        O1["cart"]:::svc
        O2["order"]:::svc
        O3["order_item"]:::svc
        O4["transaction"]:::svc
        O5["shipping_address"]:::svc
    end

    subgraph Feedback["Feedback (2)"]
        R1["review"]:::svc
        R2["review_detail"]:::svc
    end

    subgraph Support["Support Services (2)"]
        S1["email"]:::support
        S2["common"]:::support
    end

    API -->|"gRPC Client"| Identity
    API -->|"gRPC Client"| Merchant
    API -->|"gRPC Client"| Catalog
    API -->|"gRPC Client"| POS
    API -->|"gRPC Client"| Feedback
```

---

## Internal Service Architecture

Every logical business service is mapped as a decoupled submodule following structured clean architecture rules.

```mermaid
graph TB
    classDef handler fill:#1e3a5f,stroke:#7dd3fc,color:#e0f2fe,stroke-width:1.5px
    classDef service fill:#1e1b4b,stroke:#a78bfa,color:#ede9fe,stroke-width:1.5px
    classDef repo fill:#172554,stroke:#60a5fa,color:#dbeafe,stroke-width:1.5px
    classDef infra fill:#052e16,stroke:#4ade80,color:#dcfce7,stroke-width:1.5px
    classDef shared fill:#431407,stroke:#fb923c,color:#fed7aa,stroke-width:1.5px

    subgraph Service["Maven Module: <service-name>/"]
        direction TB

        VERTICLE["<ServiceName>Verticle.java<br/>Bootstrap & Lifecycle"]

        subgraph SrcJava["src/main/java/io/example/<service>/"]
            direction TB
            HANDLER["handler/<br/>gRPC Service Handlers"]:::handler
            SVC["service/ & service.impl/<br/>CQRS Business Logic"]:::service
            REPO["repository/ & repository.impl/<br/>Reactive SQL Queries"]:::repo
            MODEL["model/<br/>Entities & Mappings"]:::repo
        end

        VERTICLE --> HANDLER
        VERTICLE --> SVC
        VERTICLE --> REPO
        HANDLER --> SVC
        SVC --> REPO
        REPO --> MODEL
    end

    subgraph SharedLibs["common/ — Shared Maven Module"]
        direction LR
        CONFIG["config/<br/>AppConfig / JwtConfig"]:::shared
        FLYWAY["config/FlywayConfig<br/>Migrations Runner"]:::shared
        REDIS_CFG["config/RedisConfig<br/>Client Pools"]:::shared
        REDIS_SVC["service/RedisService<br/>Cache Actions"]:::shared
        OBS["observability/<br/>TracingMetrics / TelemetryConfig"]:::shared
        PB["proto stubs / pb<br/>gRPC Proto Stubs"]:::shared
    end

    subgraph Infrastructure["External Infrastructure"]
        direction LR
        PGDB[("PostgreSQL")]:::infra
        RCLUSTER[("Redis Cluster")]:::infra
        KAFKA[("Kafka Brokers")]:::infra
    end

    HANDLER --> PB
    SVC --> REDIS_SVC
    SVC --> OBS
    REPO --> PGDB
    REDIS_SVC --> RCLUSTER
    VERTICLE --> FLYWAY
```

---

## Data & Event Flow

### Synchronous Flow (REST Proxy & Cache Read-Through)

All external client API requests go through the REST endpoints defined in the Vert.x API Gateway Router. The API Gateway validates the JWT/API Key, connects with the correct downstream gRPC modular server, checks the Redis Cluster cache, and fetches PostgreSQL through PgBouncer if a cache miss occurs.

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant GW as API Gateway<br/>(Vert.x REST Router)
    participant SVC as Domain Service<br/>(gRPC Server)
    participant REDIS as Redis Cluster
    participant PGB as PgBouncer
    participant DB as PostgreSQL

    C->>GW: HTTP REST Request (GET/POST/PUT)
    GW->>GW: JWT Authentication Check
    GW->>SVC: gRPC Call (Protobuf payload)
    SVC->>REDIS: Check Cache (Redis Cluster)
    alt Cache Hit
        REDIS-->>SVC: Return Cached Response
    else Cache Miss
        SVC->>PGB: Acquire Connection
        PGB->>DB: Reactive SQL Execution
        DB-->>PGB: DB Result Set
        PGB-->>SVC: Reactive Rows Mapped
        SVC->>REDIS: Populate Cache for next read
    end
    SVC-->>GW: gRPC Response payload
    GW-->>C: HTTP REST Response (JSON format)
```

### Asynchronous Flow (Kafka Notification Event pipeline)

High-performance point of sale (POS) actions (like merchant onboarding, order checkouts, or transaction creations) trigger background notification events published directly to Apache Kafka brokers. The isolated Email service listens to Kafka, maps the events, and contacts SMTP services.

```mermaid
sequenceDiagram
    autonumber
    participant SVC as Producer Service
    participant K as Kafka Broker
    participant EMAIL as Email Worker Service
    participant SMTP as SMTP Server

    SVC->>K: Publish Event (e.g. order.created / merchant.onboarded)
    K-->>EMAIL: Deliver topic payload (asynchronous consumer)
    EMAIL->>EMAIL: Map payload details
    EMAIL->>SMTP: Send custom styled notification
    SMTP-->>EMAIL: Delivery Confirmation
```

---

## Observability Architecture

```mermaid
graph TB
    classDef service fill:#1e1b4b,stroke:#818cf8,color:#e0e7ff,stroke-width:1.5px
    classDef collector fill:#172554,stroke:#60a5fa,color:#dbeafe,stroke-width:1.5px
    classDef storage fill:#052e16,stroke:#4ade80,color:#dcfce7,stroke-width:1.5px
    classDef viz fill:#431407,stroke:#fb923c,color:#fed7aa,stroke-width:2px,font-weight:bold

    subgraph Sources["Telemetry Sources"]
        direction TB
        SVCS["All Business Services<br/>(22 services)"]:::service
        KAFKA_SRC["Kafka Broker"]:::service
        NODES["Host / Node"]:::service
        DB_SRC["PostgreSQL Engine"]:::service
    end

    subgraph Collectors["Collection Layer"]
        direction TB
        PROM["Prometheus<br/>Scrapes /metrics"]:::collector
        PROMTAIL["Promtail<br/>Ships container logs"]:::collector
        OTEL["OTel Collector<br/>Receives OTLP spans"]:::collector
        NODEX["Node Exporter<br/>CPU / Memory / Disk / Net"]:::collector
        KAFKAX["Kafka Exporter<br/>Topic lag / Broker health"]:::collector
        PGX["Postgres Exporter<br/>PgBouncer & Query performance"]:::collector
    end

    subgraph Storage["Storage Layer"]
        direction TB
        PROM_TSDB["Prometheus TSDB<br/>(Metrics)"]:::storage
        LOKI_STORE["Loki<br/>(Log Index + Chunks)"]:::storage
        JAEGER_STORE["Jaeger<br/>(Trace Storage)"]:::storage
    end

    subgraph Visualization["Visualization & Alerting"]
        GRAFANA["Grafana<br/>Unified Dashboards"]:::viz
        ALERTMGR["Alertmanager<br/>Alert Routing"]:::viz
    end

    SVCS -->|"/metrics"| PROM
    SVCS -->|"OTLP gRPC"| OTEL
    SVCS -->|"stdout/stderr"| PROMTAIL
    NODES --> NODEX
    KAFKA_SRC --> KAFKAX
    DB_SRC --> PGX

    NODEX --> PROM
    KAFKAX --> PROM
    PGX --> PROM
    PROM --> PROM_TSDB
    PROMTAIL --> LOKI_STORE
    OTEL --> JAEGER_STORE

    PROM_TSDB --> GRAFANA
    LOKI_STORE --> GRAFANA
    JAEGER_STORE --> GRAFANA
    PROM_TSDB --> ALERTMGR
```

| Pillar | Tool | Purpose |
| :--- | :--- | :--- |
| **Metrics** | Prometheus + Grafana | Core metrics tracking (CPU, memory, request error rates, gRPC latencies, DB connection states). |
| **Logging** | Loki + Logback | Centralized structured JSON logger for indexing logs by service, queryable via LogQL. |
| **Tracing** | OpenTelemetry + Jaeger | Distributed system tracing across API gateway and internal gRPC services. |
| **Profiling** | Pyroscope | Continuous memory/CPU profiling to eliminate allocation memory leaks in transaction loops. |
| **Alerting** | Alertmanager | Automated notification system triggered during latency hikes or service disconnects. |

---

## Deployment Architectures

### Docker Compose (Local Development)

The Docker Compose configuration provisions a 6-node Redis Cluster along with databases, event brokers, and reactive service containers to replicate a microservices environment.

```mermaid
flowchart TD
    classDef gateway fill:#1e293b,stroke:#22d3ee,color:#cffafe,stroke-width:2px,font-weight:bold
    classDef core fill:#1e1b4b,stroke:#a78bfa,color:#ede9fe,stroke-width:1.5px
    classDef infra fill:#172554,stroke:#60a5fa,color:#dbeafe,stroke-width:1.5px
    classDef obs fill:#052e16,stroke:#4ade80,color:#dcfce7,stroke-width:1.5px
    classDef event fill:#431407,stroke:#fb923c,color:#fed7aa,stroke-width:1.5px

    subgraph DockerCompose["docker-compose.yml — Local Environment"]

        subgraph Gateway["API Gateway"]
            NGINX["NGINX Proxy :80"]
            APIGW["API Gateway Container<br/>Vert.x REST Gateway :5000"]
        end
        class Gateway gateway

        subgraph Services["Core Service Containers"]
            subgraph Identity["Identity & Access"]
                AUTH["auth"]
                USER["user"]
                ROLE["role"]
            end

            subgraph MerchantSuite["Merchant Domain"]
                MERCH["merchant"]
                MDETAIL["merchant_detail"]
                MBIZ["merchant_business"]
                MPOL["merchant_policy"]
                MAWARD["merchant_award"]
            end

            subgraph CatalogSuite["Catalog"]
                PROD["product"]
                CAT["category"]
                BANNER["banner"]
                SLIDER["slider"]
            end

            subgraph POSSuite["POS & Checkout"]
                CART["cart"]
                ORDER["order"]
                OITEM["order_item"]
                TXN["transaction"]
                SHIP["shipping_address"]
            end

            subgraph ReviewSuite["Feedback"]
                REVIEW["review"]
                RDETAIL["review_detail"]
            end
        end
        class Services core

        subgraph Infra["Infrastructure Suite"]
            PG[("PostgreSQL :5432")]
            PGB[("PgBouncer :6432")]
            REDIS_CLUSTER[("Redis Cluster :6379-6384<br/>6 Nodes Enabled")]
            KAFKA[("Kafka Broker :9092")]
            PYRO[("Pyroscope :4040")]
        end
        class Infra infra

        subgraph Obs["Observability Stack"]
            PROM["Prometheus :9090"]
            GRAFANA["Grafana :3000"]
            LOKI["Loki :3100"]
            JAEGER["Jaeger :16686"]
            OTEL["OTel Collector :4317"]
            NODEX["Node Exporter"]
            KAFKAX["Kafka Exporter"]
            PGX["Postgres Exporter"]
            PROMTAIL["Promtail Log Shipper"]
        end
        class Obs obs

        subgraph Events["Event Consumers"]
            EMAIL["Email Worker"]
        end
        class Events event
    end

    NGINX --> APIGW
    APIGW -->|"gRPC"| Services
    Services -->|"gRPC/SQL"| PGB
    PGB --> PG
    Services --> KAFKA
    KAFKA --> EMAIL

    Services --> REDIS_CLUSTER
    APIGW --> REDIS_CLUSTER

    Services -.->|"Metrics"| PROM
    Services -.->|"Traces"| OTEL
    Services -.->|"Profiles"| PYRO
    OTEL -.-> JAEGER
    PROMTAIL -.-> LOKI
    PROM -.-> GRAFANA
    LOKI -.-> GRAFANA
```

---

## Technology Stack

| Category | Selected Technologies | Purpose |
| :--- | :--- | :--- |
| **Language** | Java 21 (Eclipse Vert.x v4.5.24) | Reactive, non-blocking asynchronous Java execution. |
| **API Edge Gateway** | Vert.x Web Router | Reactive REST API Gateway router and reverse proxy destination. |
| **RPC Inter-service** | Vert.x gRPC Client & Server | Blazing fast, contract-first synchronous gRPC communication. |
| **Database** | PostgreSQL v17 | Safe ACID persistent storage system. |
| **Database Gateway** | PgBouncer | Extreme-efficiency PostgreSQL socket connection pooler. |
| **DB Migrations** | Flyway | Incremental database schema version manager run on startup. |
| **Caching Tier** | Redis Cluster (6 Nodes) | Resilient, distributed key-value cache layer. |
| **Messaging Stream** | Apache Kafka | Asynchronous high-throughput messaging event bus (KRaft mode). |
| **Token Manager** | JWT | Secure stateless request authentication standard. |
| **Observability** | OpenTelemetry + Jaeger | Vendor-neutral distributed telemetry pipeline and visualization. |
| **Continuous Profiler** | Pyroscope | Real-time memory allocation tracker to identify hot paths. |
| **Docker Engine** | Compose | Local environment virtualization orchestration. |
| **Orchestrator** | Kubernetes | Production-scale auto-scaling pod clustering infrastructure. |

---

## Getting Started

### Prerequisites

Ensure the following system packages are locally configured:

- [Git](https://git-scm.com/)
- [Java Development Kit (JDK 21+)](https://adoptium.net/)
- [Apache Maven](https://maven.apache.org/) (v3.9+)
- [Docker](https://www.docker.com/) & [Docker Compose](https://docs.docker.com/compose/)
- [Protobuf Compiler](https://grpc.io/docs/protoc-installation/) (optional)

### 1. Clone the Workspace

```sh
git clone https://github.com/MamangRust/modular-monolith-vertx-point-of-sale.git
cd modular-monolith-vertx-point-of-sale
```

### 2. Prepare Environment Configurations

Setup the system configurations from placeholders:

```sh
# Copy root variables
cp .env.example .env

# Copy local docker settings overrides
cp deployments/local/docker.env.example deployments/local/docker.env
```

### 3. Build the Maven Project

Compile all submodules and build the executable JAR files:

```sh
mvn clean install
```

### 4. Build Docker Images and Start Environment

Use the included build script to compile the service Docker images, then boot the Docker Compose stack:

```sh
# Build docker images for all services
./build-docker-images.sh

# Start local infrastructure, telemetry containers, and application services
docker-compose -f deployments/local/docker-compose.yml up -d
```

Flyway database migrations run automatically on service startup, preparing the database schema.

To verify the cluster services are up and healthy:

```sh
docker-compose -f deployments/local/docker-compose.yml ps
```

---

## Port Map Registry

| Application/Service | Port Configuration / URL |
| :--- | :--- |
| **NGINX Reverse Proxy Edge** | [http://localhost](http://localhost) |
| **API Gateway Direct REST Hub** | [http://localhost:5000](http://localhost:5000) |
| **Grafana Dashboard Portal** | [http://localhost:3000](http://localhost:3000) *(Credentials: `admin`/`admin`)* |
| **Prometheus Telemetry** | [http://localhost:9090](http://localhost:9090) |
| **Jaeger Distributed Tracing** | [http://localhost:16686](http://localhost:16686) |
| **Pyroscope Profiling Panel** | [http://localhost:4040](http://localhost:4040) |
| **PgBouncer Gateway Node** | `localhost:6432` |
| **PostgreSQL Database Engine** | `localhost:5432` |

To stop the development system and clean up resources:

```sh
docker-compose -f deployments/local/docker-compose.yml down -v
```

---

## Maven & Shell Commands Reference

| Command | Scope |
| :--- | :--- |
| `mvn clean install` | Cleans target directories, runs tests, compiles all submodules, and generates package JARs. |
| `mvn compile` | Compiles raw Java source files for all modules. |
| `./build-docker-images.sh` | Orchestrates the build of Docker images for all Vert.x microservices. |
| `docker-compose -f deployments/local/docker-compose.yml up -d` | Launches all containers (DBs, Redis cluster, Kafka, observability, and Java services) in background mode. |
| `docker-compose -f deployments/local/docker-compose.yml down` | Stops compose containers, releasing standard networks. |
| `docker-compose -f deployments/local/docker-compose.yml logs -f <service>` | Follows the realtime stdout logs of a specific service container. |

---

## Workspace Directory Tree

```
vertx-point_of_sale/
├── pom.xml                         # Root Maven Parent POM
├── proto/                          # Protobuf contracts (22 domains)
│   ├── auth/                       #   Authentication specifications
│   ├── banner/                     #   Promo banner specifications
│   ├── cart/                       #   Cart specifications
│   ├── category/                   #   Product category specifications
│   ├── common/                     #   Shared data specifications
│   ├── merchant/                   #   Merchant specifications
│   ├── merchant_award/             #   Merchant award specifications
│   ├── merchant_business/          #   Merchant business specifications
│   ├── merchant_detail/            #   Merchant details specifications
│   ├── merchant_document/          #   Merchant document specifications
│   ├── merchant_social_link/       #   Merchant social link specifications
│   ├── merchant_policy/            #   Merchant policy specifications
│   ├── order/                      #   Order specifications
│   ├── order_item/                 #   Order item specifications
│   ├── product/                    #   Product specifications
│   ├── review/                     #   Review specifications
│   ├── review_detail/              #   Review details specifications
│   ├── role/                       #   Role management specifications
│   ├── shipping_address/           #   Shipping address specifications
│   ├── slider/                     #   Slider carousels specifications
│   ├── transaction/                #   Payment transactions specifications
│   └── user/                       #   User CRUD specifications
├── common/                         # Shared Maven library Module
│   └── src/main/java/io/example/common/
│       ├── config/                 #   AppConfig, JwtConfig, RedisConfig, FlywayConfig
│       ├── observability/          #   TracingMetrics config
│       ├── service/                #   RedisService utilities
│       └── pb/                     #   Compiled Java Protobuf gRPC stubs
├── apigateway/                     # REST API Gateway (REST Router proxying to gRPC)
├── auth/                           # Authentication engine verticle
├── user/                           # User profiles verticle (CQRS)
├── role/                           # RBAC authorization verticle
├── merchant/                       # Merchant core verticle
├── merchant_award/                 # Merchant awards verticle
├── merchant_business/              # Merchant business details verticle
├── merchant_detail/                # Merchant details verticle
├── merchant_policy/                # Merchant policies verticle
├── product/                        # Product management verticle
├── category/                       # Category management verticle
├── cart/                           # Shopping cart verticle
├── order/                          # Order management verticle
├── order_item/                     # Order item decomposition verticle
├── transaction/                    # Payment recording verticle
├── shipping_address/               # Shipping address management verticle
├── banner/                         # Banner management verticle
├── slider/                         # Slider carousels verticle
├── review/                         # Product review verticle
├── review_detail/                  # Review details verticle
├── email/                          # Asynchronous Kafka notifications verticle
├── deployments/
│   ├── local/                      #   Docker compose infrastructure files
│   └── kubernetes/                 #   Production K8s deployment manifests
├── observability/                  #   Telemetry pipelines configurations (Loki, OTEL, Alertmanager)
├── grafana/                        #   Pre-configured dashboard JSON files
└── nginx/                          #   Reverse-proxy NGINX rules
```

---


## License

This project is open-sourced under the MIT License for educational and development purposes.

---

<p align="center">
  Built with Java, Eclipse Vert.x, gRPC, Apache Kafka, and a passion for high-performance reactive modular monoliths.
</p>