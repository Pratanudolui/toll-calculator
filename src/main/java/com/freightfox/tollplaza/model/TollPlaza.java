package com.freightfox.tollplaza.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "toll_plaza", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "latitude", "longitude"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TollPlaza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Double latitude;

    private Double longitude;

    private String geoState;
}
