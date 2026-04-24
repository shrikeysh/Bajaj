# Quiz Leaderboard Spring Service

Spring Boot REST service for the SRM quiz assignment.

## What It Does

- Polls exactly 10 times
- Deduplicates events with `roundId|participant`
- Aggregates participant totals
- Sorts the leaderboard by `totalScore` descending
- Optionally submits the leaderboard once

## Safety First

The app is configured to stay offline by default.

- Default mode is `fixture`
- The bundled fixture is used unless you explicitly switch to `live`
- No real endpoint is called with the default configuration
- Live submission is blocked unless you explicitly enable it

## Endpoints

### `GET /quiz/run`

Runs the full workflow and returns the computed result.

Query params:

- `submit=true` by default
- `regNo` is optional and overrides `app.reg-no` for this request
- `submit=true` will attempt a submit only if live mode is enabled and live submission is allowed

Examples:

```http
GET /quiz/run
GET /quiz/run?submit=true
GET /quiz/run?regNo=RA2311003011887
GET /quiz/run?submit=false&regNo=RA2311003011887
```

## Configuration

`src/main/resources/application.properties`

```properties
server.port=8080

app.mode=fixture
app.reg-no=2024CS101
app.base-url=
app.poll-count=10
app.delay-millis=5000
app.fixture-path=classpath:data/sample-polls.json
app.live-submit-enabled=false
```

Mode options:

- `fixture`: reads bundled sample data and never touches the network
- `live`: polls the external API using `app.base-url`

## Project Structure

```text
src/
|-- main/
|   |-- java/com/example/quiz/
|   |   |-- client/ApiClient.java
|   |   |-- config/AppConfig.java
|   |   |-- config/QuizProperties.java
|   |   |-- controller/QuizController.java
|   |   |-- exception/GlobalExceptionHandler.java
|   |   |-- model/
|   |   |-- service/QuizService.java
|   |   `-- QuizApplication.java
|   `-- resources/
|       |-- application.properties
|       `-- data/sample-polls.json
`-- test/
    `-- java/com/example/quiz/service/QuizServiceTest.java
```

## Run

```bash
mvn spring-boot:run
```

Then open:

```text
http://localhost:8080/quiz/run
```

## Live Mode

If you want to switch to live polling later, update `application.properties`:

```properties
app.mode=live
app.reg-no=YOUR_REG_NO
app.base-url=https://example.com/srm-quiz-task
app.live-submit-enabled=true
```

## Note

This workspace currently does not have Maven installed, so I could not run the Spring build/tests directly here after scaffolding.
