package com.pfa.fairseatticket.repository;

import com.pfa.fairseatticket.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    // We'll likely need this later for the mobile app to fetch user tickets
    // List<Ticket> findByUserId(String userId);
}