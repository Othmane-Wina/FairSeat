package com.pfa.fairseatdiscovery.mapper;

import com.pfa.fairseatdiscovery.domain.Game;
import com.pfa.fairseatdiscovery.dto.GameResponseDTO;
import com.pfa.fairseatdiscovery.dto.StadiumDTO;
import org.springframework.stereotype.Component;

@Component
public class GameMapper {

    public GameResponseDTO toResponseDTO(Game game) {
        if (game == null) {
            return null;
        }

        StadiumDTO stadiumDTO = null;
        if (game.getStadium() != null) {
            stadiumDTO = new StadiumDTO(
                    game.getStadium().getId(),
                    game.getStadium().getName(),
                    game.getStadium().getCity(),
                    game.getStadium().getTotalCapacity()
            );
        }

        return new GameResponseDTO(
                game.getId(),
                game.getTitle(),
                game.getDescription(),
                game.getEventDateTime(),
                stadiumDTO,
                game.getBasePrice(),
                game.getStatus()
        );
    }
}