# Nexus Market — Microservices Ecosystem

A scalable, decoupled e-commerce backend infrastructure built using Spring Cloud, microservices design patterns, and containerized relational storage. This project showcases a fully configured service mesh engineered to handle real-time service discovery, dynamic routing, and isolated data schemas.

---

## 🏗️ System Architecture Overview

The ecosystem is split into three decoupled operational layers, establishing a rigid separation of concerns:

```text
               ┌───────────────────────────┐
               │    Postman / Client       │
               └─────────────┬─────────────┘
                             │ (Port 8000)
                             ▼
               ┌───────────────────────────┐
               │   API Gateway (Netty)     │◄──────┐
               └─────────────┬─────────────┘       │
                             │                     │ Fetches Service
                             │ (Load Balanced)     │ Registry
                             ▼                     │
               ┌───────────────────────────┐       │
               │      PRODUCT-SERVICE      ├───────┼────────┐
               └─────────────┬─────────────┘       │        │
                             │ (Port 8080)         │        │ Registers
                             ▼                     ▼        │ Instance IP
               ┌───────────────────────────┐   ┌───┴────────┴───────┐
               │  PostgreSQL (Docker DB)   │   │  Discovery Server  │
               └───────────────────────────┘   │      (Eureka)      │
                                               └────────────────────┘
