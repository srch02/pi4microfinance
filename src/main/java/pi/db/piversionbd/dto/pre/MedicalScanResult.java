package pi.db.piversionbd.dto.pre;

import java.util.ArrayList;
import java.util.List;

/**
 * Résultat d'un scan médical (OCR/PDF) côté admin.
 * Utilisé par {@code MedicalFormScannerService}.
 */
public class MedicalScanResult {

    private String extractedText;
    private List<String> detectedConditions = new ArrayList<>();
    private boolean rejected;
    private double confidenceScore;
    private String rejectionReason;
    private Boolean consistentWithHistory;
    private String consistencyReason;

    private MedicalScanResult(Builder builder) {
        this.extractedText = builder.extractedText;
        this.detectedConditions = builder.detectedConditions != null
                ? builder.detectedConditions
                : new ArrayList<>();
        this.rejected = builder.rejected;
        this.confidenceScore = builder.confidenceScore;
        this.rejectionReason = builder.rejectionReason;
        this.consistentWithHistory = builder.consistentWithHistory;
        this.consistencyReason = builder.consistencyReason;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public List<String> getDetectedConditions() {
        return detectedConditions;
    }

    public void setDetectedConditions(List<String> detectedConditions) {
        this.detectedConditions = detectedConditions;
    }

    public boolean isRejected() {
        return rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Boolean getConsistentWithHistory() {
        return consistentWithHistory;
    }

    public void setConsistentWithHistory(Boolean consistentWithHistory) {
        this.consistentWithHistory = consistentWithHistory;
    }

    public String getConsistencyReason() {
        return consistencyReason;
    }

    public void setConsistencyReason(String consistencyReason) {
        this.consistencyReason = consistencyReason;
    }

    public static final class Builder {
        private String extractedText;
        private List<String> detectedConditions;
        private boolean rejected;
        private double confidenceScore;
        private String rejectionReason;
        private Boolean consistentWithHistory;
        private String consistencyReason;

        public Builder extractedText(String extractedText) {
            this.extractedText = extractedText;
            return this;
        }

        public Builder detectedConditions(List<String> detectedConditions) {
            this.detectedConditions = detectedConditions;
            return this;
        }

        public Builder rejected(boolean rejected) {
            this.rejected = rejected;
            return this;
        }

        public Builder confidenceScore(double confidenceScore) {
            this.confidenceScore = confidenceScore;
            return this;
        }

        public Builder rejectionReason(String rejectionReason) {
            this.rejectionReason = rejectionReason;
            return this;
        }

        public Builder consistentWithHistory(Boolean consistentWithHistory) {
            this.consistentWithHistory = consistentWithHistory;
            return this;
        }

        public Builder consistencyReason(String consistencyReason) {
            this.consistencyReason = consistencyReason;
            return this;
        }

        public MedicalScanResult build() {
            return new MedicalScanResult(this);
        }
    }
}
