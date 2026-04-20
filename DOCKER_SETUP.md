# Docker 化配置

## 1. Dockerfile (用于打包 Spring Boot 后端)

在 `backend` 目录下创建一个名为 `Dockerfile` 的文件，内容如下：

```dockerfile
# syntax=docker/dockerfile:1

# Build 阶段：使用 Java 21 + Maven 编译 Spring Boot
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

# Runtime 阶段：仅保留运行时镜像
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# 暴露 Spring Boot 默认端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 2. docker-compose.yml (用于一键启动数据库和后端)

在项目根目录下创建一个名为 `docker-compose.yml` 的文件，内容如下：

```yaml

services:
  # PostgreSQL 数据库服务
  db:
    image: postgres:15-alpine
    container_name: seichou_db
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
      POSTGRES_DB: seichou_logos
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped

  # Spring Boot 后端服务
  backend:
    build: 
      context: ./backend
      dockerfile: Dockerfile
    container_name: seichou_backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/seichou_logos
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=password
      - DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} # 从宿主机的环境变量中读取
    depends_on:
      - db
    restart: unless-stopped

volumes:
  # 命名卷，用于持久化数据库数据
  postgres_data:
```

### 使用说明：

1.  **启动服务**：在项目根目录运行 `docker compose up -d`。Docker 会自动拉取 PostgreSQL 镜像并构建/启动 Spring Boot。
2.  **访问 API**：后端服务将在 `http://localhost:8080` 运行，你可以通过 Swagger UI (`http://localhost:8080/swagger-ui.html`) 测试接口。
