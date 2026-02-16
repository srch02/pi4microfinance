package pi.db.piversionbd.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.health.Consultation;
import pi.db.piversionbd.entities.health.ConsultationStatus;
import pi.db.piversionbd.entities.health.Doctor;
import pi.db.piversionbd.repositories.ConsultationRepository;
import pi.db.piversionbd.repositories.DoctorRepository;
import pi.db.piversionbd.repositories.MemberRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final DoctorRepository doctorRepository;
    private final MemberRepository memberRepository;

    @Override
    public Consultation bookConsultation(Long memberId, Long doctorId, Consultation consultation) {

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        consultation.setDoctor(doctor);
        consultation.setMember(member);
        consultation.setStatus(ConsultationStatus.SCHEDULED);

        return consultationRepository.save(consultation);
    }

    @Override
    public List<Consultation> getMemberConsultations(Long memberId) {
        return consultationRepository.findByMemberId(memberId);
    }

    @Override
    public List<Consultation> getDoctorConsultations(Long doctorId) {
        return consultationRepository.findByDoctorId(doctorId);
    }

    @Override
    public Consultation updateStatus(Long consultationId, ConsultationStatus status) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new RuntimeException("Consultation not found"));

        consultation.setStatus(status);
        return consultationRepository.save(consultation);
    }

    @Override
    public void deleteConsultation(Long id) {
        consultationRepository.deleteById(id);
    }
}

