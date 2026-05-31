package com.pfa.fairseatdiscovery.repository;

import com.pfa.fairseatdiscovery.domain.Game;
import com.pfa.fairseatdiscovery.domain.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByStatusOrderByEventDateTimeAsc(GameStatus status);
}