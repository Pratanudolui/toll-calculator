package com.freightfox.tollplaza.service;

import com.freightfox.tollplaza.dto.TollPlazaResponse;

public interface TollPlazaService {

    /**
     * @throws com.freightfox.tollplaza.exception.SamePincodeException if source and destination are identical
     * @throws com.freightfox.tollplaza.exception.InvalidPincodeException if either pincode cannot be geocoded
     */
    TollPlazaResponse findTollPlazasBetween(String sourcePincode, String destinationPincode);
}
