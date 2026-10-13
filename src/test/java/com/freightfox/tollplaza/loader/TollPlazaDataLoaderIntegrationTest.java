package com.freightfox.tollplaza.loader;

import com.freightfox.tollplaza.repository.TollPlazaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The CSV loader already ran once automatically on context startup (as a
 * CommandLineRunner). Running it again here must not duplicate rows — this is
 * the "efficient upsert" requirement from the assignment, and it's the only
 * place the real loader (as opposed to the pure CSV parser) is exercised.
 */
@SpringBootTest
@ActiveProfiles("test")
class TollPlazaDataLoaderIntegrationTest {

    @Autowired
    private TollPlazaDataLoader dataLoader;

    @Autowired
    private TollPlazaRepository tollPlazaRepository;

    @Test
    void runningLoaderAgain_doesNotDuplicateRows() throws Exception {
        long countAfterStartupLoad = tollPlazaRepository.count();
        assertThat(countAfterStartupLoad).isGreaterThan(1000);

        dataLoader.run();
        dataLoader.run();

        assertThat(tollPlazaRepository.count()).isEqualTo(countAfterStartupLoad);
    }
}
