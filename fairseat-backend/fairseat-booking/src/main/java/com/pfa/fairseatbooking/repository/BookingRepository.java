package com.pfa.fairseatbooking.repository;

import com.pfa.fairseatbooking.domain.Booking;
import com.pfa.fairseatbooking.domain.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(String userId);

    // Counts all individual seats across all bookings for a user, game, and specific statuses
    @Query("SELECT COUNT(i) FROM Booking b JOIN b.items i WHERE b.userId = :userId AND b.gameId = :gameId AND b.status IN :statuses")
    Long countBookedSeatsByUserAndGame(
            @Param("userId") String userId,
            @Param("gameId") Long gameId,
            @Param("statuses") List<BookingStatus> statuses
    );
}