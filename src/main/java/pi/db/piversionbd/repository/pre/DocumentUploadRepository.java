package pi.db.piversionbd.repository.pre;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pi.db.piversionbd.entities.pre.DocumentUpload;

import java.util.Collection;
import java.util.List;

public interface DocumentUploadRepository extends JpaRepository<DocumentUpload, Long> {

    List<DocumentUpload> findByIdIn(Collection<Long> ids);

    List<DocumentUpload> findByClaim_Id(Long claimId);

    List<DocumentUpload> findByPreRegistration_Id(Long preRegistrationId);

    @Modifying
    @Query("UPDATE DocumentUpload d SET d.claim = null WHERE d.claim.id = :claimId")
    void unlinkFromClaim(Long claimId);
}