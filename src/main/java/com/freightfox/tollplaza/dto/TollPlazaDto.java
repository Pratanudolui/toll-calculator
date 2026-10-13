package com.freightfox.tollplaza.dto;

public record TollPlazaDto(
        String name,
        double latitude,
        double longitude,
        long distanceFromSource
) {
}
