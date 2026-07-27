# --- 第一阶段：构建 ---
# 使用 Maven 官方镜像作为基础环境，负责编译代码和下载依赖
FROM maven:3.8.5-openjdk-17 AS builder

# 设置工作目录
WORKDIR /app

# 复制 pom.xml 并先下载依赖（利用 Docker 缓存层加速后续构建）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 执行打包命令，跳过测试以加快速度
RUN mvn package -DskipTests


# --- 第二阶段：运行 ---
# 使用轻量级的 JRE 镜像（只包含运行环境，体积更小）
FROM openjdk:17-jdk-alpine

# 设置工作目录
WORKDIR /app

# 从第一阶段复制打好的 jar 包，并重命名为 app.jar
COPY --from=builder /app/target/mz-0.0.1-SNAPSHOT.jar app.jar

# 暴露端口（根据你的 application.yml 配置修改，默认通常是 8080）
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]