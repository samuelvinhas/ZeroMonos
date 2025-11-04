package tqs.samuelvinhas.HW1.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface ZeroMonosRepository extends JpaRepository<ServiceRequest, String> {

    Optional<ServiceRequest> findByMunicipalityAndTimeSlot(String municipality, LocalDateTime timeSlot);

    List<ServiceRequest> findByMunicipality(String municipality);

}
