package tqs.samuelvinhas.HW1.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import tqs.samuelvinhas.HW1.data.ServiceRequest.REQUEST_STATE;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for ZeroMonosRepository
 * Tests JPA queries with real database (PostgreSQL)
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("ZeroMonos Repository Integration Tests")
class ZeroMonosRepositoryIT {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ZeroMonosRepository repository;

    private ServiceRequest request1;
    private ServiceRequest request2;
    private ServiceRequest request3;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        
        request1 = new ServiceRequest(
            "token-1", 
            "Estremoz", 
            "Rua 1, n3",
            now.plusDays(1).truncatedTo(ChronoUnit.MICROS), // value truncated to micros to avoid precision issues with H2
            "Old sofa"
        );
        
        request2 = new ServiceRequest(
            "token-2", 
            "Estremoz", 
            "Rua B, n6",
            now.plusDays(2).truncatedTo(ChronoUnit.MICROS),
            "Broken fridge"
        );
        request2.setState(REQUEST_STATE.ASSIGNED);
        
        request3 = new ServiceRequest(
            "token-3", 
            "Aveiro", 
            "Avenida C, n7",
            now.plusDays(1).truncatedTo(ChronoUnit.MICROS),
            "Old mattress"
        );
        request3.setState(REQUEST_STATE.ASSIGNED);
    }

    @Test
    @DisplayName("When find by municipality and time slot, then return matching request")
    void whenFindByMunicipalityAndTimeSlot_thenReturnServiceRequest() {
        // Arrange
        entityManager.persistAndFlush(request1);
        entityManager.persistAndFlush(request2);

        // Act
        Optional<ServiceRequest> found = repository.findByMunicipalityAndTimeSlot(
            "Estremoz", 
            request1.getTimeSlot()
        );

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("token-1");
        assertThat(found.get().getMunicipality()).isEqualTo("Estremoz");
        assertThat(found.get().getItemDescription()).isEqualTo("Old sofa");
    }

    @Test
    @DisplayName("When find by municipality and time slot with no match, then return empty")
    void whenFindByMunicipalityAndTimeSlot_withNoMatch_thenReturnEmpty() {
        // Arrange
        entityManager.persistAndFlush(request1);

        // Act
        Optional<ServiceRequest> found = repository.findByMunicipalityAndTimeSlot(
            "Estremoz", 
            now.plusDays(10) // non-existent time slot
        );

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("When find by municipality, then return all from that municipality")
    void whenFindByMunicipality_thenReturnAllFromMunicipality() {
        // Arrange
        entityManager.persistAndFlush(request1);
        entityManager.persistAndFlush(request2);
        entityManager.persistAndFlush(request3);

        // Act
        List<ServiceRequest> estremozRequests = repository.findByMunicipality("Estremoz");

        // Assert
        assertThat(estremozRequests).hasSize(2);
        assertThat(estremozRequests)
            .extracting(ServiceRequest::getMunicipality)
            .containsOnly("Estremoz");
        assertThat(estremozRequests)
            .extracting(ServiceRequest::getToken)
            .containsExactlyInAnyOrder("token-1", "token-2");
    }

    @Test
    @DisplayName("When find by municipality with no match, then return empty list")
    void whenFindByMunicipality_withNoMatch_thenReturnEmptyList() {
        // Arrange
        entityManager.persistAndFlush(request1);

        // Act
        List<ServiceRequest> found = repository.findByMunicipality("Faro"); // no requests for Faro

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("When save service request, then can find by id")
    void whenSaveServiceRequest_thenCanFindById() {
        // Act
        ServiceRequest saved = repository.save(request1);
        entityManager.flush();

        // Assert
        Optional<ServiceRequest> found = repository.findById(saved.getToken());
        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("token-1");
        assertThat(found.get().getItemDescription()).isEqualTo("Old sofa");
        assertThat(found.get().getState()).isEqualTo(REQUEST_STATE.RECEIVED);
    }

    @Test
    @DisplayName("When delete service request, then no longer exists")
    void whenDeleteServiceRequest_thenNoLongerExists() {
        // Arrange
        entityManager.persistAndFlush(request1);
        String token = request1.getToken();

        // Act
        repository.deleteById(token);
        entityManager.flush();

        // Assert
        Optional<ServiceRequest> found = repository.findById(token);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("When find all, then return all service requests")
    void whenFindAll_thenReturnAllServiceRequests() {
        // Arrange
        entityManager.persistAndFlush(request1);
        entityManager.persistAndFlush(request2);
        entityManager.persistAndFlush(request3);

        // Act
        List<ServiceRequest> allRequests = repository.findAll();

        // Assert
        assertThat(allRequests).hasSize(3);
        assertThat(allRequests)
            .extracting(ServiceRequest::getToken)
            .containsExactlyInAnyOrder("token-1", "token-2", "token-3");
    }

    @Test
    @DisplayName("When update service request, then changes are persisted")
    void whenUpdateServiceRequest_thenChangesArePersisted() {
        // Arrange
        entityManager.persistAndFlush(request1);
        
        // Act
        ServiceRequest toUpdate = repository.findById("token-1").get();
        toUpdate.setState(REQUEST_STATE.ASSIGNED);
        toUpdate.setItemDescription("Small old sofa");
        repository.save(toUpdate);
        entityManager.flush();
        entityManager.clear(); // Clear persistence context to force fresh read

        // Assert
        ServiceRequest updated = repository.findById("token-1").get();
        assertThat(updated.getState()).isEqualTo(REQUEST_STATE.ASSIGNED);
        assertThat(updated.getItemDescription()).isEqualTo("Small old sofa");
    }

    @Test
    @DisplayName("When multiple requests for different municipalities, then queries filter correctly")
    void whenMultipleRequestsDifferentMunicipalities_thenQueriesFilterCorrectly() {
        // Arrange
        entityManager.persistAndFlush(request1);
        entityManager.persistAndFlush(request2);
        entityManager.persistAndFlush(request3);

        // Act
        List<ServiceRequest> estremozRequests = repository.findByMunicipality("Estremoz");
        List<ServiceRequest> aveiroRequests = repository.findByMunicipality("Aveiro");

        // Assert
        assertThat(estremozRequests).hasSize(2);
        assertThat(aveiroRequests).hasSize(1);
        assertThat(aveiroRequests.get(0).getToken()).isEqualTo("token-3");
    }

    @Test
    @DisplayName("When save request with all fields, then all fields are persisted")
    void whenSaveRequestWithAllFields_thenAllFieldsPersisted() {
        // Act
        ServiceRequest saved = repository.save(request1);
        entityManager.flush();
        entityManager.clear();

        // Assert
        ServiceRequest found = repository.findById(saved.getToken()).get();
        assertThat(found.getToken()).isEqualTo("token-1");
        assertThat(found.getMunicipality()).isEqualTo("Estremoz");
        assertThat(found.getAddress()).isEqualTo("Rua 1, n3");
        assertThat(found.getItemDescription()).isEqualTo("Old sofa");
        assertThat(found.getState()).isEqualTo(REQUEST_STATE.RECEIVED);
        assertThat(found.getTimeSlot()).isNotNull();
        assertThat(found.getDate()).isNotNull();
    }
}