package com.freightfox.tollplaza.controller;

import com.freightfox.tollplaza.model.TollPlaza;
import com.freightfox.tollplaza.repository.TollPlazaRepository;
import com.freightfox.tollplaza.service.GeocodingService;
import com.freightfox.tollplaza.util.Coordinates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TollPlazaControllerIntegrationTest {

    // Equatorial test route, well away from the real (India-only) toll plaza
    // dataset loaded at startup, so seeded test fixtures are the only candidates.
    private static final Coordinates SOURCE = new Coordinates(0.0, 0.0);
    private static final Coordinates DESTINATION = new Coordinates(0.0, 2.0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TollPlazaRepository tollPlazaRepository;

    @MockBean
    private GeocodingService geocodingService;

    @BeforeEach
    void seedTollPlazas() {
        when(geocodingService.geocode("100001")).thenReturn(Optional.of(SOURCE));
        when(geocodingService.geocode("200002")).thenReturn(Optional.of(DESTINATION));
        when(geocodingService.geocode("999999")).thenReturn(Optional.empty());

        tollPlazaRepository.save(TollPlaza.builder()
                .name("Equator Test Plaza").latitude(0.0).longitude(1.0).geoState("Test").build());
    }

    @Test
    void validRequest_returnsRouteAndTollPlazas() throws Exception {
        String body = """
                {"sourcePincode": "100001", "destinationPincode": "200002"}
                """;

        mockMvc.perform(post("/api/v1/toll-plazas")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route.sourcePincode").value("100001"))
                .andExpect(jsonPath("$.route.destinationPincode").value("200002"))
                .andExpect(jsonPath("$.tollPlazas", hasSize(1)))
                .andExpect(jsonPath("$.tollPlazas[0].name").value("Equator Test Plaza"));
    }

    @Test
    void invalidPincodeFormat_returns400WithValidationMessage() throws Exception {
        String body = """
                {"sourcePincode": "abc", "destinationPincode": "200002"}
                """;

        mockMvc.perform(post("/api/v1/toll-plazas")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void unresolvablePincode_returns400WithInvalidPincodeMessage() throws Exception {
        String body = """
                {"sourcePincode": "999999", "destinationPincode": "200002"}
                """;

        mockMvc.perform(post("/api/v1/toll-plazas")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid source or destination pincode"));
    }

    @Test
    void sameSourceAndDestination_returns400() throws Exception {
        String body = """
                {"sourcePincode": "100001", "destinationPincode": "100001"}
                """;

        mockMvc.perform(post("/api/v1/toll-plazas")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Source and destination pincodes cannot be the same"));
    }

    @Test
    void noTollPlazasOnRoute_returns200WithEmptyList() throws Exception {
        when(geocodingService.geocode("300003")).thenReturn(Optional.of(new Coordinates(40.0, 40.0)));
        when(geocodingService.geocode("400004")).thenReturn(Optional.of(new Coordinates(40.0, 42.0)));

        String body = """
                {"sourcePincode": "300003", "destinationPincode": "400004"}
                """;

        mockMvc.perform(post("/api/v1/toll-plazas")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tollPlazas", hasSize(0)));
    }

    @Test
    void missingField_returns400() throws Exception {
        String body = """
                {"sourcePincode": "100001"}
                """;

        mockMvc.perform(post("/api/v1/toll-plazas")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJson_returns400WithErrorBody() throws Exception {
        String body = "{ this is not valid json ";

        mockMvc.perform(post("/api/v1/toll-plazas")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/toll-plazas")
                        .contentType("application/json")
                        .content(""))
                .andExpect(status().isBadRequest());
    }
}
