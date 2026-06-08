package com.pfa.fairseatmarketplace.repository;

import com.pfa.fairseatmarketplace.domain.ResaleListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ResaleListingRepository extends JpaRepository<ResaleListing, UUID> {
}
