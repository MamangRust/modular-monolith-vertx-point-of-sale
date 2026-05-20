# Distributed Modular Monolith Point Of Sale (Vert.x Java 21)

This project is a high-performance Distributed Modular Monolith implementation of a Point of Sale (POS) system. The core design principle is maintaining strict domain boundaries (authentication, roles, merchants, products, cashiers, orders, transactions, etc.) using the Eclipse Vert.x reactive toolkit in Java 21, while deploying them together as a highly available, sharded distributed cluster.

Each domain module communicates internally using strongly-typed gRPC services and exchanges asynchronous message events via Apache Kafka. This architecture offers the optimal balance of isolated microservice boundaries with the simplicity of coordinated distributed monolithic runtime environments.

---

## Core Features

* **Role-Based Authentication and Authorization**
  * Secured JWT-based token authentication.
  * Granular permission authorization (Admin, Merchant, Cashier).
  * Centralized modular permission validation processed at the API Gateway.

* **Merchant and Cashier Management**
  * Merchant registration, storefront configuration, and business document uploads.
  * Independent cashier management scoped per merchant account.

* **Inventory and Product Management**
  * Full product CRUD capabilities aligned with modular category structures.
  * SKU stock tracking and real-time inventory adjustments.

* **Order and Transaction Processing (CQRS)**
  * POS cart orchestration, checkout flows, and payment calculations.
  * Segregated write-optimized transactions and read-optimized CQRS query mechanisms.
  * Monthly and yearly merchant dashboard aggregation.

* **Reactive & Event-Driven Backbone**
  * Non-blocking gRPC communication for zero-latency internal RPC calls.
  * Asynchronous, event-driven checkout notifications using Apache Kafka.
  * Unified reverse proxy entrypoint using Vert.x Web Gateway.

* **Integrated Observability**
  * Distributed tracing via OpenTelemetry exported to Jaeger.
  * High-fidelity metric aggregation via Prometheus mapped to Grafana dashboards.
  * Centralized log forwarding using Loki.

---

## Active Service Module Directory

The system is structured as 11 active reactive modules running on isolated internal ports:

| Service Name | API Protocol | Internal Port | Host Port | Description |
| :--- | :--- | :--- | :--- | :--- |
| **`apigateway`** | HTTP / REST | `5000` | `8091` | Unified entrypoint gateway routing REST requests to gRPC backend stubs. |
| **`auth`** | gRPC | `50051` | `50051` | Authentication management, JWT lifecycle, and database schema migrations. |
| **`role`** | gRPC | `50052` | `50052` | User authorization rules, permission sets, and role associations. |
| **`merchant`** | gRPC | `50054` | `50054` | Merchant profile registry and verification uploads. |
| **`transaction`** | gRPC | `50058` | `50058` | CQRS transaction ledger and statistical aggregations. |
| **`cashier`** | gRPC | `50061` | `50061` | Cashier profiles and merchant scope allocations. |
| **`category`** | gRPC | `50062` | `50062` | Dynamic category taxonomies for product sorting. |
| **`product`** | gRPC | `8086` | `8086` | Inventory management, SKU catalog, and real-time stock levels. |
| **`order`** | gRPC | `8084` | `8084` | Cart validation, purchase processing, and Kafka checkout events. |
| **`order_item`**| gRPC (Read-Only)| `8085` | `8085` | High-efficiency query-only module for scanning order item records. |
| **`email`** | Kafka Consumer | - | - | Asynchronous invoice processing and email SMTP dispatch. |

---

## Technology Stack

* **Eclipse Vert.x 4.x** - Reactive, event-driven, non-blocking asynchronous Java toolkit.
* **Java 21 (Eclipse Temurin)** - Core programming language environment.
* **gRPC Java & Protobuf v3** - High-speed, low-overhead binary RPC protocol.
* **PostgreSQL 17** - Relational storage backend.
* **Flyway Migration** - Automatic database migrations executing at container startup.
* **Redis 7.4** - Distributed caching clustered layer.
* **Apache Kafka** - Distributed streaming log and message broker.
* **OpenTelemetry, Prometheus, Jaeger, Loki, Grafana** - Full-stack observability framework.
* **Docker & Docker Compose** - Local isolated development topology.
* **Kubernetes** - Production container orchestration.

---

## Deployment Architecture

### 1. Local Compose Topology

In the local environment, services connect to a 6-node local Redis cluster (3 Masters, 3 Replicas) configured with cluster-enabled flag, and database queries are routed through a pgBouncer connection pooling layer:

```mermaid
flowchart TD
    Client[HTTP Client] -->|REST/JSON| Gateway["API Gateway<br/>(Port 5000 / 8091)"]

    subgraph POS_Services["Point of Sale Backend Services"]
        Auth["Auth Service<br/>(Port 50051)"]
        Role["Role Service<br/>(Port 50052)"]
        Merchant["Merchant Service<br/>(Port 50054)"]
        Transaction["Transaction Service<br/>(Port 50058)"]
        Cashier["Cashier Service<br/>(Port 50061)"]
        Category["Category Service<br/>(Port 50062)"]
        Product["Product Service<br/>(Port 8086)"]
        Order["Order Service<br/>(Port 8084)"]
        OrderItem["OrderItem Service<br/>(Port 8085)"]
    end

    Gateway -->|gRPC| Auth
    Gateway -->|gRPC| Role
    Gateway -->|gRPC| Merchant
    Gateway -->|gRPC| Transaction
    Gateway -->|gRPC| Cashier
    Gateway -->|gRPC| Category
    Gateway -->|gRPC| Product
    Gateway -->|gRPC| Order
    Gateway -->|gRPC| OrderItem

    subgraph Data_Tier["Shared Data Tier"]
        pgBouncer[pgBouncer Pooler]
        Postgres[(PostgreSQL DB<br/>POINT_OF_SALE)]
        RedisCluster[(6-Node Redis Cluster)]
        
        Auth --> pgBouncer
        Role --> pgBouncer
        Merchant --> pgBouncer
        Transaction --> pgBouncer
        Cashier --> pgBouncer
        Category --> pgBouncer
        Product --> pgBouncer
        Order --> pgBouncer
        OrderItem --> pgBouncer
        
        pgBouncer --> Postgres
        Auth -->|Cluster Cache| RedisCluster
    end

    Kafka[Apache Kafka<br/>Port 9092]
    Order -->|Emit 'checkout_completed'| Kafka
    Kafka -->|Consume Events| EmailWorker["Email Worker<br/>(SMTP Invoices)"]

    classDef default fill:#1e1e2e,stroke:#89b4fa,color:#cdd6f4,stroke-width:1px;
    classDef gateway fill:#1e293b,stroke:#94e2d5,color:#f0fdfa,font-weight:bold;
    classDef core fill:#313244,stroke:#cba6f7,color:#f5e0dc,font-weight:bold;
    classDef infra fill:#292524,stroke:#fab387,color:#fde68a;
```

---

### 2. Kubernetes Production Topology

On Kubernetes, the architecture operates within the `pointofsale` namespace. Horizontal Pod Autoscalers (HPAs) dynamically scale deployments based on resource usage, while pods leverage gRPC TCP probes for health monitoring. Redis is deployed as a sharded StatefulSet:

```mermaid
flowchart TD
    Ingress[Nginx Ingress Controller] -->|HTTP / Port 5000| APIGateway["API Gateway Pods<br/>(HPA Active)"]

    subgraph Pods["POS Service Pods"]
        AuthService["Auth Pods (50051)"]
        RoleService["Role Pods (50052)"]
        MerchantService["Merchant Pods (50054)"]
        TransactionService["Transaction Pods (50058)"]
        CashierService["Cashier Pods (50061)"]
        CategoryService["Category Pods (50062)"]
        ProductService["Product Pods (8086)"]
        OrderService["Order Pods (8084)"]
        OrderItemService["Order-Item Pods (8085)"]
    end

    APIGateway -->|gRPC| AuthService
    APIGateway -->|gRPC| RoleService
    APIGateway -->|gRPC| MerchantService
    APIGateway -->|gRPC| TransactionService
    APIGateway -->|gRPC| CashierService
    APIGateway -->|gRPC| CategoryService
    APIGateway -->|gRPC| ProductService
    APIGateway -->|gRPC| OrderService
    APIGateway -->|gRPC| OrderItemService

    PostgresCluster[(PostgreSQL DB)]
    SharedRedis[(6-Node Redis Cluster StatefulSet)]
    KafkaCluster[[Kafka StatefulSet]]

    AuthService --> PostgresCluster
    RoleService --> PostgresCluster
    MerchantService --> PostgresCluster
    TransactionService --> PostgresCluster
    CashierService --> PostgresCluster
    CategoryService --> PostgresCluster
    ProductService --> PostgresCluster
    OrderService --> PostgresCluster
    OrderItemService --> PostgresCluster

    AuthService --> SharedRedis
    OrderService --> KafkaCluster
    KafkaCluster --> EmailWorker["Email Worker Pods"]

    classDef default fill:#1e1e2e,stroke:#89b4fa,color:#cdd6f4,stroke-width:1px;
    classDef gateway fill:#1a2e05,stroke:#a6e3a1,color:#d9f99d;
```

---

## Local Development Quickstart

### Prerequisites

Ensure the following system tools are installed:
* **Java Development Kit (JDK) 21**
* **Apache Maven 3.9+**
* **Docker & Docker Compose**

### 1. Clone the Repository
```bash
git clone https://github.com/MamangRust/modular-monolith-vertx-point-of-sale.git
cd modular-monolith-vertx-point-of-sale
```

### 2. Compile Java Source Code (Maven Reactor)
Compile the gRPC protobuf files and build the Java binaries:
```bash
mvn clean compile -DskipTests
```

### 3. Build Docker Images
Use the centralized automated build script to construct all backend container images:
```bash
chmod +x build-docker-images.sh
./build-docker-images.sh
```

### 4. Run the Cluster Using Docker Compose
Launch Postgres, Zookeeper, Kafka, the 6 sharded Redis Cluster nodes, and all 11 Java backend modules in detatched mode:
```bash
docker compose -f deployments/local/docker-compose.yml up -d
```
The Redis cluster will automatically initialize itself upon startup via the helper `redis-cluster-init` container. DB schemas are initialized automatically via Flyway when the `auth` container boots.

### 5. Tear Down Local Cluster
To completely stop all containers and wipe volume states:
```bash
docker compose -f deployments/local/docker-compose.yml down -v
```

---

## Production Kubernetes Orchestration

All deployment manifests are organized inside the `deployments/kubernetes/` directory.

### 1. Setup Namespace and Base Configs
Configure the cluster namespace, secure credentials, and common environment properties:
```bash
kubectl apply -f deployments/kubernetes/namespace.yaml
kubectl apply -f deployments/kubernetes/secrets.yaml
kubectl apply -f deployments/kubernetes/configsmaps.yaml
```

### 2. Deploy Common Infrastructure (PostgreSQL, Kafka, Redis Cluster)
Apply volume claims and launch the database, stream engine, and Redis Cluster StatefulSet:
```bash
# Deploy Postgres
kubectl apply -f deployments/kubernetes/postgres-pvc.yaml
kubectl apply -f deployments/kubernetes/postgres-deployment.yaml
kubectl apply -f deployments/kubernetes/postgres-service.yaml

# Deploy Kafka
kubectl apply -f deployments/kubernetes/kafka-pvc.yaml
kubectl apply -f deployments/kubernetes/kafka-deployment.yaml
kubectl apply -f deployments/kubernetes/kafka-service.yaml

# Deploy Redis Cluster (6 StatefulSet Pods + Services + ConfigMap)
kubectl apply -f deployments/kubernetes/redis-cluster.yaml
kubectl apply -f deployments/kubernetes/redis-cluster-service.yaml

# Run the Redis Cluster Creator Job
kubectl apply -f deployments/kubernetes/redis-cluster-init-job.yaml
```

### 3. Deploy POS Application Services
Deploy all microservices, their corresponding load balancer services, and Horizontal Pod Autoscalers:
```bash
# API Gateway
kubectl apply -f deployments/kubernetes/apigateway-deployments.yaml
kubectl apply -f deployments/kubernetes/apigateway-service.yaml
kubectl apply -f deployments/kubernetes/apigateway-hpa.yaml

# Core Domain Services
kubectl apply -f deployments/kubernetes/auth-deployment.yaml
kubectl apply -f deployments/kubernetes/auth-service.yaml

kubectl apply -f deployments/kubernetes/role-deployment.yaml
kubectl apply -f deployments/kubernetes/role-service.yaml

kubectl apply -f deployments/kubernetes/merchant-deployment.yaml
kubectl apply -f deployments/kubernetes/merchant-service.yaml

kubectl apply -f deployments/kubernetes/transaction-deployment.yaml
kubectl apply -f deployments/kubernetes/transaction-service.yaml

kubectl apply -f deployments/kubernetes/cashier-deployment.yaml
kubectl apply -f deployments/kubernetes/cashier-service.yaml

kubectl apply -f deployments/kubernetes/category-deployment.yaml
kubectl apply -f deployments/kubernetes/category-service.yaml

kubectl apply -f deployments/kubernetes/product-deployment.yaml
kubectl apply -f deployments/kubernetes/product-service.yaml

kubectl apply -f deployments/kubernetes/order-deployment.yaml
kubectl apply -f deployments/kubernetes/order-service.yaml

kubectl apply -f deployments/kubernetes/order_item-deployment.yaml
kubectl apply -f deployments/kubernetes/order_item-service.yaml

# Asynchronous Background Workers
kubectl apply -f deployments/kubernetes/email-deployments.yaml
```

Every service pod is configured with a high-fidelity **gRPC TCP Probe** on the container level. This ensures Kubernetes can proactively capture failures and automatically restart unhealthy containers with zero system downtime.

---

## Database Schema (ERD)

The Point of Sale system uses a unified `POINT_OF_SALE` schema mapping domain entities securely across modules. Below is the relational structure designed for the modular monolith, represented as a native **Mermaid.js Entity-Relationship Diagram**:

```mermaid
erDiagram
    users ||--o{ user_roles : "has roles"
    roles ||--o{ user_roles : "assigned to"
    users ||--o{ refresh_tokens : "owns"
    users ||--o{ merchants : "registers"
    users ||--o{ cashiers : "associated user account"
    merchants ||--o{ cashiers : "employs"
    merchants ||--o{ products : "owns"
    categories ||--o{ products : "classifies"
    merchants ||--o{ orders : "received at"
    cashiers ||--o{ orders : "processed by"
    orders ||--|{ order_items : "contains"
    products ||--o{ order_items : "referenced by"
    orders ||--|| transactions : "settles"
    merchants ||--o{ transactions : "belongs to"

    users {
        int user_id PK
        varchar firstname
        varchar lastname
        varchar email UK
        varchar password
        timestamp created_at
        timestamp updated_at
    }

    roles {
        int role_id PK
        varchar role_name UK
        timestamp created_at
        timestamp updated_at
    }

    user_roles {
        int user_role_id PK
        int user_id FK
        int role_id FK
        timestamp created_at
        timestamp updated_at
    }

    refresh_tokens {
        int refresh_token_id PK
        int user_id FK
        varchar token UK
        timestamp expiration
        timestamp created_at
        timestamp updated_at
    }

    merchants {
        int merchant_id PK
        int user_id FK
        varchar name
        text description
        text address
        varchar contact_email
        varchar contact_phone
        varchar status
        timestamp created_at
        timestamp updated_at
    }

    cashiers {
        int cashier_id PK
        int merchant_id FK
        int user_id FK
        varchar name
        timestamp created_at
        timestamp updated_at
    }

    categories {
        int category_id PK
        varchar name
        text description
        varchar slug_category UK
        timestamp created_at
        timestamp updated_at
    }

    products {
        int product_id PK
        int merchant_id FK
        int category_id FK
        varchar name
        text description
        int price
        int count_in_stock
        varchar brand
        int weight
        varchar slug_product UK
        text image_product
        varchar barcode UK
        timestamp created_at
        timestamp updated_at
    }

    orders {
        int order_id PK
        int merchant_id FK
        int cashier_id FK
        bigint total_price
        timestamp created_at
        timestamp updated_at
    }

    order_items {
        int order_item_id PK
        int order_id FK
        int product_id FK
        int quantity
        int price
        timestamp created_at
        timestamp updated_at
    }

    transactions {
        int transaction_id PK
        int order_id FK
        int merchant_id FK
        varchar payment_method
        int amount
        int change_amount
        varchar payment_status
        timestamp created_at
        timestamp updated_at
    }
```
