package pi.db.piversionbd.service.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pi.db.piversionbd.entities.admin.RetentionIntervention;
import pi.db.piversionbd.repository.admin.RetentionInterventionRepository;

import java.util.List;
import java.util.Optional;

@Service
public class RetentionInterventionService {

    @Autowired
    private RetentionInterventionRepository retentionInterventionRepository;

    // ✅ Get all interventions
    public List<RetentionIntervention> getAllInterventions() {
        return retentionInterventionRepository.findAll();
    }

    // ✅ Get intervention by ID
    public Optional<RetentionIntervention> getInterventionById(Long id) {
        return retentionInterventionRepository.findById(id);
    }

    // ✅ Create intervention
    public RetentionIntervention createIntervention(RetentionIntervention intervention) {
        return retentionInterventionRepository.save(intervention);
    }

    // ✅ Update intervention
    public RetentionIntervention updateIntervention(Long id, RetentionIntervention interventionDetails) {

        Optional<RetentionIntervention> optional = retentionInterventionRepository.findById(id);

        if (optional.isPresent()) {

            RetentionIntervention intervention = optional.get();

            intervention.setMember(interventionDetails.getMember());
            intervention.setPrediction(interventionDetails.getPrediction());
            intervention.setActionType(interventionDetails.getActionType());
            intervention.setDiscountPercentage(interventionDetails.getDiscountPercentage());
            intervention.setExecuted(interventionDetails.getExecuted());
            intervention.setResult(interventionDetails.getResult());

            return retentionInterventionRepository.save(intervention);
        }

        return null;
    }

    // ✅ Delete intervention
    public boolean deleteIntervention(Long id) {

        if (retentionInterventionRepository.existsById(id)) {

            retentionInterventionRepository.deleteById(id);

            return true;
        }

        return false;
    }

    // ✅ Find by member ID
    public List<RetentionIntervention> getByMemberId(Long memberId) {
        return retentionInterventionRepository.findByMemberId(memberId);
    }

    // ✅ Find by prediction ID
    public List<RetentionIntervention> getByPredictionId(Long predictionId) {
        return retentionInterventionRepository.findByPredictionId(predictionId);
    }

    // ✅ Find executed interventions
    public List<RetentionIntervention> getByExecuted(Boolean executed) {
        return retentionInterventionRepository.findByExecuted(executed);
    }

}

