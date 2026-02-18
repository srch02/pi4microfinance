package pi.db.piversionbd.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pi.db.piversionbd.entities.groups.PackageType;
import pi.db.piversionbd.entities.pre.FinancialStabilityLevel;
import pi.db.piversionbd.entities.pre.PreRegistrationStatus;

import java.text.Normalizer;

/**
 * Makes enum query params more tolerant (case/accents).
 * Example: stable, STABLE, modéré → FinancialStabilityLevel
 */
@Configuration
public class EnumConvertersConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToPackageTypeConverter());
        registry.addConverter(new StringToFinancialStabilityLevelConverter());
        registry.addConverter(new StringToPreRegistrationStatusConverter());
    }

    private static String normalize(String input) {
        String trimmed = input.trim();
        String nfd = Normalizer.normalize(trimmed, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}", "");
    }

    private static final class StringToPackageTypeConverter implements Converter<String, PackageType> {
        @Override
        public PackageType convert(String source) {
            if (source == null) return null;
            String v = normalize(source).toUpperCase();
            if ("COMFORT".equals(v)) v = "CONFORT";
            return PackageType.valueOf(v);
        }
    }

    private static final class StringToFinancialStabilityLevelConverter implements Converter<String, FinancialStabilityLevel> {
        @Override
        public FinancialStabilityLevel convert(String source) {
            if (source == null) return null;
            String v = normalize(source).toUpperCase();
            return switch (v) {
                case "STABLE" -> FinancialStabilityLevel.STABLE;
                case "MODERE", "MODEREE" -> FinancialStabilityLevel.MODERE;
                case "INSTABLE" -> FinancialStabilityLevel.INSTABLE;
                default -> FinancialStabilityLevel.valueOf(v);
            };
        }
    }

    private static final class StringToPreRegistrationStatusConverter implements Converter<String, PreRegistrationStatus> {
        @Override
        public PreRegistrationStatus convert(String source) {
            if (source == null) return null;
            String v = normalize(source).toUpperCase();
            return PreRegistrationStatus.valueOf(v);
        }
    }
}

