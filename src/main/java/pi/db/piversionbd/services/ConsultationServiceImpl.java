package pi.db.piversionbd.services;

import pi.db.piversionbd.entities.health.Consultation;
import pi.db.piversionbd.entities.health.EtatConsultation;
import pi.db.piversionbd.repositories.ConsultationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Service
public class ConsultationServiceImpl implements IConsultationService {

    private static final Logger logger = LoggerFactory.getLogger(ConsultationServiceImpl.class);

    @Autowired
    private ConsultationRepository consultationRepository;


    @Override
    public Consultation saveConsultation(Consultation consultation) {
        Consultation savedConsultation = consultationRepository.save(consultation);
        logger.info("Consultation sauvegardée avec succès pour l'ID: " + savedConsultation.getId());
        return savedConsultation;
    }

    @Override
    public Optional<Consultation> getConsultationById(Long id) {
        return consultationRepository.findById(id);
    }

    @Override
    public List<Consultation> getAllConsultations() {
        return consultationRepository.findAll();
    }

    @Override
    public Consultation updateConsultation(Long id, Consultation consultation) {
        Optional<Consultation> existingConsultation = consultationRepository.findById(id);
        if (existingConsultation.isPresent()) {
            Consultation c = existingConsultation.get();
            if (consultation.getMember() != null) {
                c.setMember(consultation.getMember());
            }
            if (consultation.getDoctor() != null) {
                c.setDoctor(consultation.getDoctor());
            }
            if (consultation.getLien() != null) {
                c.setLien(consultation.getLien());
            }
            if (consultation.getTypeConsultation() != null) {
                c.setTypeConsultation(consultation.getTypeConsultation());
            }
            if (consultation.getEtatConsultation() != null) {
                c.setEtatConsultation(consultation.getEtatConsultation());
            }
            if (consultation.getDateConsultation() != null) {
                c.setDateConsultation(consultation.getDateConsultation());
            }
            if (consultation.getNotes() != null) {
                c.setNotes(consultation.getNotes());
            }
            return consultationRepository.save(c);
        }
        return null;
    }

    @Override
    public void deleteConsultation(Long id) {
        consultationRepository.deleteById(id);
    }

    @Override
    public List<Consultation> getConsultationsByMemberId(Long memberId) {
        return consultationRepository.findByMemberId(memberId);
    }

    @Override
    public List<Consultation> getConsultationsByDoctorId(Long doctorId) {
        return consultationRepository.findByDoctorId(doctorId);
    }

    @Override
    public List<Consultation> getConsultationsByEtat(EtatConsultation etatConsultation) {
        return consultationRepository.findByEtatConsultation(etatConsultation);
    }

    @Override
    public List<Consultation> getConsultationsByMemberIdAndEtat(Long memberId, EtatConsultation etatConsultation) {
        return consultationRepository.findByMemberIdAndEtatConsultation(memberId, etatConsultation);
    }
}

