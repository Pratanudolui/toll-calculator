package com.freightfox.tollplaza.controller;

import com.freightfox.tollplaza.dto.ErrorResponse;
import com.freightfox.tollplaza.dto.TollPlazaRequest;
import com.freightfox.tollplaza.dto.TollPlazaResponse;
import com.freightfox.tollplaza.service.TollPlazaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/toll-plazas")
@RequiredArgsConstructor
@Tag(name = "Toll Plazas", description = "Find toll plazas on the simulated route between two Indian pincodes")
public class TollPlazaController {

    private final TollPlazaService tollPlazaService;

    @PostMapping
    @Operation(
            summary = "Find toll plazas between two pincodes",
            description = "Simulates a route between the source and destination pincodes (as the great-circle "
                    + "distance between their geocoded locations) and returns the toll plazas that fall within "
                    + "a configurable corridor around that route, sorted by distance from the source.")
    @ApiResponse(responseCode = "200", description = "Route and toll plaza details",
            content = @Content(schema = @Schema(implementation = TollPlazaResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request (bad pincode format, unresolvable pincode, "
            + "or identical source/destination)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TollPlazaResponse> getTollPlazas(@Valid @RequestBody TollPlazaRequest request) {
        TollPlazaResponse response = tollPlazaService.findTollPlazasBetween(
                request.sourcePincode(), request.destinationPincode());
        return ResponseEntity.ok(response);
    }
}
