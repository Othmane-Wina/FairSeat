package com.pfa.fairseatmarketplace.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resale_listings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResaleListing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true) // A ticket can only be listed once at a time
    private UUID ticketId;

    @Column(nullable = false)
    private String sellerId;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false)
    private Double askingPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingStatus status;

    @CreationTimestamp
    private LocalDateTime listedAt;

}