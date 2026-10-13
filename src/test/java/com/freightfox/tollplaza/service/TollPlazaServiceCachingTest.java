package com.freightfox.tollplaza.service;

import com.freightfox.tollplaza.config.CacheConfig;
import com.freightfox.tollplaza.repository.TollPlazaRepository;
import com.freightfox.tollplaza.service.impl.TollPlazaServiceImpl;
import com.freightfox.tollplaza.util.Coordinates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the "use caching for subsequent API calls" requirement: a second
 * call with the same pincode pair must not re-invoke geocoding or the repository.
 */
@SpringBootTest(classes = {
        TollPlazaServiceCachingTest.CachingTestConfig.class,
        TollPlazaServiceImpl.class
})
@ActiveProfiles("test")
class TollPlazaServiceCachingTest {

    @Configuration
    @EnableCaching
    @Import(CacheConfig.class)
    static class CachingTestConfig {
    }

    @Autowired
    private TollPlazaService tollPlazaService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private GeocodingService geocodingService;

    @MockBean
    private TollPlazaRepository tollPlazaRepository;

    @BeforeEach
    void clearRouteCache() {
        // Each test asserts how many times the route/pincode pair under test is
        // (re)computed; a cache entry left over from a previous test method
        // would make that count unreliable.
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Test
    void secondCallWithSamePincodes_hitsCache_doesNotCallGeocodingOrRepositoryAgain() {
        when(geocodingService.geocode("100001")).thenReturn(Optional.of(new Coordinates(0.0, 0.0)));
        when(geocodingService.geocode("200002")).thenReturn(Optional.of(new Coordinates(0.0, 2.0)));
        when(tollPlazaRepository.findWithinBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of());

        tollPlazaService.findTollPlazasBetween("100001", "200002");
        tollPlazaService.findTollPlazasBetween("100001", "200002");

        verify(geocodingService, times(1)).geocode("100001");
        verify(geocodingService, times(1)).geocode("200002");
        verify(tollPlazaRepository, times(1))
                .findWithinBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void differentPincodePair_isNotServedFromOtherPair_cacheEntry() {
        when(geocodingService.geocode(anyString())).thenAnswer(invocation ->
                Optional.of(new Coordinates(0.0, 0.0)));
        when(tollPlazaRepository.findWithinBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of());

        tollPlazaService.findTollPlazasBetween("100001", "200002");
        tollPlazaService.findTollPlazasBetween("300003", "400004");

        verify(geocodingService, times(1)).geocode("100001");
        verify(geocodingService, times(1)).geocode("300003");
    }
}
