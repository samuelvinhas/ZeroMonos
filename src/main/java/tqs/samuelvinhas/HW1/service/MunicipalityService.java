package tqs.samuelvinhas.HW1.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MunicipalityService {
    private static final Logger logger = LoggerFactory.getLogger(MunicipalityService.class);
    private final RestTemplate restTemplate;
    private static final String MUNICIPALITIES_API = "https://json.geoapi.pt/municipios";

    public MunicipalityService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        logger.info("MunicipalityService initialized");
    }

    public List<String> getAllMunicipalities() {
        logger.info("Fetching municipalities from external API: {}", MUNICIPALITIES_API);
        try {
            String[] municipalities = restTemplate.getForObject(MUNICIPALITIES_API, String[].class);
            logger.info("Successfully fetched {} municipalities", municipalities != null ? municipalities.length : 0);
            return List.of(municipalities);
        } catch (Exception e) {
            logger.error("Error fetching municipalities from API", e);
            throw e;
        }
    }
}
