package com.freightfox.tollplaza.repository;

import com.freightfox.tollplaza.model.TollPlaza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TollPlazaRepository extends JpaRepository<TollPlaza, Long> {

    /**
     * Narrows the toll plaza scan to a bounding box around the source/destination
     * pair instead of loading the whole table, so the precise (and more expensive)
     * cross-track filtering in the service layer only runs over a small candidate set.
     */
    @Query("SELECT t FROM TollPlaza t WHERE t.latitude BETWEEN :minLat AND :maxLat "
            + "AND t.longitude BETWEEN :minLon AND :maxLon")
    List<TollPlaza> findWithinBoundingBox(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLon") double minLon,
            @Param("maxLon") double maxLon);
}
