package tqs.samuelvinhas.HW1.data;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "service_requests")
public class ServiceRequest {

    public enum REQUEST_STATE { 
        RECEIVED,
        ASSIGNED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }

    @Id
    private String token;

    @Column(nullable = false)
    private String municipality;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private LocalDateTime timeSlot;

    @Column(nullable = false)
    private String itemDescription;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private REQUEST_STATE state = REQUEST_STATE.RECEIVED;

    @Column(nullable = false)
    private LocalDateTime date;

    public ServiceRequest() {
        this.date = LocalDateTime.now();
        this.state = REQUEST_STATE.RECEIVED;
    }

    public ServiceRequest(String token, String municipality, String address, LocalDateTime timeSlot, String itemDescription) {
        this.token = token;
        this.municipality = municipality;
        this.address = address;
        this.timeSlot = timeSlot;
        this.itemDescription = itemDescription;
        
        this.state = REQUEST_STATE.RECEIVED;
        this.date = LocalDateTime.now();
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getMunicipality() { return municipality; }
    public void setMunicipality(String municipality) { this.municipality = municipality; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LocalDateTime getTimeSlot() { return timeSlot; }
    public void setTimeSlot(LocalDateTime timeSlot) { this.timeSlot = timeSlot; }

    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

    public REQUEST_STATE getState() { return state; }
    public void setState(REQUEST_STATE state) { this.state = state; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

}
