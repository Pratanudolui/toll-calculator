package com.freightfox.tollplaza.service.impl;

import com.freightfox.tollplaza.model.PincodeLocation;
import com.freightfox.tollplaza.repository.PincodeLocationRepository;
import com.freightfox.tollplaza.util.Coordinates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the real geocoding integration (every other test in the project
 * mocks the {@link com.freightfox.tollplaza.service.GeocodingService} interface,
 * so this is the only place the Nominatim call / JSON shape / error handling is
 * actually exercised).
 */
@ExtendWith(MockitoExtension.class)
class NominatimGeocodingServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PincodeLocationRepository pincodeLocationRepository;

    private NominatimGeocodingService service;

    @BeforeEach
    void setUp() {
        service = new NominatimGeocodingService(restTemplate, pincodeLocationRepository);
    }

    @Test
    void geocode_alreadyCachedInDb_returnsWithoutCallingNominatim() {
        when(pincodeLocationRepository.findById("110001")).thenReturn(Optional.of(
                PincodeLocation.builder().pincode("110001").latitude(28.63).longitude(77.21).build()));

        Optional<Coordinates> result = service.geocode("110001");

        assertThat(result).contains(new Coordinates(28.63, 77.21));
        verify(restTemplate, never()).getForObject(anyString(), any(Class.class), any(Object[].class));
    }

    @Test
    void geocode_notCached_callsNominatimAndPersistsResult() {
        when(pincodeLocationRepository.findById("560001")).thenReturn(Optional.empty());
        when(restTemplate.getForObject(anyString(), eq(NominatimGeocodingService.NominatimResult[].class), eq("560001")))
                .thenReturn(new NominatimGeocodingService.NominatimResult[]{
                        new NominatimGeocodingService.NominatimResult("12.97", "77.59")
                });

        Optional<Coordinates> result = service.geocode("560001");

        assertThat(result).contains(new Coordinates(12.97, 77.59));

        ArgumentCaptor<PincodeLocation> captor = ArgumentCaptor.forClass(PincodeLocation.class);
        verify(pincodeLocationRepository).save(captor.capture());
        assertThat(captor.getValue().getPincode()).isEqualTo("560001");
        assertThat(captor.getValue().getLatitude()).isEqualTo(12.97);
        assertThat(captor.getValue().getLongitude()).isEqualTo(77.59);
    }

    @Test
    void geocode_nominatimReturnsEmptyArray_returnsEmptyAndDoesNotPersist() {
        when(pincodeLocationRepository.findById("999999")).thenReturn(Optional.empty());
        when(restTemplate.getForObject(anyString(), eq(NominatimGeocodingService.NominatimResult[].class), eq("999999")))
                .thenReturn(new NominatimGeocodingService.NominatimResult[0]);

        Optional<Coordinates> result = service.geocode("999999");

        assertThat(result).isEmpty();
        verify(pincodeLocationRepository, never()).save(any());
    }

    @Test
    void geocode_nominatimReturnsNull_returnsEmpty() {
        when(pincodeLocationRepository.findById("999998")).thenReturn(Optional.empty());
        when(restTemplate.getForObject(anyString(), eq(NominatimGeocodingService.NominatimResult[].class), eq("999998")))
                .thenReturn(null);

        Optional<Coordinates> result = service.geocode("999998");

        assertThat(result).isEmpty();
    }

    @Test
    void geocode_restClientExceptionThrown_returnsEmptyRatherThanPropagating() {
        when(pincodeLocationRepository.findById("700001")).thenReturn(Optional.empty());
        when(restTemplate.getForObject(anyString(), eq(NominatimGeocodingService.NominatimResult[].class), eq("700001")))
                .thenThrow(new RestClientException("network down"));

        Optional<Coordinates> result = service.geocode("700001");

        assertThat(result).isEmpty();
        verify(pincodeLocationRepository, never()).save(any());
    }

    @Test
    void geocode_malformedLatLonInResponse_returnsEmptyRatherThanPropagating() {
        when(pincodeLocationRepository.findById("700002")).thenReturn(Optional.empty());
        when(restTemplate.getForObject(anyString(), eq(NominatimGeocodingService.NominatimResult[].class), eq("700002")))
                .thenReturn(new NominatimGeocodingService.NominatimResult[]{
                        new NominatimGeocodingService.NominatimResult("not-a-number", "77.59")
                });

        Optional<Coordinates> result = service.geocode("700002");

        assertThat(result).isEmpty();
        verify(pincodeLocationRepository, never()).save(any());
    }
}
