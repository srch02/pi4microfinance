package pi.db.piversionbd.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pi.db.piversionbd.entities.health.*;
import pi.db.piversionbd.repositories.ChatbotMessageRepository;
import pi.db.piversionbd.repositories.DoctorRepository;
import pi.db.piversionbd.repositories.ProduitRepository;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.repositories.MemberRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ChatbotServiceImpl implements IChatbotService {

    @Autowired
    private ChatbotMessageRepository chatbotMessageRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Override
    public ChatbotMessage processUserMessage(Long memberId, String userMessage) {
        // Détcter la maladie dans le message
        String detectedMaladie = detectMaladie(userMessage);

        // Recommander un docteur et un produit
        Doctor recommendedDoctor = recommendDoctorForMaladie(detectedMaladie);
        Produit recommendedProduct = recommendProductForMaladie(detectedMaladie);

        // Générer la réponse
        String botResponse = generateResponse(userMessage, recommendedDoctor, recommendedProduct, detectedMaladie);

        // Créer et sauvegarder le message
        ChatbotMessage message = new ChatbotMessage();
        Optional<Member> member = memberRepository.findById(memberId);
        member.ifPresent(message::setMember);

        message.setUserMessage(userMessage);
        message.setBotResponse(botResponse);
        message.setDetectedMaladie(detectedMaladie);
        if (recommendedDoctor != null) {
            message.setRecommendedDoctorId(recommendedDoctor.getId());
        }
        if (recommendedProduct != null) {
            message.setRecommendedProductId(recommendedProduct.getId());
        }

        return chatbotMessageRepository.save(message);
    }

    @Override
    public String generateResponse(String userMessage, Doctor recommendedDoctor, Produit recommendedProduct, String detectedMaladie) {
        StringBuilder response = new StringBuilder();

        // Salutation et reconnaissance de la maladie
        response.append("Bonjour! 👋\n\n");

        if (!detectedMaladie.isEmpty() && !detectedMaladie.equalsIgnoreCase("unknown")) {
            response.append("J'ai détecté que vous souffrez peut-être de: **").append(detectedMaladie.toUpperCase()).append("**\n\n");
        }

        // Conseils généraux sur la maladie
        response.append(getHealthAdviceForMaladie(detectedMaladie)).append("\n\n");

        // Recommandation de produit
        if (recommendedProduct != null) {
            response.append("💊 **PRODUIT RECOMMANDÉ:**\n");
            response.append("- Nom: ").append(recommendedProduct.getName()).append("\n");
            response.append("- Type: ").append(recommendedProduct.getTypeProduit()).append("\n");
            response.append("- Idéal pour: ").append(recommendedProduct.getMaladieProduit()).append("\n\n");
        }

        // Recommandation de docteur
        if (recommendedDoctor != null) {
            response.append("🏥 **DOCTEUR RECOMMANDÉ:**\n");
            response.append("- Nom: ").append(recommendedDoctor.getName()).append("\n");
            response.append("- Email: ").append(recommendedDoctor.getEmail()).append("\n");
            response.append("- Spécialité: ").append(recommendedDoctor.getSpecialite()).append("\n");
            response.append("- Type: ").append(recommendedDoctor.getTypeDoctor()).append("\n\n");
        }

        // Conseils finaux
        response.append("✅ **CONSEILS:**\n");
        response.append("1. Suivez les recommandations du produit\n");
        response.append("2. Consultez un docteur si les symptômes persistent\n");
        response.append("3. Hydratez-vous bien et reposez-vous\n");
        response.append("4. Contactez le docteur recommandé pour une consultation détaillée\n\n");

        response.append("Besoin d'autre aide? 😊");

        return response.toString();
    }

    @Override
    public Doctor recommendDoctorForMaladie(String maladie) {
        try {
            MaladieProduit maladieProduit = MaladieProduit.valueOf(maladie.toUpperCase());
            List<Doctor> doctors = doctorRepository.findAll();

            // Logique simple: retourner le premier docteur avec la spécialité appropriée
            for (Doctor doctor : doctors) {
                if (matchesSpecialite(maladieProduit, doctor.getSpecialite())) {
                    return doctor;
                }
            }
            // Si aucun match, retourner un docteur généraliste
            return doctors.stream()
                    .filter(d -> d.getSpecialite().equals(Specialite.MÉDECINE_GÉNÉRALE))
                    .findFirst()
                    .orElse(doctors.isEmpty() ? null : doctors.get(0));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Produit recommendProductForMaladie(String maladie) {
        try {
            MaladieProduit maladieProduit = MaladieProduit.valueOf(maladie.toUpperCase());
            return produitRepository.findByMaladieProduit(maladieProduit)
                    .stream()
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String detectMaladie(String userMessage) {
        String lowerMessage = userMessage.toLowerCase();

        // PRIORITÉ 1: Détection des problèmes dentaires
        if (lowerMessage.contains("dent") || lowerMessage.contains("dentaire") ||
            lowerMessage.contains("dental") || lowerMessage.contains("mal de dent")) {
            return "PROBLEME_DENTAIRE";
        }

        // Mapping des mots clés aux maladies
        if (lowerMessage.contains("grippe") || lowerMessage.contains("flu")) {
            return "GRIPPE";
        }
        if (lowerMessage.contains("migraine") || lowerMessage.contains("headache") || lowerMessage.contains("mal de tête")) {
            return "MIGRAINE";
        }
        if (lowerMessage.contains("mal de tête") || lowerMessage.contains("tête")) {
            return "MAL_DE_TETE";
        }
        if (lowerMessage.contains("fièvre") || lowerMessage.contains("fever")) {
            return "FIEVRE";
        }
        if (lowerMessage.contains("douleur") || lowerMessage.contains("musculaire") || lowerMessage.contains("pain")) {
            return "DOULEUR_MUSCULAIRE";
        }
        if (lowerMessage.contains("allergie") || lowerMessage.contains("allergy")) {
            return "ALLERGIE";
        }
        if (lowerMessage.contains("toux") || lowerMessage.contains("cough")) {
            return "TOUX";
        }
        if (lowerMessage.contains("rhume") || lowerMessage.contains("cold")) {
            return "RHUME";
        }
        if (lowerMessage.contains("constipation")) {
            return "CONSTIPATION";
        }
        if (lowerMessage.contains("acidité") || lowerMessage.contains("acide") || lowerMessage.contains("reflux")) {
            return "ACIDITE";
        }
        if (lowerMessage.contains("hypertension") || lowerMessage.contains("tension") || lowerMessage.contains("pressure")) {
            return "HYPERTENSION";
        }
        if (lowerMessage.contains("infection") || lowerMessage.contains("bacterial")) {
            return "INFECTION_BACTERIENNE";
        }
        if (lowerMessage.contains("inflammat") || lowerMessage.contains("inflammation")) {
            return "INFLAMMATION";
        }

        return "unknown";
    }

    private boolean matchesSpecialite(MaladieProduit maladie, Specialite specialite) {
        switch (maladie) {
            case MIGRAINE:
            case MAL_DE_TETE:
                return specialite.equals(Specialite.NEUROLOGIE);
            case HYPERTENSION:
            case DOULEUR_MUSCULAIRE:
                return specialite.equals(Specialite.CARDIOLOGIE);
            case ALLERGIE:
                return specialite.equals(Specialite.DERMATOLOGIE);
            case TOUX:
            case RHUME:
            case GRIPPE:
                return specialite.equals(Specialite.PNEUMOLOGIE);
            case CONSTIPATION:
            case ACIDITE:
                return specialite.equals(Specialite.GASTRO_ENTEROLOGIE);
            default:
                return specialite.equals(Specialite.MÉDECINE_GÉNÉRALE);
        }
    }

    private String getHealthAdviceForMaladie(String maladie) {
        switch (maladie.toUpperCase()) {
            case "MIGRAINE":
                return "💡 **CONSEILS POUR LA MIGRAINE:**\n" +
                        "- Reposez-vous dans un endroit sombre et calme\n" +
                        "- Hydratez-vous régulièrement\n" +
                        "- Évitez les écrans pendant quelques heures\n" +
                        "- Prenez un analgésique recommandé\n" +
                        "- Consultez un docteur si cela persiste";

            case "GRIPPE":
                return "💡 **CONSEILS POUR LA GRIPPE:**\n" +
                        "- Restez à la maison pour éviter la contagion\n" +
                        "- Buvez beaucoup de liquides (eau, thé chaud)\n" +
                        "- Prenez du repos\n" +
                        "- Prendre des antipyrétiques pour la fièvre\n" +
                        "- Consultez un médecin si les symptômes s'aggravent";

            case "FIEVRE":
                return "💡 **CONSEILS POUR LA FIÈVRE:**\n" +
                        "- Maintenez-vous hydraté\n" +
                        "- Reposez-vous autant que possible\n" +
                        "- Prenez des analgésiques si nécessaire\n" +
                        "- Portez des vêtements légers\n" +
                        "- Consultez un docteur si la fièvre dépasse 39°C";

            case "DOULEUR_MUSCULAIRE":
                return "💡 **CONSEILS POUR LES DOULEURS MUSCULAIRES:**\n" +
                        "- Appliquez du chaud/froid sur la zone affectée\n" +
                        "- Reposez le muscle douloureux\n" +
                        "- Faites des étirements légers\n" +
                        "- Prenez des anti-inflammatoires si recommandé\n" +
                        "- Consultez un docteur pour une évaluation";

            case "ALLERGIE":
                return "💡 **CONSEILS POUR LES ALLERGIES:**\n" +
                        "- Identifiez et évitez l'allergène\n" +
                        "- Lavez-vous les mains régulièrement\n" +
                        "- Maintenez votre maison propre\n" +
                        "- Prenez des antihistaminiques si nécessaire\n" +
                        "- Consultez un dermatologue pour les allergies cutanées";

            case "TOUX":
                return "💡 **CONSEILS POUR LA TOUX:**\n" +
                        "- Buvez beaucoup d'eau chaude\n" +
                        "- Utilisez un humidificateur\n" +
                        "- Évitez les irritants (fumée, poussière)\n" +
                        "- Prenez du miel pour la gorge\n" +
                        "- Consultez un docteur si la toux persiste";

            default:
                return "💡 **CONSEILS GÉNÉRAUX:**\n" +
                        "- Restez bien hydraté\n" +
                        "- Reposez-vous adéquatement\n" +
                        "- Maintenez une bonne hygiène\n" +
                        "- Mangez sainement\n" +
                        "- Consultez un docteur si vous vous inquiétez";
        }
    }
}

