# Fabflix

A movie storefront built as a Java servlet application, deployed to Kubernetes
as containerized services behind an Nginx Ingress.

Originally built for UC Irvine CS 122B (Winter 2025) as a two-person team.. Published here with the course dataset and all credentials removed.

**Demo:** https://www.youtube.com/watch?v=yhKR3EB0HRk

## What's interesting in here

- **Split read/write datasources** — two `jdbc/moviedb` DataSources in
  `WebContent/META-INF/context.xml` behind a Tomcat JDBC pool (`maxTotal=100`),
  routing catalog reads to a replica so they stop contending with checkout writes.
- **Three-stage search cascade** (`src/servlets/SearchServlet.java`,
  `AutocompleteSearch.java`) — MySQL FULLTEXT in boolean mode, falling back to
  `LIKE` substring matching, falling back to Levenshtein edit distance via UCI's
  Flamingo UDF in `toolkit/`. Edit-distance tolerance scales with query length.
- **Multi-service Kubernetes deployment** — `fabflix-multi.yaml` splits login and
  movies into independent Deployments and Services; `ingress-multi.yaml` routes
  between them by path. `fabflix.yaml` is the single-service variant.
- **Auth** — Jasypt salted password hashing (`VerifyPassword.java`), stateless JWT
  in an HttpOnly cookie (`src/common/JwtUtil.java`), reCAPTCHA on login.

## Running it

Requires JDK 17+, Maven, MySQL 8, and Tomcat 10.

```bash
export JWT_SECRET=<base64-encoded 256-bit key>
export RECAPTCHA_SECRET=<your recaptcha v2 secret>

mvn clean package          # produces target/fabflix.war
```

Deploy the WAR to Tomcat. It serves at `/fabflix`.

Set the database password in `WebContent/META-INF/context.xml` — it currently
reads `CHANGE_ME`.

### Database

The schema and movie dataset were provided by the course and are not included
here. `MoviesServlet`, `BrowseServlet`, and `SearchServlet` expect the standard
`moviedb` schema (`movies`, `stars`, `genres`, `stars_in_movies`,
`genres_in_movies`, `ratings`, `customers`, `sales`, `creditcards`, `employees`).

### Docker

```bash
docker build -t <you>/fabflix:v1 .
docker push <you>/fabflix:v1
kubectl apply -f fabflix-multi.yaml
kubectl apply -f ingress-multi.yaml
```

Update the `image:` fields in the manifests to your own registry first.

## A note on the artifact name

The Maven `artifactId` and `<finalName>` are both `fabflix`, which determines the
WAR filename and therefore the Tomcat context path. If you rename it, you must
also update `Dockerfile` (the WAR copy path),
`src/servlets/EmployeeLoginFilter.java` (the allowlisted URIs), and
`ingress-multi.yaml` (the routing paths) — they all derive from it.
