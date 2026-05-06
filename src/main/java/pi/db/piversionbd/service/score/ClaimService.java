package pi.db.piversionbd.service.score;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.dto.score.ClaimCreateRequest;
import pi.db.piversionbd.dto.score.ClaimResponse;
import pi.db.piversionbd.entities.groups.Group;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.groups.Membership;
import pi.db.piversionbd.entities.score.*;
import pi.db.piversionbd.entities.pre.DocumentUpload;
import pi.db.piversionbd.exception.ResourceNotFoundException;
import pi.db.piversionbd.repository.score.ClaimRepository;
import pi.db.piversionbd.repository.groups.GroupRepository;
import pi.db.piversionbd.repository.groups.MemberRepository;
import pi.db.piversionbd.repository.groups.MembershipRepository;
import pi.db.piversionbd.repository.groups.PaymentRepository;
import pi.db.piversionbd.repository.admin.AdminReviewQueueItemRepository;
import pi.db.piversionbd.repository.hedera.BlockchainTransactionRepository;
import pi.db.piversionbd.repository.pre.DocumentUploadRepository;
import pi.db.piversionbd.repository.score.AdherenceTrackingRepository;
import pi.db.piversionbd.repository.score.ClaimScoringRepository;
import pi.db.piversionbd.service.groups.MembershipClaimConsumptionService;
import pi.db.piversionbd.service.hedera.HederaClaimService;
import pi.db.piversionbd.service.hedera.SolidariHealthContractService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.YearMonth;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class ClaimService {

    private static final Logger log = LoggerFactory.getLogger(ClaimService.class);

    private final ClaimRepository claimRepository;
    private final MemberRepository memberRepository;
    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final PaymentRepository paymentRepository;
    private final DocumentUploadRepository documentUploadRepository;
    private final HederaClaimService hederaClaimService;
    private final SolidariHealthContractService solidariHealthContractService;
    private final MembershipClaimConsumptionService membershipClaimConsumptionService;
    private final ClaimScoringService claimScoringService;
    private final ClaimScoringRepository claimScoringRepository;
    private final AdminReviewQueueItemRepository adminReviewQueueItemRepository;
    private final AdherenceTrackingRepository adherenceTrackingRepository;
    private final BlockchainTransactionRepository blockchainTransactionRepository;
    private final ClaimRewardService claimRewardService;
    @Value("${claims.auto-score-on-submit:true}")
    private boolean autoScoreOnSubmit;

    @Value("${claims.minimum-payments-before-claim:0}")
    private long minimumPaymentsBeforeClaim;

    @Value("${claim.bulletins.upload-dir:uploads/claim-bulletins}")
    private String claimBulletinsUploadDir;

    public Claim create(Claim claim) {
        if (claim.getClaimNumber() == null || claim.getClaimNumber().isBlank()) {
            claim.setClaimNumber(generateUniqueClaimNumber());
        } else if (claimRepository.existsByClaimNumber(claim.getClaimNumber().trim())) {
            throw new IllegalArgumentException("claimNumber existe déjà.");
        } else {
            claim.setClaimNumber(claim.getClaimNumber().trim());
        }
        if (claim.getMember() == null) {
            throw new IllegalArgumentException("member est obligatoire.");
        }
        if (claim.getGroup() == null) {
            throw new IllegalArgumentException("group est obligatoire.");
        }

        if (claim.getStatus() == null) {
            claim.setStatus(ClaimStatus.SUBMITTED);
        }

        return claimRepository.save(claim);
    }

    @Transactional(readOnly = true)
    public Claim getById(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim introuvable: " + id));
    }

    @Transactional(readOnly = true)
    public Claim getDetailsById(Long id) {
        return claimRepository.findDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim introuvable: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Claim> getAll(Pageable pageable) {
        return claimRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Claim> getByMemberId(Long memberId, Pageable pageable) {
        return claimRepository.findByMember_IdOrderByCreatedAtDesc(memberId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Claim> getByMemberIdAndStatus(Long memberId, ClaimStatus status, Pageable pageable) {
        return claimRepository.findByMember_IdAndStatusOrderByCreatedAtDesc(memberId, status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Claim> getByStatus(ClaimStatus status, Pageable pageable) {
        return claimRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    @Transactional(readOnly = true)
    public Claim getByClaimNumber(String claimNumber) {
        return claimRepository.findDetailsByClaimNumber(claimNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Claim introuvable pour claimNumber: " + claimNumber));
    }

    /** Saves the claim (e.g. after setting amountApproved in controller). */
    public Claim save(Claim claim) {
        return claimRepository.save(claim);
    }

    public Claim update(Long id, Claim request) {
        Claim existing = getById(id);
        ClaimStatus previous = existing.getStatus();

        // Champs modifiables en CRUD simple
        existing.setAmountRequested(request.getAmountRequested());
        existing.setAmountApproved(request.getAmountApproved());
        existing.setFinalScoreSnapshot(request.getFinalScoreSnapshot());
        existing.setExcludedConditionDetected(request.isExcludedConditionDetected());
        existing.setDecisionComment(request.getDecisionComment());
        existing.setDecisionReason(request.getDecisionReason());
        existing.setDecisionAt(request.getDecisionAt());

        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }

        // Mettre à jour le membre si fourni
        if (request.getMember() != null && request.getMember().getId() != null) {
            Member m = memberRepository.findById(request.getMember().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Member introuvable: " + request.getMember().getId()));
            existing.setMember(m);
        }

        Claim saved = claimRepository.save(existing);
        if (request.getStatus() != null
                && (request.getStatus() == ClaimStatus.APPROVED_AUTO || request.getStatus() == ClaimStatus.APPROVED_MANUAL)) {
            membershipClaimConsumptionService.consumeOnApproval(saved, previous);
        }
        return saved;
    }

    public Claim updateStatus(Long id, ClaimStatus status, ClaimDecisionReason reason, String comment) {
        Claim claim = getById(id);
        ClaimStatus previous = claim.getStatus();
        claim.setStatus(status);
        claim.setDecisionReason(reason);
        claim.setDecisionComment(comment);
        claim.setDecisionAt(LocalDateTime.now());
        if (status == ClaimStatus.APPROVED_AUTO || status == ClaimStatus.APPROVED_MANUAL) {
            BigDecimal amountApproved = claim.getAmountApproved();
            if (amountApproved == null || amountApproved.compareTo(BigDecimal.ZERO) <= 0) {
                log.info("=== HEDERA SKIP: claim {} has no amountApproved (set it via PUT /api/claims/{} before approving)", id, id);
            }
            if (amountApproved != null && amountApproved.compareTo(BigDecimal.ZERO) > 0) {
                log.info("=== HEDERA: recording reimbursement for claim {}", id);
                BigDecimal reimbursementCoins = amountApproved.divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP);
                hederaClaimService.recordReimbursement(claim, reimbursementCoins);
                Long memberId = claim.getMember() != null ? claim.getMember().getId() : null;
                if (memberId != null) {
                    long amountCents = amountApproved.multiply(BigDecimal.valueOf(100)).longValue();
                    long fraudScore = claim.getFinalScoreSnapshot() != null
                            ? claim.getFinalScoreSnapshot().longValue() : 75L;
                    solidariHealthContractService.processClaim(
                            memberId, amountCents, claim.getClaimNumber(), fraudScore,
                            status == ClaimStatus.APPROVED_AUTO ? "APPROVED_AUTO" : "APPROVED_MANUAL");
                }
            }
            membershipClaimConsumptionService.consumeOnApproval(claim, previous);
        }
        return claimRepository.save(claim);
    }

    public void delete(Long id) {
        if (!claimRepository.existsById(id)) {
            throw new ResourceNotFoundException("Claim introuvable: " + id);
        }
        // Remove / unlink dependent rows so the DB FK does not block deletion.
        claimScoringRepository.findByClaimId(id).ifPresent(claimScoringRepository::delete);
        claimScoringRepository.flush();
        documentUploadRepository.unlinkFromClaim(id);
        adminReviewQueueItemRepository.deleteByClaimId(id);
        adherenceTrackingRepository.unlinkRelatedClaim(id);
        blockchainTransactionRepository.deleteByClaimId(id);
        claimRepository.flush();
        claimRepository.deleteById(id);
    }

    /**
     * Uses {@link #generateUniqueClaimNumber()} when {@code claimNumber} is null or blank; otherwise trims and checks uniqueness.
     */
    private String resolveClaimNumber(String requested) {
        if (requested == null || requested.isBlank()) {
            return generateUniqueClaimNumber();
        }
        String trimmed = requested.trim();
        if (claimRepository.existsByClaimNumber(trimmed)) {
            throw new IllegalArgumentException("claimNumber existe déjà.");
        }
        return trimmed;
    }

    /** Format {@code CLM-yyyyMMdd-XXXXXXXX} (date + random suffix), guaranteed unique in DB. */
    private String generateUniqueClaimNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int attempt = 0; attempt < 32; attempt++) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            String candidate = "CLM-" + datePart + "-" + suffix;
            if (!claimRepository.existsByClaimNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique claimNumber");
    }

    @Transactional
    public Claim createFromIds(ClaimCreateRequest req) {
        if (req.memberId == null) throw new IllegalArgumentException("memberId obligatoire");
        if (req.groupId == null) throw new IllegalArgumentException("groupId obligatoire");
        String claimNumber = resolveClaimNumber(req.claimNumber);
        if (req.amountRequested == null || req.amountRequested.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amountRequested doit être > 0");
        }
        // documentUploadIds optional for now (testing); re-tighten when claim upload API is wired.

        Member member = memberRepository.findById(req.memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member introuvable: " + req.memberId));

        Group group = groupRepository.findById(req.groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group introuvable: " + req.groupId));

        // Membership check — optional, used only for coverage limits if present
        Membership membership = membershipRepository
                .findByMember_IdAndGroup_IdAndEndedAtIsNull(req.memberId, req.groupId)
                .orElse(null);

        // Rule: 5 payments minimum before first claim.
        //long paymentCount = paymentRepository.countByMember_IdAndGroup_Id(req.memberId, req.groupId);
        //if (paymentCount < 5) {
            //throw new IllegalArgumentException("Claim not allowed yet: minimum 5 payments are required.");
        //}
        long paymentCount = paymentRepository.countByMember_IdAndGroup_Id(req.memberId, req.groupId);

        if (paymentCount < minimumPaymentsBeforeClaim) {
            throw new IllegalArgumentException(
                    "Claim not allowed yet: minimum " + minimumPaymentsBeforeClaim + " payments are required."
            );
        }
        // Rule: one claim per month (member+group).
        //YearMonth ym = YearMonth.now();
        //LocalDateTime from = ym.atDay(1).atStartOfDay();
        //LocalDateTime to = ym.atEndOfMonth().atTime(23, 59, 59);
        //if (claimRepository.existsByMember_IdAndGroup_IdAndCreatedAtBetween(req.memberId, req.groupId, from, to)) {
          //  throw new IllegalArgumentException("Claim already submitted this month for this group.");
        //}

        // Rule: amount cannot exceed remaining annual coverage for this membership.
        if (membership != null && membership.getAnnualLimit() != null && membership.getAnnualLimit() > 0f) {
            float remaining = membership.getRemainingAnnualAmount();
            if (remaining <= 0f) {
                throw new IllegalArgumentException("Annual coverage for this membership is exhausted.");
            }
            if (req.amountRequested.compareTo(BigDecimal.valueOf(remaining)) > 0) {
                throw new IllegalArgumentException(
                        "Amount exceeds remaining annual coverage (" + remaining + " DT).");
            }
        }

        if (membership != null && membership.getConsultationsLimit() != null && membership.getConsultationsLimit() > 0) {
            if (membership.getRemainingConsultations() <= 0) {
                throw new IllegalArgumentException("No consultations remaining for this membership.");
            }
        }

        Claim c = new Claim();
        c.setMember(member);
        c.setGroup(group);
        c.setClaimNumber(claimNumber);
        c.setAmountRequested(req.amountRequested);
        c.setStatus(ClaimStatus.SUBMITTED);
        Claim saved = claimRepository.save(c);
        claimRewardService.handleClaimSubmitted(saved);
        if (req.documentUploadIds != null && !req.documentUploadIds.isEmpty()) {
            List<Long> distinctDocIds = req.documentUploadIds.stream().distinct().collect(Collectors.toList());
            List<DocumentUpload> docs = documentUploadRepository.findByIdIn(distinctDocIds);
            Set<Long> foundIds = docs.stream().map(DocumentUpload::getId).collect(Collectors.toCollection(HashSet::new));
            List<Long> missingIds = distinctDocIds.stream().filter(id -> !foundIds.contains(id)).collect(Collectors.toList());
            if (!missingIds.isEmpty()) {
                throw new IllegalArgumentException(
                        "Unknown documentUploadIds (no rows in DOCUMENT_UPLOADS): " + missingIds
                                + ". Use real ids from uploads already stored for this member (see DB or pre-registration uploads).");
            }
            for (DocumentUpload doc : docs) {
                boolean ownerMatch = (doc.getMember() != null && doc.getMember().getId() != null
                        && doc.getMember().getId().equals(member.getId()))
                        || (doc.getPreRegistration() != null
                        && doc.getPreRegistration().getCinNumber() != null
                        && doc.getPreRegistration().getCinNumber().equals(member.getCinNumber()));
                if (!ownerMatch) {
                    throw new IllegalArgumentException("Document " + doc.getId() + " does not belong to this member.");
                }
                doc.setClaim(saved);
            }
            documentUploadRepository.saveAll(docs);
        }
        if (autoScoreOnSubmit) {
            try {
                claimScoringService.applyDefaultScoringOnSubmit(saved.getId());
                return claimRepository.findDetailsById(saved.getId()).orElse(saved);
            } catch (Exception e) {
                log.warn("Auto-scoring after claim create failed for claim {}: {}", saved.getId(), e.getMessage(), e);
            }
        }
        return saved;
    }

    @Transactional
    public Claim createFromIdsWithoutScoring(ClaimCreateRequest req) {
        // Same as createFromIds but skips auto-scoring (used when bulletin is uploaded separately)
        boolean originalFlag = autoScoreOnSubmit;
        autoScoreOnSubmit = false;
        try {
            return createFromIds(req);
        } finally {
            autoScoreOnSubmit = originalFlag;
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentUpload> getDocumentsByClaim(Long claimId) {
        return documentUploadRepository.findByClaim_Id(claimId);
    }

    public ClaimResponse toResponse(Claim c) {
        return new ClaimResponse(
                c.getId(),
                c.getClaimNumber(),
                c.getAmountRequested(),
                c.getAmountApproved(),
                c.getFinalScoreSnapshot(),
                c.getStatus() != null ? c.getStatus().name() : null,
                c.getDecisionReason() != null ? c.getDecisionReason().name() : null,
                c.isExcludedConditionDetected(),
                c.getDecisionComment(),
                c.getDecisionAt(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getMember() != null ? c.getMember().getId() : null,
                c.getGroup() != null ? c.getGroup().getId() : null
        );
    }
    @Transactional(rollbackFor = IOException.class)
    public Claim createWithBulletin(ClaimCreateRequest req, MultipartFile bulletin) throws IOException {
        if (bulletin == null || bulletin.isEmpty()) {
            throw new IllegalArgumentException("Bulletin obligatoire.");
        }

        validateBulletinFile(bulletin);

        // Create claim WITHOUT scoring (bulletin not yet saved)
        Claim saved = createFromIdsWithoutScoring(req);

        // Save bulletin BEFORE scoring so the scoring can read the document
        saveBulletinForClaim(saved, bulletin);

        // Now re-score with the document available
        if (autoScoreOnSubmit) {
            try {
                return claimScoringService.applyAutomaticScoringOnSubmit(saved.getId());
            } catch (Exception e) {
                log.warn("Auto-scoring after bulletin upload failed for claim {}: {}", saved.getId(), e.getMessage(), e);
            }
        }

        return claimRepository.findDetailsById(saved.getId()).orElse(saved);
    }
    private void validateBulletinFile(MultipartFile file) {
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Le fichier ne doit pas dépasser 10MB.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Nom du fichier invalide.");
        }

        String extension = getExtension(originalFilename);

        if (!List.of(".pdf", ".png", ".jpg", ".jpeg").contains(extension)) {
            throw new IllegalArgumentException("Format non autorisé. Utilisez PDF, PNG, JPG ou JPEG.");
        }
    }

    private void saveBulletinForClaim(Claim claim, MultipartFile file) throws IOException {
        Path uploadDir = Paths.get(claimBulletinsUploadDir)
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(uploadDir);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "bulletin";
        }

        originalFilename = StringUtils.cleanPath(originalFilename);

        String extension = getExtension(originalFilename);
        String storedFilename = "claim-" + claim.getId() + "-" + UUID.randomUUID() + extension;

        Path targetPath = uploadDir.resolve(storedFilename).normalize();

        // Read bytes once so we can both save and extract text
        byte[] fileBytes = file.getBytes();
        Files.write(targetPath, fileBytes);

        // Extract text from PDF or image for scoring
        String extractedText = null;
        try {
            String ct = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
            if (ct.contains("pdf") || originalFilename.toLowerCase().endsWith(".pdf")) {
                try (org.apache.pdfbox.pdmodel.PDDocument doc =
                        org.apache.pdfbox.Loader.loadPDF(
                                new org.apache.pdfbox.io.RandomAccessReadBuffer(
                                        new java.io.ByteArrayInputStream(fileBytes)))) {
                    org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                    extractedText = stripper.getText(doc);
                }
            }
            // For images, extractedText stays null (OCR requires Google Vision)
        } catch (Exception e) {
            log.warn("Could not extract text from bulletin for claim {}: {}", claim.getId(), e.getMessage());
        }

        DocumentUpload doc = new DocumentUpload();
        doc.setClaim(claim);
        doc.setMember(claim.getMember());
        doc.setOriginalFilename(originalFilename);
        doc.setStoredFilename(storedFilename);
        doc.setFilePath(targetPath.toString());
        doc.setContentType(file.getContentType());
        doc.setSizeBytes(file.getSize());
        doc.setDocumentType("CLAIM_BULLETIN");
        doc.setExtractedText(extractedText);

        documentUploadRepository.save(doc);
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex == -1) {
            return "";
        }

        return filename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

}
