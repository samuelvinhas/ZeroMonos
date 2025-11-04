package tqs.samuelvinhas.HW1.boundary;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import tqs.samuelvinhas.HW1.data.ServiceRequest;
import tqs.samuelvinhas.HW1.data.ServiceRequest.REQUEST_STATE;
import tqs.samuelvinhas.HW1.data.ZeroMonosRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Full-stack integration test using REST-Assured
 * Tests the complete API with real HTTP calls and database
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ZeroMonos API Full-Stack Integration Tests")
class ZeroMonosApiIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ZeroMonosRepository repository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        
        // Clean database before each test
        repository.deleteAll();
    }

    // ============ POST /api/bookings ============

    @Test
    @Order(1)
    @DisplayName("POST /api/bookings with valid data returns 201 and token")
    void whenCreateBooking_thenStatus201AndReturnToken() {
        ServiceRequest request = createValidRequest();

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/bookings")
        .then()
            .statusCode(201)
            .body(not(emptyString()))
            .body(matchesPattern("[a-f0-9-]{36}")); // UUID pattern
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/bookings with conflicting time slot returns 400")
    void whenCreateBookingWithConflictingTimeSlot_thenStatus400() {
        // Create first booking
        ServiceRequest request1 = createValidRequest();
        LocalDateTime timeSlot = LocalDateTime.now().plusDays(1);
        request1.setTimeSlot(timeSlot);
        
        given()
            .contentType(ContentType.JSON)
            .body(request1)
            .post("/api/bookings")
            .then()
            .statusCode(201);

        // Try to create conflicting booking (same municipality and time slot)
        ServiceRequest request2 = createValidRequest();
        request2.setTimeSlot(timeSlot);
        request2.setAddress("Different address");

        given()
            .contentType(ContentType.JSON)
            .body(request2)
        .when()
            .post("/api/bookings")
        .then()
            .statusCode(400)
            .body(containsString("Time slot already booked"));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/bookings with time slot less than 1 hour returns 400")
    void whenCreateBookingWithInvalidTimeSlot_thenStatus400() {
        ServiceRequest request = createValidRequest();
        request.setTimeSlot(LocalDateTime.now().plusMinutes(30)); // Less than 1 hour

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/bookings")
        .then()
            .statusCode(400)
            .body(containsString("at least 1 hour in advance"));
    }

    // ============ GET /api/bookings/{token} ============

    @Test
    @Order(4)
    @DisplayName("GET /api/bookings/{token} with valid token returns 200 and booking")
    void whenGetBookingWithValidToken_thenStatus200AndReturnBooking() {
        // First create a booking
        ServiceRequest request = createValidRequest();
        String token = given()
            .contentType(ContentType.JSON)
            .body(request)
            .post("/api/bookings")
            .then()
            .extract()
            .asString();

        // Then retrieve it
        given()
            .pathParam("token", token)
        .when()
            .get("/api/bookings/{token}")
        .then()
            .statusCode(200)
            .body("token", equalTo(token))
            .body("municipality", equalTo("Estremoz"))
            .body("address", equalTo("Rua Principal, n12"))
            .body("itemDescription", equalTo("Old mattress"))
            .body("state", equalTo("RECEIVED"));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/bookings/{token} with invalid token returns 404")
    void whenGetBookingWithInvalidToken_thenStatus404() {
        given()
            .pathParam("token", "invalid-token-xyz")
        .when()
            .get("/api/bookings/{token}")
        .then()
            .statusCode(404);
    }

    // ============ GET /api/bookings ============

    @Test
    @Order(6)
    @DisplayName("GET /api/bookings returns 200 and list of all bookings")
    void whenGetAllBookings_thenStatus200AndReturnList() {
        // Create two bookings
        ServiceRequest request1 = createValidRequest();
        request1.setMunicipality("Estremoz");
        
        ServiceRequest request2 = createValidRequest();
        request2.setMunicipality("Aveiro");
        request2.setTimeSlot(LocalDateTime.now().plusDays(2));

        given().contentType(ContentType.JSON).body(request1).post("/api/bookings");
        given().contentType(ContentType.JSON).body(request2).post("/api/bookings");

        // Get all bookings
        given()
        .when()
            .get("/api/bookings")
        .then()
            .statusCode(200)
            .body("$", hasSize(2))
            .body("municipality", hasItems("Estremoz", "Aveiro"));
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/bookings returns empty list when no bookings")
    void whenGetAllBookingsWithNoData_thenReturnEmptyList() {
        given()
        .when()
            .get("/api/bookings")
        .then()
            .statusCode(200)
            .body("$", hasSize(0));
    }

    // ============ GET /api/bookings/municipality/{municipality} ============

    @Test
    @Order(8)
    @DisplayName("GET /api/bookings/municipality/{municipality} returns filtered list")
    void whenGetBookingsByMunicipality_thenReturnOnlyThatMunicipality() {
        // Create bookings in different municipalities
        ServiceRequest estremozRequest = createValidRequest();
        estremozRequest.setMunicipality("Estremoz");
        
        ServiceRequest aveiroRequest = createValidRequest();
        aveiroRequest.setMunicipality("Aveiro");
        aveiroRequest.setTimeSlot(LocalDateTime.now().plusDays(2));

        given().contentType(ContentType.JSON).body(estremozRequest).post("/api/bookings");
        given().contentType(ContentType.JSON).body(aveiroRequest).post("/api/bookings");

        // Get only Estremoz bookings
        given()
            .pathParam("municipality", "Estremoz")
        .when()
            .get("/api/bookings/municipality/{municipality}")
        .then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("municipality", everyItem(equalTo("Estremoz")));
    }

    @Test
    @Order(9)
    @DisplayName("GET /api/bookings/municipality/{municipality} with no results returns empty list")
    void whenGetBookingsByMunicipalityWithNoResults_thenReturnEmptyList() {
        given()
            .pathParam("municipality", "NonExistentCity")
        .when()
            .get("/api/bookings/municipality/{municipality}")
        .then()
            .statusCode(200)
            .body("$", hasSize(0));
    }

    // ============ PUT /api/bookings/{token} ============

    @Test
    @Order(10)
    @DisplayName("PUT /api/bookings/{token} with valid data returns 200")
    void whenUpdateBooking_thenStatus200() {
        // Create a booking
        ServiceRequest request = createValidRequest();
        String token = given()
            .contentType(ContentType.JSON)
            .body(request)
            .post("/api/bookings")
            .then()
            .extract()
            .asString();

        // Update its state and address
        request.setState(REQUEST_STATE.ASSIGNED);
        request.setAddress("Updated Address, n99");
        
        given()
            .pathParam("token", token)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .put("/api/bookings/{token}")
        .then()
            .statusCode(200)
            .body(equalTo(token));

        // Verify the update
        given()
            .pathParam("token", token)
        .when()
            .get("/api/bookings/{token}")
        .then()
            .statusCode(200)
            .body("state", equalTo("ASSIGNED"))
            .body("address", equalTo("Updated Address, n99"));
    }

    @Test
    @Order(11)
    @DisplayName("PUT /api/bookings/{token} with conflicting time slot returns 400")
    void whenUpdateBookingWithConflictingTimeSlot_thenStatus400() {
        // Create two bookings
        ServiceRequest request1 = createValidRequest();
        String token1 = given()
            .contentType(ContentType.JSON)
            .body(request1)
            .post("/api/bookings")
            .then()
            .extract()
            .asString();

        ServiceRequest request2 = createValidRequest();
        request2.setTimeSlot(LocalDateTime.now().plusDays(2));
        LocalDateTime conflictingTime = request2.getTimeSlot();
        
        given()
            .contentType(ContentType.JSON)
            .body(request2)
            .post("/api/bookings");

        // Try to update first booking with conflicting time
        request1.setTimeSlot(conflictingTime);
        
        given()
            .pathParam("token", token1)
            .contentType(ContentType.JSON)
            .body(request1)
        .when()
            .put("/api/bookings/{token}")
        .then()
            .statusCode(400)
            .body(containsString("Time slot already booked"));
    }

    @Test
    @Order(12)
    @DisplayName("PUT /api/bookings/{token} with invalid token returns 400")
    void whenUpdateNonExistingBooking_thenStatus400() {
        ServiceRequest request = createValidRequest();
        
        given()
            .pathParam("token", "invalid-token")
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .put("/api/bookings/{token}")
        .then()
            .statusCode(400);
    }

    // ============ DELETE /api/bookings/{token} ============

    @Test
    @Order(13)
    @DisplayName("DELETE /api/bookings/{token} with valid token returns 204")
    void whenDeleteBooking_thenStatus204AndNoLongerExists() {
        // Create a booking
        ServiceRequest request = createValidRequest();
        String token = given()
            .contentType(ContentType.JSON)
            .body(request)
            .post("/api/bookings")
            .then()
            .extract()
            .asString();

        // Delete it
        given()
            .pathParam("token", token)
        .when()
            .delete("/api/bookings/{token}")
        .then()
            .statusCode(204);

        // Verify it's gone
        given()
            .pathParam("token", token)
        .when()
            .get("/api/bookings/{token}")
        .then()
            .statusCode(404);
    }

    @Test
    @Order(14)
    @DisplayName("DELETE /api/bookings/{token} with invalid token returns 404")
    void whenDeleteNonExistingBooking_thenStatus404() {
        given()
            .pathParam("token", "invalid-token")
        .when()
            .delete("/api/bookings/{token}")
        .then()
            .statusCode(404);
    }

    // ============ GET /api/municipalities ============

    @Test
    @Order(15)
    @DisplayName("GET /api/municipalities returns 200 and list of municipalities")
    void whenGetMunicipalities_thenStatus200AndReturnList() {
        given()
        .when()
            .get("/api/municipalities")
        .then()
            .statusCode(200)
            .body("$", not(empty()))
            .body("$", hasSize(greaterThan(0)));
    }

    // ============ WORKFLOW TESTS ============

    @Test
    @Order(16)
    @DisplayName("Complete workflow: Create -> Get -> Update -> Delete")
    void completeWorkflow() {
        // 1. Create
        ServiceRequest request = createValidRequest();
        String token = given()
            .contentType(ContentType.JSON)
            .body(request)
            .post("/api/bookings")
            .then()
            .statusCode(201)
            .extract()
            .asString();

        // 2. Get
        given()
            .pathParam("token", token)
            .get("/api/bookings/{token}")
            .then()
            .statusCode(200)
            .body("state", equalTo("RECEIVED"));

        // 3. Update state
        request.setState(REQUEST_STATE.IN_PROGRESS);
        given()
            .pathParam("token", token)
            .contentType(ContentType.JSON)
            .body(request)
            .put("/api/bookings/{token}")
            .then()
            .statusCode(200);

        // 4. Verify update
        given()
            .pathParam("token", token)
            .get("/api/bookings/{token}")
            .then()
            .statusCode(200)
            .body("state", equalTo("IN_PROGRESS"));

        // 5. Delete
        given()
            .pathParam("token", token)
            .delete("/api/bookings/{token}")
            .then()
            .statusCode(204);

        // 6. Verify deletion
        given()
            .pathParam("token", token)
            .get("/api/bookings/{token}")
            .then()
            .statusCode(404);
    }

    @Test
    @Order(17)
    @DisplayName("Multiple bookings for same municipality different time slots")
    void whenMultipleBookingsSameMunicipalityDifferentTimes_thenAllAccepted() {
        ServiceRequest request1 = createValidRequest();
        request1.setTimeSlot(LocalDateTime.now().plusDays(1));
        
        ServiceRequest request2 = createValidRequest();
        request2.setTimeSlot(LocalDateTime.now().plusDays(2));
        
        ServiceRequest request3 = createValidRequest();
        request3.setTimeSlot(LocalDateTime.now().plusDays(3));

        // All should be accepted
        given().contentType(ContentType.JSON).body(request1).post("/api/bookings").then().statusCode(201);
        given().contentType(ContentType.JSON).body(request2).post("/api/bookings").then().statusCode(201);
        given().contentType(ContentType.JSON).body(request3).post("/api/bookings").then().statusCode(201);

        // Verify all are in database
        given()
            .get("/api/bookings/municipality/Estremoz")
            .then()
            .statusCode(200)
            .body("$", hasSize(3));
    }

    // ============ HELPER METHODS ============

    private ServiceRequest createValidRequest() {
        ServiceRequest request = new ServiceRequest();
        request.setMunicipality("Estremoz");
        request.setAddress("Rua Principal, n12");
        request.setTimeSlot(LocalDateTime.now().plusDays(1));
        request.setItemDescription("Old mattress");
        return request;
    }
}