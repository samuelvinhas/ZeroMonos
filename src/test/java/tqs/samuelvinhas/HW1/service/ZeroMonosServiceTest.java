package tqs.samuelvinhas.HW1.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tqs.samuelvinhas.HW1.data.ServiceRequest;
import tqs.samuelvinhas.HW1.data.ZeroMonosRepository;
import tqs.samuelvinhas.HW1.data.ServiceRequest.REQUEST_STATE;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZeroMonosServiceTest {

    @Mock
    private ZeroMonosRepository repository;

    @InjectMocks
    private ZeroMonosService service;

    private ServiceRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new ServiceRequest();
        validRequest.setMunicipality("Estremoz");
        validRequest.setTimeSlot(LocalDateTime.now().plusDays(1));
        validRequest.setItemDescription("Old mattress");
        validRequest.setAddress("Rua Principal, n12");
        validRequest.setState(REQUEST_STATE.RECEIVED);
        validRequest.setDate(LocalDateTime.now());
        validRequest.setToken("valid-token");
    }

    @Test
    @DisplayName("When requesting service for an empty TimeSlot, then return token")
    void whenRequestServiceWithValidData_thenReturnToken() {
        // Arrange
        when(repository.findByMunicipalityAndTimeSlot(anyString(), any(LocalDateTime.class)))
            .thenReturn(Optional.empty());
        when(repository.save(any(ServiceRequest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String token = service.requestService(validRequest);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        verify(repository, times(1)).save(any(ServiceRequest.class));
    }

    @Test
    @DisplayName("When requesting service with an already booked time slot, then throw exception")
    void whenTimeSlotAlreadyBooked_thenThrowException() {
        // Arrange
        ServiceRequest existingRequest = new ServiceRequest();
        existingRequest.setToken("existing-token");
        when(repository.findByMunicipalityAndTimeSlot(anyString(), any(LocalDateTime.class)))
            .thenReturn(Optional.of(existingRequest));

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> service.requestService(validRequest)
        );
        
        assertEquals("Time slot already booked in this municipality.", exception.getMessage());
        verify(repository, never()).save(any(ServiceRequest.class));
    }

    @Test
    @DisplayName("When requesting service with time slot less than 1 hour in advance, then throw exception")
    void whenTimeSlotLessThanOneHour_thenThrowException() {
        // Arrange
        validRequest.setTimeSlot(LocalDateTime.now().plusMinutes(30)); // Less than 1 hour
        when(repository.findByMunicipalityAndTimeSlot(anyString(), any(LocalDateTime.class)))
            .thenReturn(Optional.empty());

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> service.requestService(validRequest)
        );
        
        assertEquals("Pick a time slot with at least 1 hour in advance.", exception.getMessage());
    }

    @Test
    @DisplayName("When getting service request with valid token, then return request")
    void whenGetServiceRequestWithValidToken_thenReturnRequest() {
        // Arrange
        String token = validRequest.getToken();
        when(repository.findById(token)).thenReturn(Optional.of(validRequest));

        // Act
        Optional<ServiceRequest> result = service.getServiceRequest(token);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(token, result.get().getToken());
        verify(repository, times(1)).findById(token);
    }

    @Test
    @DisplayName("When getting service request with invalid token, then return empty")
    void whenGetServiceRequestWithInvalidToken_thenReturnEmpty() {
        // Arrange
        String token = "invalid-token";
        when(repository.findById(token)).thenReturn(Optional.empty());

        // Act
        Optional<ServiceRequest> result = service.getServiceRequest(token);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("When getting all service requests, then return list")
    void whenGetAllServiceRequests_thenReturnList() {
        // Arrange
        List<ServiceRequest> requests = Arrays.asList(
            new ServiceRequest(),
            new ServiceRequest()
        );
        when(repository.findAll()).thenReturn(requests);

        // Act
        List<ServiceRequest> result = service.getAllServiceRequests();

        // Assert
        assertEquals(2, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("When updating existing request with a new valid TimeSlot, then return token")
    void whenUpdateExistingRequestWithNewValidTimeSlot_thenReturnToken() {
        // Arrange
        String token = validRequest.getToken();
        when(repository.findById(token)).thenReturn(Optional.of(validRequest));
        when(repository.findByMunicipalityAndTimeSlot(anyString(), any(LocalDateTime.class)))
            .thenReturn(Optional.empty());
        when(repository.save(any(ServiceRequest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceRequest updatedRequest = new ServiceRequest();
        updatedRequest.setMunicipality("Estremoz");
        updatedRequest.setTimeSlot(LocalDateTime.now().plusDays(2));

        // Act
        String resultToken = service.updateServiceRequest(token, updatedRequest);

        // Assert
        assertEquals(token, resultToken);
        verify(repository, times(1)).save(any(ServiceRequest.class));
        verify(repository).save(argThat(req -> req.getTimeSlot().isEqual(updatedRequest.getTimeSlot())));
    }

    @Test
    @DisplayName("When updating non-existing request, then throw exception")
    void whenUpdateNonExistingRequest_thenThrowException() {
        // Arrange
        String token = "invalid-token";
        when(repository.findById(token)).thenReturn(Optional.empty());
        ServiceRequest updatedRequest = new ServiceRequest();

        // Act & Assert
        NoSuchElementException exception = assertThrows(
            NoSuchElementException.class,
            () -> service.updateServiceRequest(token, updatedRequest)
        );

        assertEquals("Service request with token " + token + " not found.", exception.getMessage());
        verify(repository, never()).save(any(ServiceRequest.class));
    }

    @Test
    @DisplayName("When update to conflicting time slot, then throw exception")
    void whenUpdateToConflictingTimeSlot_thenThrowException() {
        // Arrange
        String token = validRequest.getToken();
        String token2 = "token-2";
        LocalDateTime conflictingTime = LocalDateTime.now().plusDays(2);
        
        ServiceRequest request2 = new ServiceRequest();
        request2.setToken(token2);
        request2.setMunicipality(validRequest.getMunicipality());
        request2.setTimeSlot(conflictingTime);
        
        ServiceRequest updateRequest = new ServiceRequest();
        updateRequest.setMunicipality(validRequest.getMunicipality());
        updateRequest.setTimeSlot(conflictingTime);
        updateRequest.setAddress(validRequest.getAddress());

        when(repository.findById(validRequest.getToken())).thenReturn(Optional.of(validRequest));
        when(repository.findByMunicipalityAndTimeSlot(validRequest.getMunicipality(), conflictingTime))
            .thenReturn(Optional.of(request2));

        // Act & Assert
        assertThrows(
            IllegalStateException.class,
            () -> service.updateServiceRequest(token, updateRequest)
        );
    }

    @Test
    @DisplayName("When update to a time slot less than 1 hour in advance, then throw exception")
    void whenUpdateToTimeSlotLessThanOneHour_thenThrowException() {
        // Arrange
        String token = validRequest.getToken();
        ServiceRequest updateRequest = new ServiceRequest();
        updateRequest.setMunicipality(validRequest.getMunicipality());
        updateRequest.setTimeSlot(LocalDateTime.now().plusMinutes(30));

        when(repository.findById(token)).thenReturn(Optional.of(validRequest));
        when(repository.findByMunicipalityAndTimeSlot(anyString(), any(LocalDateTime.class)))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> service.updateServiceRequest(token, updateRequest)
        );
        assertEquals("Pick a time slot with at least 1 hour in advance.", exception.getMessage());
    }
        
    @ParameterizedTest
    @EnumSource(REQUEST_STATE.class)
    @DisplayName("When update to different states, then state is updated")
    void whenUpdateToAllStates_thenStateUpdated(REQUEST_STATE newState) {
        // Arrange
        String token = validRequest.getToken();
        ServiceRequest updated = new ServiceRequest();
        updated.setMunicipality(validRequest.getMunicipality());
        updated.setTimeSlot(validRequest.getTimeSlot());
        updated.setAddress(validRequest.getAddress());
        updated.setState(newState);

        when(repository.findById(token)).thenReturn(Optional.of(validRequest));
        when(repository.findByMunicipalityAndTimeSlot(anyString(), any()))
            .thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        String resultToken = service.updateServiceRequest(token, updated);

        // Assert
        assertEquals(token, resultToken);
        verify(repository, times(1)).save(any(ServiceRequest.class));
        verify(repository).save(argThat(req -> req.getState() == newState));
    }

    @Test
    @DisplayName("When deleting existing request, then succeed")
    void whenDeleteExistingRequest_thenSucceed() {
        // Arrange
        String token = "valid-token";
        ServiceRequest request = new ServiceRequest();
        request.setToken(token);
        when(repository.findById(token)).thenReturn(Optional.of(request));

        // Act
        service.deleteServiceRequest(token);

        // Assert
        verify(repository, times(1)).deleteById(token);
    }

    @Test
    @DisplayName("When deleting non-existing request, then throw exception")
    void whenDeleteNonExistingRequest_thenThrowException() {
        // Arrange
        String token = "invalid-token";
        when(repository.findById(token)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> service.deleteServiceRequest(token));
        verify(repository, never()).deleteById(anyString());
    }
}