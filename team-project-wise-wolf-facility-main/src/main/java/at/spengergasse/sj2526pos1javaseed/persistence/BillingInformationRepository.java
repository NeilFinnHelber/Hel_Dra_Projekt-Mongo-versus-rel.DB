package at.spengergasse.sj2526pos1javaseed.persistence;

import at.spengergasse.sj2526pos1javaseed.Domain.BillingInformation;
import at.spengergasse.sj2526pos1javaseed.Ids.BillingInformationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface BillingInformationRepository extends JpaRepository<BillingInformation, BillingInformationId> {
}