package com.freightfox.tollplaza.dto;

import java.io.Serializable;
import java.util.List;

public record TollPlazaResponse(
        RouteDto route,
        List<TollPlazaDto> tollPlazas
) implements Serializable {
}
