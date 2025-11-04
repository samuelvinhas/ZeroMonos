package tqs.samuelvinhas.HW1.service;

import org.springframework.stereotype.Service;

import tqs.samuelvinhas.HW1.data.ServiceRequest;
import tqs.samuelvinhas.HW1.data.ZeroMonosRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ZeroMonosService {
    private final ZeroMonosRepository repository;

    public ZeroMonosService(ZeroMonosRepository repository) {
        this.repository = repository;
    }

    public String requestService(ServiceRequest request) {
        Optional<ServiceRequest> existingRequest = repository.findByMunicipalityAndTimeSlot(
                request.getMunicipality(), 
                request.getTimeSlot()
        );

        if (existingRequest.isPresent()) {
            throw new IllegalStateException("Time slot already booked in this municipality.");
        }

        LocalDateTime now = LocalDateTime.now();

        if (request.getTimeSlot().isBefore(now.plusHours(1))) {
            throw new IllegalStateException("Pick a time slot with at least 1 hour in advance.");
        }

        String token = UUID.randomUUID().toString();
        request.setToken(token);

        repository.save(request);

        return token;

    }

    public Optional<ServiceRequest> getServiceRequest(String token) {
        return repository.findById(token);
    }

    public List<ServiceRequest> getAllServiceRequests() {
        return repository.findAll();
    }

    public List<ServiceRequest> getServiceRequestsByMunicipality(String municipality) {
        return repository.findByMunicipality(municipality);
    }

    public String updateServiceRequest(String token, ServiceRequest updatedRequest) {
        Optional<ServiceRequest> existingRequestOpt = repository.findById(token);
        if (existingRequestOpt.isEmpty()) {
            throw new NoSuchElementException("Service request with token " + token + " not found.");
        }

        ServiceRequest existingRequest = existingRequestOpt.get();

        Optional<ServiceRequest> conflictingRequestOpt = repository.findByMunicipalityAndTimeSlot(
                updatedRequest.getMunicipality(),
                updatedRequest.getTimeSlot()
        );

        if (conflictingRequestOpt.isPresent()) {
            ServiceRequest conflictingRequest = conflictingRequestOpt.get();

            if (!conflictingRequest.getToken().equals(token)) {
                throw new IllegalStateException("Time slot already booked in this municipality.");
            }
        }

        if (!updatedRequest.getTimeSlot().isEqual(existingRequest.getTimeSlot()) && updatedRequest.getTimeSlot().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new IllegalStateException("Pick a time slot with at least 1 hour in advance.");
        }

        existingRequest.setMunicipality(updatedRequest.getMunicipality());
        existingRequest.setAddress(updatedRequest.getAddress());
        existingRequest.setTimeSlot(updatedRequest.getTimeSlot());
        existingRequest.setState(updatedRequest.getState());

        repository.save(existingRequest);

        return token;
    }

    public void deleteServiceRequest(String token) {
        Optional<ServiceRequest> existingRequestOpt = repository.findById(token);
        if (existingRequestOpt.isEmpty()) {
            throw new NoSuchElementException("Service request with token " + token + " not found.");
        }

        repository.deleteById(token);
    }
}
