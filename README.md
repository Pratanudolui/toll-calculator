# Toll Plaza Finder

Spring Boot REST API that determines the toll plazas located between two Indian
pincodes — built for the FreightFox backend assignment
("Toll Plazas Between Two Pincodes").

## How it works

1. **Geocode** both pincodes to lat/lon using the free OpenStreetMap Nominatim
   search API (no API key required). Results are cached (DB + in-memory) so a
   pincode is only geocoded once.
2. **Simulate the route** as the great-circle (straight-line) path between the
   two points, since no routing API key/budget was assumed available. The
   route's total distance is the haversine distance between the two points.
3. **Find toll plazas on the route**: for every toll plaza within a bounding
   box around the route, compute its perpendicular ("cross-track") distance
   from the straight-line route and its distance along the route from the
   source ("along-track" distance). A plaza is included if:
   - its cross-track distance is within `toll.corridor.width-km` (default 25 km), and
   - its along-track position falls within the route, plus a small endpoint
     buffer (`toll.corridor.endpoint-buffer-km`, default 15 km).
   Matches are sorted by distance from the source.
4. **Cache the whole response** per `(sourcePincode, destinationPincode)` pair
   so a repeated API call for the same route is served instantly.

### Why straight-line simulation?

The assignment explicitly allows "any other external API" and treats the
route as *simulated*; a real routing API (Google Directions, etc.) needs a
paid key and would make the grader's test run non-reproducible without
credentials. The corridor-based approach is a reasonable proxy for "toll
plazas along the route" without one, and the corridor width is a single
config value you can widen/narrow to trade off precision vs. recall against
real-world routes (which bend around cities/highways more than a straight
line does).

## Tech stack

- Java 17, Spring Boot 3.3 (Web, Data JPA, Validation, Cache, Actuator)
- H2 (file-based, so upserted data persists across restarts)
- springdoc-openapi (Swagger UI)
- Apache Commons CSV
- JUnit 5, Mockito, AssertJ, Spring Boot Test

## Running locally

```bash
./mvnw spring-boot:run       # macOS / Linux
mvnw.cmd spring-boot:run     # Windows
```

(Windows users: see `WINDOWS_SETUP.md` for prerequisites and exact commands.)

The app starts on `http://localhost:8080`. On first run it loads
`src/main/resources/data/toll_plaza_india.csv` into H2 via a batched
**upsert** (`MERGE ... KEY(name, latitude, longitude)`), so re-running the
app never duplicates rows.

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/tolldb`, user `sa`, blank password)
- Health check: http://localhost:8080/actuator/health

## API

### `POST /api/v1/toll-plazas`

Request:

```json
{ "sourcePincode": "560064", "destinationPincode": "411045" }
```

Success response (`200`):

```json
{
  "route": { "sourcePincode": "560064", "destinationPincode": "411045", "distanceInKm": 840 },
  "tollPlazas": [
    { "name": "Nelamangala Toll Plaza", "latitude": 13.09, "longitude": 77.4, "distanceFromSource": 32 }
  ]
}
```

Error responses (`400`):

```json
{ "error": "Invalid source or destination pincode" }
{ "error": "Source and destination pincodes cannot be the same" }
```

A route with no toll plazas on it is **not** an error — it's a normal `200`
with `"tollPlazas": []`.

Try it with the bundled `postman_collection.json`, or with curl:

```bash
curl -X POST http://localhost:8080/api/v1/toll-plazas \
  -H "Content-Type: application/json" \
  -d '{"sourcePincode":"560064","destinationPincode":"411045"}'
```

## Testing

```bash
./mvnw test       # macOS / Linux
mvnw.cmd test     # Windows
```

Covers:
- `GeoUtilsTest` — haversine / cross-track / along-track geometry, including the
  "point behind the source" edge case
- `TollPlazaCsvParserTest` — CSV parsing, skips blank/`nan` names and malformed rows
- `TollPlazaDataLoaderIntegrationTest` — running the upsert loader twice never
  duplicates rows
- `NominatimGeocodingServiceTest` — the real geocoding integration: DB cache hit,
  empty/null Nominatim response, network failure, malformed response
- `TollPlazaServiceImplTest` — same-pincode / unresolvable-pincode errors, corridor
  filtering/sorting including near-boundary cases (pure Mockito, no Spring context)
- `TollPlazaServiceCachingTest` — a second call with the same pincode pair does
  **not** re-invoke geocoding or the repository
- `TollPlazaControllerIntegrationTest` — full MockMvc round trip: success,
  validation errors, unresolvable pincode, same pincode, empty-result route,
  malformed/empty request body

A coverage report is generated at `target/site/jacoco/index.html` after running the tests.

## Configuration

| Property | Default | Meaning |
|---|---|---|
| `toll.corridor.width-km` | 25 | Max perpendicular distance from the route for a plaza to count as "on" it |
| `toll.corridor.endpoint-buffer-km` | 15 | Slack beyond the source/destination endpoints |

## Deployment

A `Dockerfile` is included (multi-stage Maven build → JRE runtime image).

Quickest free option — [Render](https://render.com):
1. Push this repo to GitHub.
2. New **Web Service** → connect the repo → Render auto-detects the `Dockerfile`.
3. Expose port `8080`; no environment variables are required.
4. Once deployed, Swagger UI is at `https://<your-service>.onrender.com/swagger-ui.html`.

## Assumptions

- Route is simulated as a straight line between geocoded pincodes (see above);
  no paid routing API is used.
- Geocoding uses OpenStreetMap Nominatim (free, keyless). Nominatim's usage
  policy caps anonymous traffic at ~1 req/sec; this is mitigated by the
  DB + in-memory caching so each pincode is only looked up once ever.
- A toll plaza counts as "on the route" if it's within a configurable
  corridor width of the straight-line route (see Configuration).
- Pincodes must match `^[1-9][0-9]{5}$` (6 digits, not starting with 0); this
  is validated before any geocoding call is made.
