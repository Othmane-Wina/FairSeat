package com.pfa.fairseatdiscovery.config;

import com.pfa.fairseatdiscovery.domain.Game;
import com.pfa.fairseatdiscovery.domain.GameStatus;
import com.pfa.fairseatdiscovery.domain.Stadium;
import com.pfa.fairseatdiscovery.repository.GameRepository;
import com.pfa.fairseatdiscovery.repository.StadiumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final StadiumRepository stadiumRepository;
    private final GameRepository gameRepository;

    @Override
    public void run(String... args) {
        if (stadiumRepository.count() == 0) {
            log.info("Database is empty. Seeding initial stadium and game data...");

            // 1. Seed Stadiums
            Stadium mohammedV = Stadium.builder()
                    .name("Stade Mohammed V")
                    .city("Casablanca")
                    .totalCapacity(45000)
                    .build();

            Stadium moulayAbdallah = Stadium.builder()
                    .name("Stade Prince Moulay Abdellah")
                    .city("Rabat")
                    .totalCapacity(53000)
                    .build();

            stadiumRepository.saveAll(List.of(mohammedV, moulayAbdallah));

            // 2. Seed Games
            Game derby = Game.builder()
                    .title("Raja Casablanca vs Wydad AC")
                    .description("The legendary Casablanca Derby.")
                    .eventDateTime(LocalDateTime.now().plusDays(7))
                    .stadium(mohammedV)
                    .basePrice(new BigDecimal("50.00"))
                    .status(GameStatus.SCHEDULED) // Updated here
                    .build();

            Game classic = Game.builder()
                    .title("AS FAR vs AS Sale")
                    .description("Regional classic match-up.")
                    .eventDateTime(LocalDateTime.now().plusDays(10))
                    .stadium(moulayAbdallah)
                    .basePrice(new BigDecimal("40.00"))
                    .status(GameStatus.SCHEDULED) // Updated here
                    .build();

            gameRepository.saveAll(List.of(derby, classic));
            log.info("Successfully seeded mock data into discovery_db!");
        }
    }
}
