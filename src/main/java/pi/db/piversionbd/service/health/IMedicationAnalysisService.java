package pi.db.piversionbd.service.health;

import org.springframework.web.multipart.MultipartFile;
import pi.db.piversionbd.entities.health.MedicationAnalysis;
import java.util.List;

public interface IMedicationAnalysisService {
    MedicationAnalysis analyzeMedicationImage(MultipartFile imageFile, Long memberId) throws Exception;
    List<MedicationAnalysis> getAnalysisHistoryByMember(Long memberId);
    MedicationAnalysis getAnalysisById(Long analysisId);
    void deleteAnalysis(Long analysisId);
}

