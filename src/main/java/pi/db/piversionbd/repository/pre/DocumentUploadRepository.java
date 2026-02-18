package pi.db.piversionbd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pi.db.piversionbd.entities.pre.DocumentUpload;

import java.util.List;

public interface DocumentUploadRepository extends JpaRepository<DocumentUpload, Long> {
    List<DocumentUpload> findByPreRegistration_Id(Long preRegistrationId);
}