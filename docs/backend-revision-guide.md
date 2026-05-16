# Backend Revision Guide

This guide explains the backend of this project in the same order a real request travels through the app.

Project backend path:

```text
src/main/java/com/yourform/formbuilder
```

Main backend stack:

```text
Java 17
Spring Boot 3.3.5
Spring Web
Spring Data JPA
Hibernate
MySQL
Spring Security
JWT
Lombok
Gemini API integration
```

## 1. Big Picture

This backend is a REST API for a form builder.

Users can:

- register and login
- create forms
- add questions
- add options to questions
- share forms publicly
- submit public responses
- view analytics
- export responses as CSV
- generate templates
- generate AI summaries/questions

The normal backend flow is:

```text
Frontend request
-> Controller
-> Service
-> Repository
-> Database
-> Repository
-> Service
-> Controller
-> JSON response
```

Example:

```text
React calls POST /api/forms
-> FormController.createForm()
-> FormService.createForm()
-> FormRepository.save()
-> MySQL form table
-> saved Form returned to frontend
```

## 2. Main Application File

File:

```text
src/main/java/com/yourform/formbuilder/FormbuilderApplication.java
```

This starts the Spring Boot app:

```java
SpringApplication.run(FormbuilderApplication.class, args);
```

`@SpringBootApplication` tells Spring to:

- start embedded server
- scan classes inside `com.yourform.formbuilder`
- create beans for controllers, services, repositories, security config, etc.

## 3. Configuration

File:

```text
src/main/resources/application.properties
```

Important properties:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/formdb
spring.datasource.username=root
spring.datasource.password=root123
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
gemini.api.key=dummy
```

Meaning:

- `spring.datasource.url`: MySQL database location.
- `username/password`: database login.
- `ddl-auto=update`: Hibernate updates tables automatically from entity classes.
- `show-sql=true`: SQL queries are printed in terminal.
- `gemini.api.key`: API key used by `AiService`.

Important learning point:

If you add a new field in a model class, Hibernate can add a new database column because `ddl-auto=update` is enabled.

## 4. Package Responsibilities

```text
controller  -> HTTP API endpoints
service     -> business logic
repository  -> database operations
model       -> database tables/entities
dto         -> request/response helper objects
security    -> JWT and Spring Security config
```

When changing functionality, first decide which layer owns the change:

- API URL/request/response changes: controller and DTO.
- Rules/validation/calculation: service.
- Database table fields: model.
- Database query methods: repository.
- Login/token/CORS/security: security package.

## 5. Models

Models are database tables. Each class has `@Entity`.

### User

File:

```text
model/User.java
```

Fields:

```text
id
username
password
role
```

Used for registration and login.

Current important note:

Passwords are stored as plain text. For a production project, this should be changed to BCrypt password hashing.

### Form

File:

```text
model/Form.java
```

Fields:

```text
id
title
description
shareToken
collectEmail
limitOneResponsePerEmail
closeAt
timeLimitMinutes
ownerUsername
```

Meaning:

- `shareToken`: public link token.
- `collectEmail`: asks respondent email.
- `limitOneResponsePerEmail`: prevents duplicate submissions by email.
- `closeAt`: form close time.
- `timeLimitMinutes`: time limit setting.
- `ownerUsername`: connects form to logged-in user.

### Question

File:

```text
model/Question.java
```

Fields:

```text
id
text
type
conditionQuestionId
conditionValue
required
displayOrder
form
```

Important relationship:

```java
@ManyToOne
@JoinColumn(name = "form_id")
private Form form;
```

Meaning:

Many questions belong to one form.

Question types used in project:

```text
TEXT
MCQ
DROPDOWN
CHECKBOX
DATE
FILE
```

Conditional logic:

- `conditionQuestionId`: which previous question controls this question.
- `conditionValue`: which answer should show this question.

### Option

File:

```text
model/Option.java
```

Fields:

```text
id
text
question
```

Many options belong to one question.

The table name is `options` because `option` can be confusing/reserved in some databases.

### Response

File:

```text
model/Response.java
```

Fields:

```text
id
submittedAt
respondentEmail
form
```

One response belongs to one form.

### Answer

File:

```text
model/Answer.java
```

Fields:

```text
id
answerText
response
question
```

Each submitted answer connects:

```text
one response + one question + answer text
```

Example:

```text
Response #5
Question #12: "Are you satisfied?"
Answer: "Yes"
```

## 6. Repositories

Repositories talk to the database.

They extend:

```java
JpaRepository<EntityName, IdType>
```

This automatically gives methods like:

```text
save()
findById()
findAll()
deleteById()
deleteAll()
```

### FormRepository

File:

```text
repository/FormRepository.java
```

Custom methods:

```java
Optional<Form> findByShareToken(String shareToken);
List<Form> findByOwnerUsername(String ownerUsername);
```

Spring Data reads the method names and builds queries.

### QuestionRepository

Custom methods:

```java
List<Question> findByFormId(Long formId);
boolean existsByText(String text);
```

`findByFormId` means:

```sql
select * from question where form_id = ?
```

### OptionRepository

Custom methods:

```java
List<Option> findByQuestionId(Long questionId);
void deleteByQuestionId(Long questionId);
```

### ResponseRepository

Custom methods:

```java
List<Response> findByFormId(Long formId);
boolean existsByFormIdAndRespondentEmail(Long formId, String respondentEmail);
```

Used for analytics and one-response-per-email validation.

### AnswerRepository

Custom methods:

```java
void deleteByQuestionId(Long questionId);
void deleteByResponseId(Long responseId);
```

Used during form deletion.

### UserRepository

Custom method:

```java
Optional<User> findByUsername(String username);
```

Used by login/register.

## 7. DTOs

DTO means Data Transfer Object.

DTOs are not database tables. They shape request/response JSON.

### SubmitRequest

File:

```text
dto/SubmitRequest.java
```

Used when submitting answers.

JSON shape:

```json
{
  "formId": 1,
  "respondentEmail": "test@example.com",
  "answers": [
    {
      "questionId": 10,
      "answerText": "Yes"
    }
  ]
}
```

### QuestionResponseDto

Used when sending questions to frontend with their options.

Contains:

```text
id
text
type
required
conditionQuestionId
conditionValue
options
```

### AnalyticsDto

Used for analytics.

Shape:

```text
totalResponses
questions [
  questionText
  answerCount
]
```

### ChartDto

Used for chart labels and values.

### FilterRequest

Used for conditional question filtering.

Maps:

```text
questionId -> answer
```

### InsightDto

Contains generated insight strings.

### OrderRequest

Used to update question display order.

## 8. Controllers

Controllers define API endpoints.

### AuthController

Base URL:

```text
/auth
```

Endpoints:

```text
POST /auth/register
POST /auth/login
POST /auth/forgot-password
```

Register flow:

```text
validate username/password
check username already exists
save user
return message
```

Login flow:

```text
validate username/password
find user by username
compare password
generate JWT
return token
```

Important files involved:

```text
AuthController
UserRepository
JwtUtil
User
```

### FormController

Base URL:

```text
/api/forms
```

Main private/admin form endpoints.

Important endpoints:

```text
POST   /api/forms
GET    /api/forms
GET    /api/forms/{id}
PUT    /api/forms/{id}
DELETE /api/forms/{id}

POST   /api/forms/question
PUT    /api/forms/question/{id}
DELETE /api/forms/question/{id}
GET    /api/forms/{id}/questions

POST   /api/forms/questions/{id}/option
GET    /api/forms/questions/{id}/options
PUT    /api/forms/options/{id}

GET    /api/forms/analytics/{formId}
GET    /api/forms/insights/{formId}
GET    /api/forms/ai-summary/{formId}
GET    /api/forms/ai-questions?topic=...

GET    /api/forms/export/{id}
GET    /api/forms/chart/{id}

POST   /api/forms/{id}/clone
POST   /api/forms/template?type=...
POST   /api/forms/upload
PUT    /api/forms/question/{id}/order
```

Auth pattern:

Most endpoints expect:

```text
Authorization: Bearer <token>
```

Then this method extracts username:

```java
private String currentUsername(String authorization)
```

After username extraction, ownership is checked:

```java
service.assertFormOwner(id, currentUsername(authorization));
```

### PublicFormController

Base URL:

```text
/public/forms
```

Public endpoints do not require login.

Endpoints:

```text
GET  /public/forms/{token}
GET  /public/forms/{token}/questions
POST /public/forms/{token}/submit
```

These use `shareToken`, not form id.

Public submission flow:

```text
share token comes from URL
find form by token
validate close date
validate email rules
validate required questions
save Response
save Answers
return success message
```

### ResponseController

Base URL:

```text
/api/responses
```

Endpoint:

```text
POST /api/responses/submit
```

This submits a response using form id. The public version is more complete because it validates close date, email, and required questions.

## 9. Services

Services contain business logic.

### FormService

This is the biggest backend class.

Main responsibilities:

- create/update/delete forms
- check ownership
- add/update/delete questions
- add/update options
- get questions with options
- submit public forms
- calculate analytics
- export CSV
- clone forms
- create templates
- AI summary/questions

Important methods:

```text
createForm
assertFormOwner
assertQuestionOwner
assertOptionOwner
updateForm
deleteForm
addQuestion
getQuestions
getAnalytics
getFilteredQuestions
generateInsights
generateAiSummary
generateAIQuestions
exportCsv
getChartData
getFormByToken
submitPublicForm
addOption
updateOption
cloneForm
createTemplate
getForms
updateQuestion
deleteQuestion
```

### Ownership

Forms are owned by username:

```java
private String ownerUsername;
```

When a form is created:

```java
form.setOwnerUsername(ownerUsername);
```

When a user tries to access a form:

```java
if (form.getOwnerUsername() == null || !form.getOwnerUsername().equals(ownerUsername)) {
    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
}
```

This prevents one logged-in user from editing another user's form.

### Delete Form

Deleting a form manually deletes:

```text
answers
responses
options
questions
form
```

This is done because the entity relationships do not currently use cascade delete.

### Analytics

Analytics reads all answers:

```java
List<Answer> answers = answerRepo.findAll();
```

Then filters answers by form id.

It counts answers like:

```text
question id -> answer text -> count
```

Example result:

```text
Are you satisfied?
Yes: 10
No: 3
```

### Public Submit

The most important method for response submission:

```text
submitPublicForm(String token, SubmitRequest request)
```

It does:

1. Find form by share token.
2. Check if form is closed.
3. Check email requirement.
4. Check duplicate email.
5. Check required questions.
6. Save `Response`.
7. Save each `Answer`.

### Template Creation

Template creation is inside:

```text
createTemplate(String type, String ownerUsername)
```

It creates a form and default questions/options for types like:

```text
job_application
event_registration
contact
student_admission
appointment
support_ticket
product_order
employee_onboarding
leave_request
customer_survey
newsletter
volunteer
rental_application
project_request
feedback default
```

If a template has fewer than 10 questions, `ensureMinimumTemplateQuestions` adds extra questions.

### ResponseService

Handles:

```text
POST /api/responses/submit
```

It saves a response and answers by form id.

Current note:

This service has less validation than `FormService.submitPublicForm`.

### AiService

Calls Gemini API using `RestTemplate`.

Method:

```text
generateSummary(String data)
```

It builds a Gemini request body, sends it, and parses the response text.

Current note:

The API key is currently `dummy`, so real AI calls will fail until a valid key is configured.

## 10. Security

### SecurityConfig

File:

```text
security/SecurityConfig.java
```

It disables:

```text
CSRF
HTTP Basic
form login
```

It permits:

```text
/auth/**
/api/forms/**
/api/responses/**
/public/forms/**
```

Important learning point:

Spring Security is currently not enforcing JWT automatically. Instead, `FormController` manually reads the `Authorization` header and checks owner access.

### JwtUtil

File:

```text
security/JwtUtil.java
```

Responsibilities:

```text
generateToken(username)
extractUsername(token)
```

Token expiry:

```text
24 hours
```

Current important note:

The JWT signing key is generated when the app starts:

```java
Keys.secretKeyFor(SignatureAlgorithm.HS256)
```

That means old tokens become invalid after backend restart. For production, use a fixed secret from application properties or environment variable.

## 11. Common Request Flows

### Register

```text
POST /auth/register
-> AuthController.register
-> UserRepository.findByUsername
-> UserRepository.save
-> user table
```

### Login

```text
POST /auth/login
-> AuthController.login
-> UserRepository.findByUsername
-> JwtUtil.generateToken
-> token returned
```

### Create Form

```text
POST /api/forms
Authorization: Bearer token
-> FormController.createForm
-> currentUsername
-> FormService.createForm
-> generate shareToken
-> set ownerUsername
-> FormRepository.save
```

### Add Question

```text
POST /api/forms/question
-> FormController.addQuestion
-> assert form owner
-> FormService.addQuestion
-> QuestionRepository.save
```

Request example:

```json
{
  "text": "Are you satisfied?",
  "type": "MCQ",
  "required": true,
  "form": {
    "id": 1
  }
}
```

### Add Option

```text
POST /api/forms/questions/{questionId}/option?text=Yes
-> assert question owner
-> FormService.addOption
-> OptionRepository.save
```

### Get Public Form

```text
GET /public/forms/{token}
-> PublicFormController.getSharedForm
-> FormService.getFormByToken
-> FormRepository.findByShareToken
```

### Submit Public Response

```text
POST /public/forms/{token}/submit
-> PublicFormController.submitPublicResponse
-> FormService.submitPublicForm
-> validate
-> ResponseRepository.save
-> AnswerRepository.save
```

### Analytics

```text
GET /api/forms/analytics/{formId}
-> FormController.analytics
-> assert form owner
-> FormService.getAnalytics
-> AnswerRepository.findAll
-> calculate counts
```

## 12. How To Change Functionality

Use this checklist whenever someone asks for a backend change.

### A. Add a new field to Form

Example: add `themeColor`.

Steps:

1. Add field in `Form.java`.
2. Add getter/setter if not using Lombok.
3. Update `updateForm` in `FormService`.
4. Check frontend sends/reads the field.
5. Restart app; Hibernate updates table.

Files:

```text
model/Form.java
service/FormService.java
frontend files if needed
```

### B. Add a new question type

Example: `RATING`.

Steps:

1. Decide frontend rendering.
2. Allow type value in question creation.
3. If it needs options, use `Option`.
4. If it needs special validation, update `submitPublicForm`.
5. If analytics should treat it differently, update `getAnalytics` or chart logic.

Files:

```text
model/Question.java
service/FormService.java
controller/FormController.java if API changes
frontend rendering files
```

### C. Add validation before submitting response

Example: answer text max length.

Best place:

```text
FormService.submitPublicForm
```

Why:

This is business logic, not controller logic.

### D. Make login more secure

Steps:

1. Add `PasswordEncoder` bean.
2. Hash password during registration.
3. Use `passwordEncoder.matches` during login.
4. Do not compare plain text passwords.

Files:

```text
AuthController.java
SecurityConfig.java
```

### E. Add a new endpoint

Example: get response count for a form.

Steps:

1. Add method in repository if needed.
2. Add method in service.
3. Add endpoint in controller.
4. Add ownership check if private.
5. Test with Postman/frontend.

Typical structure:

```java
@GetMapping("/{id}/response-count")
public Long responseCount(
    @RequestHeader("Authorization") String authorization,
    @PathVariable Long id
) {
    service.assertFormOwner(id, currentUsername(authorization));
    return service.getResponseCount(id);
}
```

### F. Change analytics

Best place:

```text
FormService.getAnalytics
FormService.getChartData
FormService.generateInsights
```

If analytics needs better performance, create repository queries instead of `answerRepo.findAll()`.

### G. Change public form rules

Best place:

```text
FormService.submitPublicForm
```

Examples:

- require email
- block duplicate email
- block after close date
- require all mandatory questions
- validate file answers
- validate answer belongs to option list

### H. Change owner access rules

Best place:

```text
assertFormOwner
assertQuestionOwner
assertOptionOwner
```

These methods protect private endpoints.

### I. Add custom database query

Add method in repository using Spring Data naming:

```java
List<Response> findByFormId(Long formId);
```

Or use `@Query` for complex queries.

## 13. Important Weak Points To Revise

These are good topics to improve after you understand the current backend.

1. Passwords are plain text.
2. JWT secret changes on every restart.
3. Security permits all routes; auth is mostly manual inside controller.
4. Some endpoints are public even if they are admin-style.
5. `ResponseService.submitResponse` has less validation than public submit.
6. `deleteQuestion` does not delete related options/answers first.
7. Analytics uses `answerRepo.findAll()` then filters in Java.
8. CSV export does not escape commas, quotes, or new lines.
9. File upload stores files locally in `uploads`.
10. Some exceptions use generic `RuntimeException` instead of proper HTTP status.
11. `timeLimitMinutes` exists but is not fully enforced during submission.
12. Question ordering is saved but `getQuestions` does not explicitly sort by display order.

## 14. Practice Tasks

Do these in order to become confident.

### Beginner

1. Add `createdAt` to `Form`.
2. Add `updatedAt` to `Form`.
3. Return questions sorted by `displayOrder`.
4. Add delete option endpoint.
5. Add validation that question text cannot be duplicate inside the same form.

### Intermediate

1. Hash passwords with BCrypt.
2. Move JWT secret to `application.properties`.
3. Create a real JWT filter instead of manual token checks.
4. Add pagination to responses.
5. Make CSV export return a downloadable file response.

### Advanced

1. Add cascade mappings between form, questions, options, responses, answers.
2. Replace analytics loop with database aggregation query.
3. Add role-based access.
4. Add refresh tokens.
5. Store file uploads in cloud storage.

## 15. Fast Debugging Guide

### App does not start

Check:

```text
application.properties database URL/password
MySQL is running
port already used
compilation errors
```

### API gives 401

Check:

```text
Authorization header starts with Bearer
token is not expired
backend was not restarted after token was generated
```

### API gives 403

Check:

```text
logged-in user owns the form
ownerUsername is saved correctly
```

### Questions not showing

Check:

```text
question has correct form id
GET /api/forms/{id}/questions is called
token owner matches form owner
```

### Public form not opening

Check:

```text
shareToken exists in form table
frontend uses /public/forms/{token}
```

### Duplicate email not blocked

Check:

```text
limitOneResponsePerEmail is true
respondentEmail is sent
same email string is submitted again
```

## 16. Minimum Concepts To Master

To truly own this backend, revise these concepts:

```text
HTTP methods: GET, POST, PUT, DELETE
REST endpoints
JSON request/response
Spring annotations
Dependency injection
JPA entities
Entity relationships
Repositories
DTOs
Service layer
Validation
JWT
CORS
Transactions
Exception handling
SQL basics
```

Key Spring annotations in this project:

```text
@SpringBootApplication
@RestController
@RequestMapping
@GetMapping
@PostMapping
@PutMapping
@DeleteMapping
@RequestBody
@RequestParam
@PathVariable
@RequestHeader
@Service
@Entity
@Id
@GeneratedValue
@ManyToOne
@JoinColumn
@Repository is not needed because JpaRepository handles it
@Transactional
@CrossOrigin
@Configuration
@Bean
```

## 17. Best Order To Revise This Project

Follow this order:

1. `application.properties`
2. `pom.xml`
3. model classes
4. repository interfaces
5. DTO classes
6. `AuthController`
7. `JwtUtil`
8. `SecurityConfig`
9. `FormController`
10. `PublicFormController`
11. `ResponseController`
12. `ResponseService`
13. `AiService`
14. `FormService`

Leave `FormService` for later because it contains many features together.

## 18. Golden Rule For Any New Feature

Ask these five questions:

1. What JSON will frontend send?
2. Which controller endpoint receives it?
3. Which service method owns the business logic?
4. Which repository/database data is needed?
5. What JSON should backend return?

If you can answer these five, you can change almost any backend functionality in this project.
