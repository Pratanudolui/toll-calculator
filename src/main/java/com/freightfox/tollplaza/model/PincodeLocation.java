package com.freightfox.tollplaza.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Caches the geocoded coordinates of a pincode so that repeated lookups
 * do not re-hit the external geocoding provider.
 */
@Entity
@Table(name = "pincode_location")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PincodeLocation {

    @Id
    private String pincode;

    private Double latitude;

    private Double longitude;
}
