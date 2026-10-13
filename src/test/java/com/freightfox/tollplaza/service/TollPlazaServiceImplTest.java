package com.freightfox.tollplaza.service;

import com.freightfox.tollplaza.dto.TollPlazaResponse;
import com.freightfox.tollplaza.exception.InvalidPincodeException;
import com.freightfox.tollplaza.exception.SamePincodeException;
import com.freightfox.tollplaza.model.TollPlaza;
import com.freightfox.tollplaza.repository.TollPlazaRepository;
import com.freightfox.tollplaza.service.impl.TollPlazaServiceImpl;
import com.freightfox.tollplaza.util.Coordinates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TollPlazaServiceImplTest {

    @Mock
    private GeocodingService geocodingService;

    @Mock
    private TollPlazaRepository tollPlazaRepository;

    private TollPlazaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TollPlazaServiceImpl(geocodingService, tollPlazaRepository);
        ReflectionTestUtils.setField(service, "corridorWidthKm", 25.0);
        ReflectionTestUtils.setField(service, "endpointBufferKm", 15.0);
    }

    @Test
    void sameSourceAndDestination_throwsSamePincodeException() {
        assertThatThrownBy(() -> service.findTollPlazasBetween("110001", "110001"))
                .isInstanceOf(SamePincodeException.class)
                .hasMessage("Source and destination pincodes cannot be the same");
    }

    @Test
    void unresolvableSourcePincode_throwsInvalidPincodeException() {
        when(geocodingService.geocode("999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findTollPlazasBetween("999999", "560001"))
                .isInstanceOf(InvalidPincodeException.class)
                .hasMessage("Invalid source or destination pincode");
    }

    @Test
    void unresolvableDestinationPincode_throwsInvalidPincodeException() {
        when(geocodingService.geocode("110001")).thenReturn(Optional.of(new Coordinates(28.63, 77.21)));
        when(geocodingService.geocode("999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findTollPlazasBetween("110001", "999999"))
                .isInstanceOf(InvalidPincodeException.class)
                .hasMessage("Invalid source or destination pincode");
    }

    @Test
    void validRoute_returnsPlazasOnRouteSortedByDistanceFromSource_andExcludesOffRoutePlazas() {
        // Equatorial route: source (0,0) -> destination (0,2), ~222 km total.
        Coordinates source = new Coordinates(0.0, 0.0);
        Coordinates destination = new Coordinates(0.0, 2.0);
        when(geocodingService.geocode("100001")).thenReturn(Optional.of(source));
        when(geocodingService.geocode("200002")).thenReturn(Optional.of(destination));

        TollPlaza onRouteFar = TollPlaza.builder().name("Far Plaza").latitude(0.0).longitude(1.5).geoState("X").build();
        TollPlaza onRouteNear = TollPlaza.builder().name("Near Plaza").latitude(0.0).longitude(0.5).geoState("X").build();
        TollPlaza offRoute = TollPlaza.builder().name("Off Route Plaza").latitude(10.0).longitude(1.0).geoState("X").build();

        when(tollPlazaRepository.findWithinBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(onRouteFar, onRouteNear, offRoute));

        TollPlazaResponse response = service.findTollPlazasBetween("100001", "200002");

        assertThat(response.route().sourcePincode()).isEqualTo("100001");
        assertThat(response.route().destinationPincode()).isEqualTo("200002");
        assertThat(response.route().distanceInKm()).isBetween(220L, 224L);

        assertThat(response.tollPlazas()).extracting("name")
                .containsExactly("Near Plaza", "Far Plaza");
        assertThat(response.tollPlazas().get(0).distanceFromSource())
                .isLessThan(response.tollPlazas().get(1).distanceFromSource());
    }

    @Test
    void plazaBehindSourceButWithinEndpointBuffer_isIncludedAtZeroDistance_notNegative() {
        Coordinates source = new Coordinates(0.0, 0.0);
        Coordinates destination = new Coordinates(0.0, 2.0);
        when(geocodingService.geocode("100001")).thenReturn(Optional.of(source));
        when(geocodingService.geocode("200002")).thenReturn(Optional.of(destination));

        // ~0.05 degrees west of source along the same great-circle line: a few km
        // "behind" the source, but well inside the endpoint buffer.
        TollPlaza justBehindSource = TollPlaza.builder()
                .name("Just Behind Source").latitude(0.0).longitude(-0.05).geoState("X").build();

        when(tollPlazaRepository.findWithinBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(justBehindSource));

        TollPlazaResponse response = service.findTollPlazasBetween("100001", "200002");

        assertThat(response.tollPlazas()).hasSize(1);
        assertThat(response.tollPlazas().get(0).distanceFromSource()).isZero();
    }

    @Test
    void plazaFarBehindSource_isExcluded() {
        Coordinates source = new Coordinates(0.0, 0.0);
        Coordinates destination = new Coordinates(0.0, 2.0);
        when(geocodingService.geocode("100001")).thenReturn(Optional.of(source));
        when(geocodingService.geocode("200002")).thenReturn(Optional.of(destination));

        // ~222km west of source: same line, but well beyond the endpoint buffer.
        TollPlaza farBehindSource = TollPlaza.builder()
                .name("Far Behind Source").latitude(0.0).longitude(-2.0).geoState("X").build();

        when(tollPlazaRepository.findWithinBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(farBehindSource));

        TollPlazaResponse response = service.findTollPlazasBetween("100001", "200002");

        assertThat(response.tollPlazas()).isEmpty();
    }

    @Test
    void plazaJustInsideCorridorWidth_isIncluded() {
        Coordinates source = new Coordinates(0.0, 0.0);
        Coordinates destination = new Coordinates(0.0, 4.0);
        when(geocodingService.geocode("100001")).thenReturn(Optional.of(source));
        when(geocodingService.geocode("200002")).thenReturn(Optional.of(destination));

        // ~0.2 degrees of latitude north of the equatorial route is ~22.2 km of
        // cross-track distance, comfortably inside the 25 km corridor width.
        TollPlaza justInside = TollPlaza.builder()
                .name("Just Inside").latitude(0.2).longitude(2.0).geoState("X").build();

        when(tollPlazaRepository.findWithinBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(justInside));

        TollPlazaResponse response = service.findTollPlazasBetween("100001", "200002");

        assertThat(response.tollPlazas()).extracting("name").containsExactly("Just Inside");
    }

    @Test
    void plazaJustOutsideCorridorWidth_isExcluded() {
        Coordinates source = new Coordinates(0.0, 0.0);
        Coordinates destination = new Coordinates(0.0, 4.0);
        when(geocodingService.geocode("100001")).thenReturn(Optional.of(source));
        when(geocodingService.geocode("200002")).thenReturn(Optional.of(destination));

        // ~0.35 degrees of latitude is ~38.9 km of cross-track distance, outside the
        // 25 km corridor width.
        TollPlaza justOutside = TollPlaza.builder()
                .name("Just Outside").latitude(0.35).longitude(2.0).geoState("X").build();

        when(tollPlazaRepository.findWithinBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(justOutside));

        TollPlazaResponse response = service.findTollPlazasBetween("100001", "200002");

        assertThat(response.tollPlazas()).isEmpty();
    }

    @Test
    void noPlazasWithinCorridor_returnsEmptyListNotError() {
        Coordinates source = new Coordinates(0.0, 0.0);
        Coordinates destination = new Coordinates(0.0, 2.0);
        when(geocodingService.geocode("100001")).thenReturn(Optional.of(source));
        when(geocodingService.geocode("200002")).thenReturn(Optional.of(destination));
        when(tollPlazaRepository.findWithinBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of());

        TollPlazaResponse response = service.findTollPlazasBetween("100001", "200002");

        assertThat(response.tollPlazas()).isEmpty();
        assertThat(response.route().distanceInKm()).isBetween(220L, 224L);
    }
}
