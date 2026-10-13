package com.freightfox.tollplaza.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TollPlazaRequest(

        @Schema(description = "6-digit Indian source pincode", example = "110001")
        @NotBlank(message = "sourcePincode is required")
        @Pattern(regexp = "^[1-9][0-9]{5}$", message = "sourcePincode must be a valid 6-digit Indian pincode")
        String sourcePincode,

        @Schema(description = "6-digit Indian destination pincode", example = "560001")
        @NotBlank(message = "destinationPincode is required")
        @Pattern(regexp = "^[1-9][0-9]{5}$", message = "destinationPincode must be a valid 6-digit Indian pincode")
        String destinationPincode
) {
}
