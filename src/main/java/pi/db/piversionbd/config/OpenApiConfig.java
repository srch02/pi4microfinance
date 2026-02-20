package pi.db.piversionbd.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class OpenApiConfig {

    private static final Set<String> ENTITY_SCHEMA_NAMES = Set.of(
            "PreRegistration", "Member", "MedicalHistory", "RiskAssessment",
            "DocumentUpload", "ExcludedCondition", "BlacklistEntry", "AdminReviewQueueItem",
            "Group", "Membership", "Payment", "Claim", "AdminUser", "GroupPool",
            "Consultation", "Doctor", "Medication", "HealthTrackingEntry", "PharmacyRecommendation",
            "MemberChurnForecast", "PlatformKpiSnapshot",
            "AdherenceTracking", "MemberReward", "RewardCatalogItem", "ClaimScoring"
    );

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PI4 Microfinance API")
                        .version("1.0")
                        .description("Backend endpoints for Pre-Registration, Admin and Solidarity Groups modules"));
    }

    @Bean
    public OpenApiCustomizer removeEntitySchemasCustomizer() {
        return openApi -> {
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                ENTITY_SCHEMA_NAMES.forEach(openApi.getComponents().getSchemas()::remove);
            }
        };
    }
}