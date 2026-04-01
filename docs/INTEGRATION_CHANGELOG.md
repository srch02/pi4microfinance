# Integration Change History – Option B & Hedera Blockchain

Use this file to see what was added per module when porting to another version.

---

## 1. Admin/Auth Module – Option B (AdminUser ↔ Member)

### 1.1 AdminUser entity
- Added `@OneToOne @JoinColumn(name = "member_id", nullable = true) private Member member;`

### 1.2 AdminUserRepository
- Added `long countByMemberIsNotNull();`

### 1.3 AdminUserService
- Injected `MemberRepository`
- Added `registerMemberPortalAccount(username, email, rawPassword, memberId)`
- Extended `dashboardStats()` with `insuredMembers`, `linkedAccounts`, `portalAccounts`

### 1.4 MemberAuthController
- `RegisterRequest`: added `Long memberId`
- `MemberResponse`: added `Long memberId`
- `/register`: when `memberId != null`, calls `registerMemberPortalAccount`

---

## 2. Member / Groups Module – Auth cleanup

### 2.1 Member entity
- Removed: `password`, `enabled`, `failedLoginAttempts`, `lockedAt`, `lastLogin`
- Kept: `email`, `createdAt`
- Added (Hedera): `walletAddress`, `coinBalance`, `blockchainContractHash`

### 2.2 MemberRepository
- Removed: `countByEnabledFalse`, `countByLockedAtNotNull`

### 2.3 IMemberService
- Removed: `register`, `login`, `resetPassword`

### 2.4 MemberServiceImp
- Removed auth logic, `generateSecurePassword`, `generateCinNumber`, `sendResetPasswordEmail`
- Removed `PasswordEncoder`, `JavaMailSender`
- `createMember`: sets `createdAt` if null
- `dashboardStatsForMembers`: only `totalMembers`, `newMembersToday`

### 2.5 GroupsModuleDto.MemberDto
- Removed: `enabled`, `failedLoginAttempts`, `lockedAt`, `lastLogin`
- Kept: `memberId`, `cinNumber`, `age`, `profession`, `region`, `email`, prices, `adherenceScore`, `currentGroupId`, `createdAt`

### 2.6 MemberController
- Updated `MemberDto` constructor calls to 13 args (no auth fields)

### 2.7 Payment entity
- Added: `blockchainHash`, `coinAmount`, `dtAmount`

---

## 3. Pre-Registration Module – Hedera integration

### 3.1 PreRegistrationServiceImpl
- Injected: `HederaContractService`, `HederaWalletService`
- Added private method `applyHederaIntegration(member, packageType, finalPrice, exclusionsNote)`:
  - `createWallet(member.getId())` → `member.setWalletAddress`
  - `member.setCoinBalance(initialCoins)` with `initialCoins = finalPrice / 3` (1 coin = 3 DT)
  - `deployContract(...)` → `member.setBlockchainContractHash`
- Called from `confirmPayment` (BASIC) and `confirmPaymentByPackage`

---

## 4. Payments Module – Hedera integration

### 4.1 PaymentServiceImp
- Injected: `HederaPaymentService`
- After `paymentRepository.save(payment)` in both `recordSuccessfulPayment` and `processMonthlyPayment`:
  - `hederaPaymentService.recordPayment(payment)`
  - `paymentRepository.save(payment)` (persist blockchain fields)

---

## 5. Claims Module – Hedera integration

### 5.1 Claim entity
- Added: `blockchainHash`, `reimbursementCoins`

### 5.2 ClaimService
- Injected: `HederaClaimService`
- In `updateStatus`: when status is `APPROVED_AUTO` or `APPROVED_MANUAL`:
  - `reimbursementCoins = amountApproved / 3`
  - `hederaClaimService.recordReimbursement(claim, reimbursementCoins)`

---

## 6. Hedera / Blockchain Module (NEW)

### 6.1 Dependencies (pom.xml)
```xml
<dependency>
    <groupId>com.hedera.hashgraph</groupId>
    <artifactId>sdk</artifactId>
    <version>2.47.0</version>
</dependency>
```

### 6.2 Config (application.properties)
```
hedera.enabled=false
hedera.network=testnet
hedera.operator.account-id=0.0.1234
hedera.operator.private-key=...
hedera.topic.contracts=0.0.0
hedera.topic.transactions=0.0.0
```

### 6.3 SolidariHealth Smart Contract (Option B)
- `service/hedera/SolidariHealthContractService.java` – calls Solidity contract: `createMemberPolicy`, `recordMonthlyPayment`, `processClaim`
- Config: `hedera.contract.id=0.0.8012466`
- Wired in: PreRegistrationServiceImpl (createMemberPolicy), PaymentServiceImp (recordMonthlyPayment), ClaimService (processClaim)

### 6.4 New files (topics + contract)
- `config/HederaProperties.java` – `@ConfigurationProperties(prefix = "hedera")`
- `entities/hedera/BlockchainTransaction.java` – audit entity
- `repository/hedera/BlockchainTransactionRepository.java`
- `service/hedera/HederaAuditService.java` – low-level topic message submission
- `service/hedera/HederaContractService.java` – deploy contract metadata
- `service/hedera/HederaWalletService.java` – create wallet, DT↔coins conversion
- `service/hedera/HederaPaymentService.java` – record payments on blockchain
- `service/hedera/HederaClaimService.java` – record reimbursements on blockchain

### 6.4 Database migration
- `V202602181200__hedera_blockchain_fields.sql`:
  - MEMBERS: `wallet_address`, `coin_balance`, `blockchain_contract_hash`
  - PAYMENTS: `blockchain_hash`, `coin_amount`, `dt_amount`
  - CLAIMS: `blockchain_hash`, `reimbursement_coins`
  - New table: `BLOCKCHAIN_TRANSACTIONS`

---

## 7. Documentation

- `docs/SIMULATION_MEMBER_PORTAL_FLOW.md` – manual flow for Option B
- `docs/INTEGRATION_CHANGELOG.md` – this file
