package com.freightfox.tollplaza.dto;

public record RouteDto(
        String sourcePincode,
        String destinationPincode,
        long distanceInKm
) {
}
