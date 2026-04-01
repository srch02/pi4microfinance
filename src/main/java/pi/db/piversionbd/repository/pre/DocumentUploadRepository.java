package pi.db.piversionbd.repository.pre;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pi.db.piversionbd.entities.pre.DocumentUpload;

import java.util.List;

public interface DocumentUploadRepository extends JpaRepository<DocumentUpload, Long> {
    List<DocumentUpload> findByPreRegistration_Id(Long preRegistrationId);
    List<DocumentUpload> findByIdIn(List<Long> ids);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE DocumentUpload d SET d.claim = null WHERE d.claim.id = :claimId")
    void unlinkFromClaim(@Param("claimId") Long claimId);
}