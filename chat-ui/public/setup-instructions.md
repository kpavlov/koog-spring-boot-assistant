## Prerequisites
- Java 17+ (JDK)
- Maven 3.9+

## Quick Start

### 1. Clone the repo

````shell
git clone https://github.com/kpavlov/koog-spring-boot-assistant
cd koog-spring-boot-assistant
````

### 1. Configure API Key
Export OpenAI API key:
```shell
OPENAI_API_KEY=sk-your-key-here
```
Get it on [OpenAI Platform](https://platform.openai.com/api-keys)

### 2. Run the Server
```bash
mvn spring-boot:run -pl :app
```

The server will start on **http://127.0.0.1:8080**. 
API Version endpoint: http://127.0.0.1:8080/api/version

## API Documentation

View the [OpenAPI specification](https://petstore.swagger.io/?url=https://kpavlov.github.io/koog-spring-boot-assistant/docs/openapi.yaml) 
for available endpoints and features.

Project on [GitHub](https://github.com/kpavlov/koog-spring-boot-assistant)
