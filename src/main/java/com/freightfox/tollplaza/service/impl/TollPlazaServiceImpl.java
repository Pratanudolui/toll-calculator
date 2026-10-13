package com.freightfox.tollplaza.service.impl;

import com.freightfox.tollplaza.config.CacheConfig;
import com.freightfox.tollplaza.dto.RouteDto;
import com.freightfox.tollplaza.dto.TollPlazaDto;
import com.freightfox.tollplaza.dto.TollPlazaResponse;
import com.freightfox.tollplaza.exception.InvalidPincodeException;
import com.freightfox.tollplaza.exception.SamePincodeException;
import com.freightfox.tollplaza.model.TollPlaza;
import com.freightfox.tollplaza.repository.TollPlazaRepository;
import com.freightfox.tollplaza.service.GeocodingService;
import com.freightfox.tollplaza.service.TollPlazaService;
import com.freightfox.tollplaza.util.Coordinates;
import com.freightfox.tollplaza.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TollPlazaServiceImpl implements TollPlazaService {

    private final GeocodingService geocodingService;
    private final TollPlazaRepository tollPlazaRepository;

    @Value("${toll.corridor.width-km:25}")
    private double corridorWidthKm;

    @Value("${toll.corridor.endpoint-buffer-km:15}")
    private double endpointBufferKm;

    @Override
    @Cacheable(value = CacheConfig.ROUTE_CACHE, key = "#sourcePincode + '-' + #destinationPincode")
    public TollPlazaResponse findTollPlazasBetween(String sourcePincode, String destinationPincode) {
        if (sourcePincode.equals(destinationPincode)) {
            throw new SamePincodeException("Source and destination pincodes cannot be the same");
        }

        Coordinates source = geocodingService.geocode(sourcePincode)
                .orElseThrow(() -> new InvalidPincodeException("Invalid source or destination pincode"));
        Coordinates destination = geocodingService.geocode(destinationPincode)
                .orElseThrow(() -> new InvalidPincodeException("Invalid source or destination pincode"));

        double totalDistanceKm = GeoUtils.haversineKm(source, destination);

        List<TollPlaza> candidates = tollPlazaRepository.findWithinBoundingBox(
                Math.min(source.latitude(), destination.latitude()) - boundingBoxMarginDeg(),
                Math.max(source.latitude(), destination.latitude()) + boundingBoxMarginDeg(),
                Math.min(source.longitude(), destination.longitude()) - boundingBoxMarginDeg(),
                Math.max(source.longitude(), destination.longitude()) + boundingBoxMarginDeg());

        List<TollPlazaDto> tollPlazas = candidates.stream()
                .map(plaza -> toScoredPlaza(plaza, source, destination))
                .filter(scored -> Math.abs(scored.crossTrackKm) <= corridorWidthKm)
                .filter(scored -> scored.alongTrackKm >= -endpointBufferKm
                        && scored.alongTrackKm <= totalDistanceKm + endpointBufferKm)
                .sorted(Comparator.comparingDouble(scored -> scored.alongTrackKm))
                .map(scored -> new TollPlazaDto(
                        scored.plaza.getName(),
                        scored.plaza.getLatitude(),
                        scored.plaza.getLongitude(),
                        // Clamped to 0: a plaza within the endpoint buffer but technically
                        // just "behind" the source pincode's centroid is still reported as
                        // being at the start of the route, not at a confusing negative distance.
                        Math.max(0, Math.round(scored.alongTrackKm))))
                .toList();

        RouteDto route = new RouteDto(sourcePincode, destinationPincode, Math.round(totalDistanceKm));
        return new TollPlazaResponse(route, tollPlazas);
    }

    private double boundingBoxMarginDeg() {
        // ~111 km per degree of latitude; pad generously since longitude degrees
        // shrink at higher latitudes and the route itself is only approximate.
        return (corridorWidthKm / 111.0) + 0.5;
    }

    private ScoredPlaza toScoredPlaza(TollPlaza plaza, Coordinates source, Coordinates destination) {
        Coordinates point = new Coordinates(plaza.getLatitude(), plaza.getLongitude());
        double crossTrackKm = GeoUtils.crossTrackDistanceKm(point, source, destination);
        double alongTrackKm = GeoUtils.alongTrackDistanceKm(point, source, destination);
        return new ScoredPlaza(plaza, crossTrackKm, alongTrackKm);
    }

    private record ScoredPlaza(TollPlaza plaza, double crossTrackKm, double alongTrackKm) {
    }
}
