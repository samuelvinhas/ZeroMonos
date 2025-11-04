package tqs.samuelvinhas.HW1.boundary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tqs.samuelvinhas.HW1.data.ServiceRequest;
import tqs.samuelvinhas.HW1.service.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ZeroMonosController {

    private static final Logger logger = LoggerFactory.getLogger(ZeroMonosController.class);
    private final ZeroMonosService service;
    private final MunicipalityService municipalityService;

    public ZeroMonosController(ZeroMonosService service, MunicipalityService municipalityService) {
        this.service = service;
        this.municipalityService = municipalityService;
        logger.info("ZeroMonosController initialized");
    }

    @PostMapping("/bookings")
    public ResponseEntity<String> book(@RequestBody ServiceRequest request) {
        logger.info("POST /api/bookings - Creating new booking for municipality: {}", request.getMunicipality());
        try {
            String token = service.requestService(request);
            logger.info("Booking created successfully with token: {}", token);
            return ResponseEntity.status(201).body(token);
        } catch (IllegalStateException e) {
            logger.warn("Failed to create booking: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/bookings/{token}")
    public ResponseEntity<ServiceRequest> getBooking(@PathVariable String token) {
        logger.info("GET /api/bookings/{} - Fetching booking", token);
        Optional<ServiceRequest> request = service.getServiceRequest(token);
        if (request.isPresent()) {
            logger.info("Booking found: {}", token);
            return ResponseEntity.ok(request.get());
        } else {
            logger.warn("Booking not found: {}", token);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<ServiceRequest>> getAllBookings() {
        logger.info("GET /api/bookings - Fetching all bookings");
        List<ServiceRequest> requests = service.getAllServiceRequests();
        logger.info("Found {} bookings", requests.size());
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/bookings/municipality/{municipality}")
    public ResponseEntity<List<ServiceRequest>> getBookingsByMunicipality(@PathVariable String municipality) {
        logger.info("GET /api/bookings/municipality/{} - Fetching bookings by municipality", municipality);
        List<ServiceRequest> requests = service.getServiceRequestsByMunicipality(municipality);
        logger.info("Found {} bookings for municipality: {}", requests.size(), municipality);
        return ResponseEntity.ok(requests);
    }

    @PutMapping("/bookings/{token}")
    public ResponseEntity<String> updateBooking(@PathVariable String token, @RequestBody ServiceRequest updatedRequest) {
        logger.info("PUT /api/bookings/{} - Updating booking", token);
        try {
            String updatedService = service.updateServiceRequest(token, updatedRequest);
            logger.info("Booking updated successfully: {}", token);
            return ResponseEntity.ok(updatedService);
        } catch (NoSuchElementException | IllegalStateException e) {
            logger.warn("Failed to update booking {}: {}", token, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/bookings/{token}")
    public ResponseEntity<Void> deleteBooking(@PathVariable String token) {
        logger.info("DELETE /api/bookings/{} - Deleting booking", token);
        try {
            service.deleteServiceRequest(token);
            logger.info("Booking deleted successfully: {}", token);
            return ResponseEntity.noContent().build();  } 
        catch (NoSuchElementException e) {
            logger.warn("Booking not found for deletion: {}", token);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/municipalities")
    public ResponseEntity<List<String>> getMunicipalities() {
        logger.info("GET /api/municipalities - Fetching municipalities");
        try {
            List<String> municipalities = municipalityService.getAllMunicipalities();
            logger.info("Successfully fetched {} municipalities", municipalities.size());
            return ResponseEntity.ok(municipalities);
        } catch (Exception e) {
            logger.error("Error fetching municipalities", e);
            return ResponseEntity.internalServerError().build();
        }
    }

}
