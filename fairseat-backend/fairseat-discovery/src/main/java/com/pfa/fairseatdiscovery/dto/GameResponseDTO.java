package com.pfa.fairseatdiscovery.dto;

import com.pfa.fairseatdiscovery.domain.GameStatus;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GameResponseDTO(
        Long id,
        String title,
        String description,
        LocalDateTime eventDateTime,
        StadiumDTO stadium,
        BigDecimal basePrice,
        GameStatus status
) implements Serializable {}
