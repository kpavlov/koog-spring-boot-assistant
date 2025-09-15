# AI Testing Challenges

This repository contains a Kotlin/Spring Boot application (module: `app`) and supporting tooling for evaluating LLM prompts. This document explains how to set up your environment, build the project with Maven, run the app, and use the included Makefile tasks for observability and prompt evaluation.

If you're in a hurry, follow the Quick start section. Makefile commands are preferred where available.

## Prerequisites
- Java 17+ (JDK). Using the same JDK as your IDE is recommended.
- Maven 3.9+
- macOS with Homebrew for installing optional tools (or install those tools manually on your platform).

## 1) Create your environment file first
The project includes a template for environment variables.

- Copy the template and edit values as needed:
  - `cp .env.template .env`
- At minimum, ensure your OpenAI key is available to tools that read from `.env`.
  - In `.env` add: `OPENAI_API_KEY=sk-...`

## 2) Define secrets in application properties
The Spring app reads secrets from `app/config/application.properties`.

- Open app/config/application.properties and set your key(s), for example:
    ```properties
    OPENAI_API_KEY=sk-...
    ```
<summary>
<description>
Note: Do not commit real secrets.
</description>
The repo may contain a template, but ensure your local file is ignored by VCS.

Run 
```shell
gitleaks git .
```
or 
```shell
make no-leaks
```
to make sure your secrets are not gonna be commited.
</summary>

## 3) Build the project with Maven
Build the Spring Boot application (module `app`) using Maven:

- `mvn clean package`

This will produce a runnable jar under `app/target/`.

## 4) Run the project
You can run the app from your IDE (IntelliJ IDEA recommended) or via Maven.

- IntelliJ IDEA: Open the project, select the `Application.kt` run configuration (module `app`), and Run.
- Maven:
  `mvn spring-boot:run -pl :app`
- Makefile
    ```shell
    make run
    ``` 

## 5) Observability: run OpenTelemetry TUI
Use the Makefile target to launch the [OpenTelemetry terminal UI (otel-tui)](https://github.com/ymtdzzz/otel-tui). 
This target also cleans any process already bound to the default port.

```shell
make otel
``` 

Tip: If you don't have the tool, run the `prepare` target first to install it.

```shell
make prepare
``` 

## 6) Evaluate prompts with Promptfoo
Promptfoo commands are wrapped in Makefile targets and read environment from the `promptfoo/.env` file if present.

- Start a continuous evaluation and write results to `promptfoo/output.yml`:

    ```shell
    make propmtfoo
    ``` 
- Launch the Promptfoo UI to explore results:

    ```shell
    make promptfoo-ui
    ```

## Troubleshooting
- Ensure `OPENAI_API_KEY` is present in both `.env` (for tools) and `app/config/application.properties` (for the Spring app).
- If Maven cannot find Java, confirm `JAVA_HOME` points to a JDK (not a JRE).
- If otel-tui or promptfoo are missing, run `make prepare` (macOS/Homebrew) or install them manually.

## Useful paths
- Application entry point: [Application.kt](app/src/main/kotlin/com/example/app/Application.kt)
- Spring config: [application.yml](app/src/main/resources/application.yml)
- External properties: [application.properties](app/config/application.properties)
- Prompt scenarios: [scenarios](promptfoo/scenarios)

