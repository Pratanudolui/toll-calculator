package com.freightfox.tollplaza.repository;

import com.freightfox.tollplaza.model.PincodeLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PincodeLocationRepository extends JpaRepository<PincodeLocation, String> {
}
