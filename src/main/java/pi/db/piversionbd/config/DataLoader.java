package pi.db.piversionbd.config;

import pi.db.piversionbd.entities.health.*;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.repository.health.DoctorRepository;
import pi.db.piversionbd.repository.health.ProduitRepository;
import pi.db.piversionbd.repository.groups.MemberRepository;
import pi.db.piversionbd.repository.health.ConsultationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ConsultationRepository consultationRepository;

    @Override
    public void run(String... args) throws Exception {
        // Load doctors only if the table is empty
        if (doctorRepository.count() == 0) {
            loadDoctors();
        }

        // Load produits only if the table is empty
        if (produitRepository.count() == 0) {
            loadProduits();
        }

        // Load members only if the table is empty
        if (memberRepository.count() == 0) {
            loadMembers();
        }

        System.out.println("Data loaded successfully!");
    }

    /**
     * Method to clean the database
     * Delete data in the correct order to respect foreign key constraints
     */
    public void cleanDatabase() {
        System.out.println("Cleaning database...");
        try {
            consultationRepository.deleteAll();
            System.out.println("Consultations deleted!");

            doctorRepository.deleteAll();
            System.out.println("Doctors deleted!");

            produitRepository.deleteAll();
            System.out.println("Produits deleted!");

            memberRepository.deleteAll();
            System.out.println("Members deleted!");

            System.out.println("Database cleaned successfully!");
        } catch (Exception e) {
            System.out.println("Error cleaning database: " + e.getMessage());
        }
    }

    private void loadDoctors() {
        System.out.println("Loading doctors...");

        // 20 Doctors with different specialties
        Doctor[] doctors = new Doctor[]{
            createDoctor("Dr. Ahmed Chakroun", "ahmed.chakroun@hospital.tn", "password123", TypeDoctor.MEDECINS_GENERALISTES, Specialite.MÉDECINE_GÉNÉRALE),
            createDoctor("Dr. Fatima Bouaziz", "fatima.bouaziz@hospital.tn", "password123", TypeDoctor.MEDECINS_GENERALISTES, Specialite.MÉDECINE_GÉNÉRALE),
            createDoctor("Dr. Mohamed Ben Ali", "mohamed.benali@hospital.tn", "password123", TypeDoctor.DENTISTES, Specialite.DENTISTE),
            createDoctor("Dr. Aisha Mansouri", "aisha.mansouri@hospital.tn", "password123", TypeDoctor.DENTISTES, Specialite.DENTISTE),
            createDoctor("Dr. Youssef Gharbi", "youssef.gharbi@hospital.tn", "password123", TypeDoctor.MEDECINS_GENERALISTES, Specialite.CARDIOLOGIE),
            createDoctor("Dr. Leila Mahboub", "leila.mahboub@hospital.tn", "password123", TypeDoctor.URGENCES, Specialite.TRAUMATOLOGIE),
            createDoctor("Dr. Khalid Khouja", "khalid.khouja@hospital.tn", "password123", TypeDoctor.MEDECINS_GENERALISTES, Specialite.NEUROLOGIE),
            createDoctor("Dr. Salma Rami", "salma.rami@hospital.tn", "password123", TypeDoctor.MEDECINS_GENERALISTES, Specialite.DERMATOLOGIE),
            createDoctor("Dr. Tarek Mansour", "tarek.mansour@hospital.tn", "password123", TypeDoctor.MEDECINS_GENERALISTES, Specialite.ORTHOPÉDIE),
            createDoctor("Dr. Hana Belhadj", "hana.belhadj@hospital.tn", "password123", TypeDoctor.MEDECINS_GENERALISTES, Specialite.PNEUMOLOGIE),
            createDoctor("Dr. Rami Farah", "rami.farah@hospital.tn", "password123", TypeDoctor.MEDECINS_GENERALISTES, Specialite.GASTRO_ENTEROLOGIE),
            createDoctor("Dr. Zainab Triki", "zainab.triki@hospital.tn", "password123", TypeDoctor.MEDECINS_GENERALISTES, Specialite.OPHTALMOLOGIE),
            createDoctor("Dr. Nasim Boukhris", "nasim.boukhris@hospital.tn", "password123", TypeDoctor.MEDECINS_GENERALISTES, Specialite.UROLOGIE),
            createDoctor("Dr. Mariam Guedef", "mariam.guedef@hospital.tn", "password123", TypeDoctor.MEDECINS_GENERALISTES, Specialite.RHUMATOLOGIE),
            createDoctor("Dr. Hassan El Amri", "hassan.elamri@hospital.tn", "password123", TypeDoctor.MEDECINS_GENERALISTES, Specialite.TRAUMATOLOGIE),
            createDoctor("Dr. Noor Karim", "noor.karim@hospital.tn", "password123", TypeDoctor.MEDECINS_GENERALISTES, Specialite.PÉDIATRIE),
            createDoctor("Dr. Jalel Masmoudi", "jalel.masmoudi@hospital.tn", "password123", TypeDoctor.MEDECINS_GENERALISTES, Specialite.GYNÉCOLOGIE),
            createDoctor("Dr. Samira Arfaoui", "samira.arfaoui@hospital.tn", "password123", TypeDoctor.DENTISTES, Specialite.DENTISTE),
            createDoctor("Dr. Fadi Hadj", "fadi.hadj@hospital.tn", "password123", TypeDoctor.URGENCES, Specialite.TRAUMATOLOGIE),
            createDoctor("Dr. Rania Zahra", "rania.zahra@hospital.tn", "password123", TypeDoctor.MEDECINS_GENERALISTES, Specialite.MÉDECINE_GÉNÉRALE)
        };

        for (Doctor doctor : doctors) {
            doctorRepository.save(doctor);
        }

        System.out.println("20 Doctors loaded successfully!");
    }

    private void loadProduits() {
        System.out.println("Loading produits...");

        // 20 Produits with different types and diseases
        Produit[] produits = new Produit[]{
            createProduit("Doliprane", TypeProduit.PARACETAMOL, MaladieProduit.MIGRAINE),
            createProduit("Paracétamol 500mg", TypeProduit.PARACETAMOL, MaladieProduit.MAL_DE_TETE),
            createProduit("Tachipirine", TypeProduit.PARACETAMOL, MaladieProduit.FIEVRE),
            createProduit("Ibuprofène 200mg", TypeProduit.IBUPROFEN, MaladieProduit.DOULEUR_MUSCULAIRE),
            createProduit("Advil", TypeProduit.IBUPROFEN, MaladieProduit.INFLAMMATION),
            createProduit("Aspirine", TypeProduit.ASPIRIN, MaladieProduit.MAL_DE_TETE),
            createProduit("Pénicilline V", TypeProduit.PENICILLINE, MaladieProduit.INFECTION_BACTERIENNE),
            createProduit("Amoxicilline 500mg", TypeProduit.AMOXICILLIN, MaladieProduit.INFECTION_BACTERIENNE),
            createProduit("Cetirizine 10mg", TypeProduit.CETIRIZINE, MaladieProduit.ALLERGIE),
            createProduit("Loratadine", TypeProduit.LORATADINE, MaladieProduit.ALLERGIE),
            createProduit("Oméprazole 20mg", TypeProduit.OMEPRAZOLE, MaladieProduit.ACIDITE),
            createProduit("Metformine 500mg", TypeProduit.METFORMIN, MaladieProduit.HYPERTENSION),
            createProduit("Lisinopril 10mg", TypeProduit.LISINOPRIL, MaladieProduit.HYPERTENSION),
            createProduit("Sirop pour la toux", TypeProduit.PARACETAMOL, MaladieProduit.TOUX),
            createProduit("Remède rhume", TypeProduit.IBUPROFEN, MaladieProduit.RHUME),
            createProduit("Acide acétylsalicylique", TypeProduit.ASPIRIN, MaladieProduit.FIEVRE),
            createProduit("Ibuprofen 400mg", TypeProduit.IBUPROFEN, MaladieProduit.MAL_DE_TETE),
            createProduit("Paracétamol 1000mg", TypeProduit.PARACETAMOL, MaladieProduit.DOULEUR_MUSCULAIRE),
            createProduit("Pénicilline G", TypeProduit.PENICILLINE, MaladieProduit.INFECTION_BACTERIENNE),
            createProduit("Acide mefenanique", TypeProduit.IBUPROFEN, MaladieProduit.MIGRAINE)
        };

        for (Produit produit : produits) {
            produitRepository.save(produit);
        }

        System.out.println("20 Produits loaded successfully!");
    }

    private void loadMembers() {
        System.out.println("Loading members...");

        // 20 Members with different CIN numbers and emails (auth is via AdminUser / portal; Member has no password field)
        Member[] members = new Member[]{
            createMember("12345678", "member1@example.tn"),
            createMember("23456789", "member2@example.tn"),
            createMember("34567890", "member3@example.tn"),
            createMember("45678901", "member4@example.tn"),
            createMember("56789012", "member5@example.tn"),
            createMember("67890123", "member6@example.tn"),
            createMember("78901234", "member7@example.tn"),
            createMember("89012345", "member8@example.tn"),
            createMember("90123456", "member9@example.tn"),
            createMember("01234567", "member10@example.tn"),
            createMember("11223344", "member11@example.tn"),
            createMember("22334455", "member12@example.tn"),
            createMember("33445566", "member13@example.tn"),
            createMember("44556677", "member14@example.tn"),
            createMember("55667788", "member15@example.tn"),
            createMember("66778899", "member16@example.tn"),
            createMember("77889900", "member17@example.tn"),
            createMember("88990011", "member18@example.tn"),
            createMember("99001122", "member19@example.tn"),
            createMember("00112233", "member20@example.tn")
        };

        for (Member member : members) {
            memberRepository.save(member);
        }

        System.out.println("20 Members loaded successfully!");
    }

    private Doctor createDoctor(String name, String email, String password, TypeDoctor typeDoctor, Specialite specialite) {
        Doctor doctor = new Doctor();
        doctor.setName(name);
        doctor.setEmail(email);
        doctor.setPassword(password);
        doctor.setTypeDoctor(typeDoctor);
        doctor.setSpecialite(specialite);
        return doctor;
    }

    private Produit createProduit(String name, TypeProduit typeProduit, MaladieProduit maladieProduit) {
        Produit produit = new Produit();
        produit.setName(name);
        produit.setTypeProduit(typeProduit);
        produit.setMaladieProduit(maladieProduit);
        return produit;
    }

    private Member createMember(String cinNumber, String email) {
        Member member = new Member();
        member.setCinNumber(cinNumber);
        member.setEmail(email);
        member.setPersonalizedMonthlyPrice(50.0f);
        member.setAdherenceScore(0.0f);
        return member;
    }
}

