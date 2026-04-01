# Simulate: Pre-inscription → Admin approval → Member → Portal account

Use this order to simulate the full flow. Base URL: `http://localhost:8080` (or your server).

---

## Step 1 – User submits pre-inscription

**Create a pre-registration** (user fills the medical form).

**Option A – JSON body**

```http
POST /api/pre-registration
Content-Type: application/json

{
  "cinNumber": "12345678",
  "medicalDeclarationText": "Recurrent flu, seasonal allergies, no chronic disease",
  "currentConditions": "rhume saisonnier",
  "familyHistory": "mother with allergies",
  "ongoingTreatments": "none",
  "consultationFrequency": "2 times per year",
  "age": 35,
  "profession": "office worker",
  "financialStability": "STABLE",
  "seasonalIllnessMonthsPerYear": 2
}
```

**Option B – Form params**

```http
POST /api/pre-registration/form?cinNumber=12345678&age=35&profession=office%20worker&medicalDeclarationText=Recurrent%20flu
```

**Response:** You get `preRegistrationId` (e.g. `1`). Save it. Status is `PENDING_REVIEW`.  
The backend creates: `PreRegistration`, `MedicalHistory`, `RiskAssessment`, `AdminReviewQueueItem`.

---

## Step 2 – Admin approves the pre-inscription

Admin (or you in Postman) sets status to **APPROVED**. Acceptance is “sent” by this status change (you can later add email/alert on top).

```http
PATCH /api/pre-registration/1/status?status=APPROVED
```

Replace `1` with your `preRegistrationId`.  
Response: pre-registration summary with `status: APPROVED`.

---

## Step 3 – Confirm payment → Member is created

Once approved, “payment confirmed” creates the **Member** and sets pre-registration to **ACTIVATED**.

**Option A – Confirm with exact amount** (use `calculatedPrice` from step 1 response, e.g. 25.0)

```http
POST /api/pre-registration/1/confirm-payment?paymentAmount=25.0
```

**Option B – Confirm by package** (simpler for simulation)

```http
POST /api/pre-registration/1/confirm-payment-by-package?packageType=BASIC
```

`packageType` can be: `BASIC`, `CONFORT`, `PREMIUM`.

Response: pre-registration summary with `status: ACTIVATED`.  
**The backend has created a `Member`** linked to this pre-registration (same CIN). The response does not include `memberId`, so you get it in step 4.

---

## Step 4 – Get the Member ID

You need the **Member id** to link the portal account (Option B).

```http
GET /api/members
```

In the list, find the member with `cinNumber: "12345678"` (or the CIN you used). Note **`memberId`** (e.g. `1`).

---

## Step 5 – Create the member portal account (link AdminUser ↔ Member)

Create the login account and **link it to that Member** with `memberId`.

```http
POST /api/members/auth/register
Content-Type: application/json

{
  "username": "john_portal",
  "email": "john@example.com",
  "password": "Secret123",
  "memberId": 1
}
```

Use the `memberId` from step 4.  
Backend creates an `AdminUser` with `role = "MEMBER"` and sets `admin_user.member_id = 1`.

---

## Step 6 – Log in as the member

```http
POST /api/members/auth/login
Content-Type: application/json

{
  "username": "john_portal",
  "password": "Secret123"
}
```

Response includes `memberId` when the account is linked.  
In your app you can then do: `adminUser.getMember()` to get the same Member from pre-inscription.

---

## Summary table

| Order | What to create / do | Endpoint / action |
|-------|---------------------|-------------------|
| 1 | Pre-inscription (user data) | `POST /api/pre-registration` or `POST /api/pre-registration/form` |
| 2 | Admin approval | `PATCH /api/pre-registration/{id}/status?status=APPROVED` |
| 3 | Confirm payment → **Member** created | `POST /api/pre-registration/{id}/confirm-payment-by-package?packageType=BASIC` |
| 4 | Get Member id | `GET /api/members` → find by CIN, note `memberId` |
| 5 | Portal account linked to Member | `POST /api/members/auth/register` with `memberId` |
| 6 | Login | `POST /api/members/auth/login` |

After step 6, the logged-in user is an **AdminUser** whose `member` field points to the **Member** created in step 3 (same person as the pre-inscription).
