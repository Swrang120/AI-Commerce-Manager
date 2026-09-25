# Nexus AI Commerce — Web Storefront & Android App

Production-ready, multi-platform AI E-Commerce & Dropshipping Management Platform powered by **Supabase PostgreSQL**, **Razorpay Secure Payments**, and **Jetpack Compose / Modern Web**.

---

## 🚀 Quick Setup for GitHub & Local Environments (हिंदी और English)

Agar aapne is repository ko GitHub par import ya push kiya hai, toh yeh guide aapko **Android App** aur **Website** dono chalane me help karegi:

### 1. 📱 Android App (How to Build & Run)

#### Option A: Direct APK Download via GitHub Actions (Sabse Aasan)
- Repository ke **Actions** tab par jayein.
- Har bar jab aap commit/push karenge, workflow automatic `./gradlew assembleDebug` run karega.
- Workflow run complete hone ke baad **Artifacts** section se `Nexus-AI-Commerce-Android-APK` zip download karein aur phone me install karein!

#### Option B: Android Studio me Run karein
1. Repository ko clone karein:
   ```bash
   git clone https://github.com/YOUR_USERNAME/REPO_NAME.git
   cd REPO_NAME
   ```
2. Android Studio (Ladybug / Iguana+) open karein aur folder choose karein.
3. Terminal me command run karke build check karein:
   ```bash
   ./gradlew assembleDebug
   ```
   *(Windows me: `gradlew.bat assembleDebug`)*
4. Run button dabakar emulator ya phone par test karein.

---

### 2. 🌐 Website (Web Storefront & Admin Portal)

Yeh repository ek full-fledged responsive Web Storefront aur Admin Portal ke sath aati hai (`index.html`).

#### Option A: GitHub Pages par Live Host karein (1 Click)
1. Apne GitHub repo ke **Settings** > **Pages** me jayein.
2. **Build and deployment**:
   - Source: **Deploy from a branch**
   - Branch: `main` (ya `master`), Folder: `/ (root)`
3. Save karein. 1-2 minute me aapki website live ho jayegi:
   `https://<username>.github.io/<repo-name>/`

#### Option B: Apne Computer / Browser me kholein
- Simply `index.html` ko double-click karke kisi bhi browser (Chrome, Edge, Firefox) me open karein.
- Ya local server se run karein:
  ```bash
  npx serve .
  # OR
  python3 -m http.server 8080
  ```

---

## 🔑 Backend & API Keys Configuration

Default setup me pre-configured Supabase project connect hota hai. Aap `.env` file me apni keys update kar sakte hain:

```env
# Supabase Configuration
SUPABASE_URL=https://eklycpnfnyjydrgxdnzm.supabase.co
SUPABASE_ANON_KEY=sb_publishable_NUt0MMWF-50fcrWCL5Nq-g_OX3FGqVf

# Razorpay Configuration
RAZORPAY_PAGE_URL=https://razorpay.me/@santiramswargiary
RAZORPAY_KEY_ID=rzp_test_placeholder

# Gemini AI API Key
GEMINI_API_KEY=placeholder_gemini_api_key
```

Supabase tables migration SQL file `supabase/migrations/01_initial_schema.sql` me uplabdh hai.

---

## 🛡️ Key System Implementations & Rules

1. **Rule 25 (Supplier Fulfillment)**: Zero fake supplier confirmation. Admin manual order creation -> entry of real supplier order number and tracking code -> saves to Supabase.
2. **Rule 26 (Suppliers Registry)**: Suppliers loaded directly from `public.suppliers`. API status marked honestly.
3. **Rule 27 & 28 (Stock & Price Monitoring)**: Logs verified source changes directly into `stock_history` and `price_history`.
4. **Rule 29 & 30 (Automations)**: Honest operation logs in `automation_runs` without fabricated scanning claims.
5. **Rule 31 (Review Moderation)**: New reviews default to `is_published = false` and `is_verified_purchase = false` until purchase validation and admin approval.
6. **Rule 32 & 33 (Coupons & Cart)**: Atomic validation against Supabase `public.coupons`, persistent guest session (`session_id`) with authenticated cart merging.
7. **Rule 35 (Customer Profiles)**: Real customer address input without hardcoded locations.
