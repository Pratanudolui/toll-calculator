package com.freightfox.tollplaza.loader;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TollPlazaCsvParserTest {

    private final TollPlazaCsvParser parser = new TollPlazaCsvParser();

    @Test
    void parse_validRows_returnsAllRecords() {
        String csv = """
                longitude,latitude,toll_name,geo_state
                77.485,23.1415,11 Miles Toll Plaza,Madhya Pradesh
                72.965821,19.1530141,Airoli Toll Plaza,Maharashtra
                """;

        List<TollPlazaRecord> records = parser.parse(toStream(csv));

        assertThat(records).hasSize(2);
        assertThat(records.get(0).name()).isEqualTo("11 Miles Toll Plaza");
        assertThat(records.get(0).latitude()).isEqualTo(23.1415);
        assertThat(records.get(0).longitude()).isEqualTo(77.485);
        assertThat(records.get(0).geoState()).isEqualTo("Madhya Pradesh");
    }

    @Test
    void parse_skipsRowsWithBlankOrNanName() {
        String csv = """
                longitude,latitude,toll_name,geo_state
                82.9424053,21.2997706,nan,Chhattisgarh
                77.485,23.1415,,Madhya Pradesh
                72.965821,19.1530141,Airoli Toll Plaza,Maharashtra
                """;

        List<TollPlazaRecord> records = parser.parse(toStream(csv));

        assertThat(records).hasSize(1);
        assertThat(records.get(0).name()).isEqualTo("Airoli Toll Plaza");
    }

    @Test
    void parse_skipsRowsWithUnparsableCoordinates() {
        String csv = """
                longitude,latitude,toll_name,geo_state
                not-a-number,21.2997706,Broken Row Toll Plaza,Chhattisgarh
                72.965821,19.1530141,Airoli Toll Plaza,Maharashtra
                """;

        List<TollPlazaRecord> records = parser.parse(toStream(csv));

        assertThat(records).hasSize(1);
        assertThat(records.get(0).name()).isEqualTo("Airoli Toll Plaza");
    }

    @Test
    void parse_handlesQuotedNamesContainingCommas() {
        String csv = """
                longitude,latitude,toll_name,geo_state
                77.5193241,28.7040936,"Eastern Peripheral Expy, Kallu Garhi Toll Plaza",Uttar Pradesh
                """;

        List<TollPlazaRecord> records = parser.parse(toStream(csv));

        assertThat(records).hasSize(1);
        assertThat(records.get(0).name()).isEqualTo("Eastern Peripheral Expy, Kallu Garhi Toll Plaza");
    }

    private InputStream toStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }
}
