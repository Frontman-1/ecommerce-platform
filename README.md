# 🛒 ShopFlow — E-Commerce Microservices Platform

> Built by **Paila Akhil** | Java Backend Developer

## 🎥 Demo Video
[Watch Demo on LinkedIn](https://www.linkedin.com/feed/update/urn:li:activity:7447709092208410624/?originTrackingId=NghgvNBJOgfTFER51dnG7g%3D%3D)

## 🏗️ Architecture
3 Spring Boot microservices communicating via Apache Kafka.
Orders trigger async payment processing. Redis prevents duplicate payments.

## Tech Stack
| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.5 |
| Messaging | Apache Kafka |
| Cache | Redis 7 |
| Database | MySQL 8 |
| Frontend | Bootstrap 5, HTML/CSS/JS |
| Container | Docker |

## Services
| Service | Port | Responsibility |
|---|---|---|
| product-service | 8081 | Products + Redis caching |
| order-service | 8082 | Orders + Kafka producer |
| payment-service | 8083 | Payments + Kafka consumer |

## Run Locally
```bash
docker compose up --build
```

## Developer
**Paila Akhil** — Java Backend Developer
GitHub: https://github.com/Frontman-1
