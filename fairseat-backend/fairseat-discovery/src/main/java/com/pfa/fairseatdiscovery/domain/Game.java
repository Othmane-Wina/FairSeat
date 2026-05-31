package com.pfa.fairseatdiscovery.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title; // e.g., "Real Madrid vs Barcelona"

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDateTime eventDateTime;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stadium_id", nullable = false)
    private Stadium stadium;

    @Column(nullable = false)
    private Double basePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status;
}