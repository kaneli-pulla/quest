# Quest

A full-stack web application for organizing and running team-based quests.

The system supports the complete quest lifecycle: administrators create quests and challenges, teams join through QR codes, participants submit text and photo answers, administrators review submissions, and a live scoreboard calculates results based on solved challenges, completion time, and penalties.

The application was deployed to a production VPS and successfully used during a real-world event.

---

## Features

### Quest management

Administrators can:

- create and edit quests;
- configure descriptions and participant rules;
- upload checkpoint boundary/reference images;
- configure penalties for incorrect attempts;
- start and finish a quest;
- freeze the scoreboard (ICPC format);
- manage teams and challenges.

### Multiple challenge types

The application supports three challenge types:

- `TEXT` — automatically validated text answer;
- `PHOTO` — photo submission reviewed manually by an administrator;
- `TEXT_AND_PHOTO` — combines automatic text validation with manual photo review.

Multiple acceptable text answers can be configured for a challenge.

### Team participation

Each team receives a unique join URL and QR code.

Participants can open the quest directly from a mobile device without an administrator account and access:

- quest rules;
- challenge list;
- answer submission pages;
- team progress;
- current scoreboard.

### Submission workflow

Text submissions are checked automatically.

Photo submissions enter a manual review queue where administrators can accept or reject them.

For combined text + photo challenges, the text part must be correct before the photo can proceed to manual review.

The system prevents:

- submissions to challenges belonging to another quest;
- new submissions after a challenge has already been solved;
- duplicate photo submissions while a previous one is awaiting review;
- submissions when the quest isn't active.

### Photo review queue

Administrators have a dedicated review interface for photo submissions.

Pending submissions are prioritized before already reviewed submissions, with older pending submissions shown first.

### Live scoring

The scoreboard ranks teams by:

1. number of solved challenges;
2. total penalty time;
3. team name as a deterministic tie-breaker.

Penalty time includes:

- time elapsed between quest start and the correct submission;
- configurable penalties for incorrect attempts made before the correct answer.

### Scoreboard freeze and reveal

The project includes a custom scoreboard freeze mechanism designed for the final stage of an event.

When the scoreboard is frozen:

- new submissions continue to be accepted;
- participants do not immediately see their effect on the ranking;
- administrators can see hidden post-freeze results;
- results can later be revealed progressively;
- the scoreboard can eventually be fully unfrozen.

This makes it possible to reveal final results gradually instead of exposing the winner immediately.

### Administration and security

Administrative functionality is protected with Spring Security.

Implemented security measures include:

- role-based access control;
- BCrypt password hashing;
- dedicated administrator authentication;
- deny-by-default routing for unrecognized endpoints;
- server-side input validation;
- configurable production secrets through environment variables;
- secure session cookie settings for production;
- path validation for uploaded files.

---

## Tech Stack

### Backend

- Java
- Spring Boot
- Spring MVC
- Spring Data JPA
- Hibernate
- Spring Security
- Jakarta Persistence

### Frontend

- Thymeleaf
- HTML
- CSS
- JavaScript

### Database

- PostgreSQL

### Additional libraries and technologies

- ZXing — QR code generation
- Maven
- Multipart file uploads
- Spring transaction management

### Production environment

The application was deployed on a Linux VPS using:

- Ubuntu
- Java 21
- PostgreSQL
- Nginx
- HTTPS / Let's Encrypt
- systemd

Nginx acted as a reverse proxy while the Spring Boot application was bound to the local server interface.

---

## Architecture

The application follows a layered Spring architecture:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
PostgreSQL
