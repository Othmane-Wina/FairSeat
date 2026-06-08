package com.pfa.fairseatticket.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private Long bookingId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false, unique = true)
    private String ticketSecret; // The seed for the TOTP QR Code

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

}