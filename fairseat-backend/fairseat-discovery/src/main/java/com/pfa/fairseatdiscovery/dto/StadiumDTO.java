package com.pfa.fairseatdiscovery.dto;

import java.io.Serializable;

public record StadiumDTO(
        Long id,
        String name,
        String city,
        Integer totalCapacity
) implements Serializable {}