package com.pfa.fairseatdiscovery.repository;

import com.pfa.fairseatdiscovery.domain.Stadium;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StadiumRepository extends JpaRepository<Stadium, Long> {
    // Standard CRUD operations are automatically inherited here
}