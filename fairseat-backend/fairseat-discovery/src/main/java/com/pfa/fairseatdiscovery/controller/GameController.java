package com.pfa.fairseatdiscovery.controller;

import com.pfa.fairseatdiscovery.dto.GameResponseDTO;
import com.pfa.fairseatdiscovery.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping("/active")
    public ResponseEntity<List<GameResponseDTO>> getActiveGamesCatalog() {
        return ResponseEntity.ok(gameService.getActiveGames());
    }
}