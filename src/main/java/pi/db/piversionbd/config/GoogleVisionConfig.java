package pi.db.piversionbd.config;

import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Configuration pour Google Cloud Vision API (OCR).
 * Active uniquement si ml.vision.enabled=true.
 * Nécessite GOOGLE_APPLICATION_CREDENTIALS ou authentification GCP.
 */
@Configuration
public class GoogleVisionConfig {

    @Bean
    @ConditionalOnProperty(name = "ml.vision.enabled", havingValue = "true")
    @ConditionalOnExpression("'${GOOGLE_APPLICATION_CREDENTIALS:}' != ''")
    public ImageAnnotatorClient imageAnnotatorClient() throws IOException {
        return ImageAnnotatorClient.create(ImageAnnotatorSettings.newBuilder().build());
    }
}
