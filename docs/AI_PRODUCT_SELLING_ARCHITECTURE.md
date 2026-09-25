# AI Product Selling / Dropshipping Automation

## Pipeline
Product Link / Authorized Feed -> Product Import -> AI Optimizer -> Price & Margin -> Admin Approval -> Store -> Cart -> Server-verified Payment -> Order -> Supplier -> Shipment -> Profit Analytics.

## AI Product Hunter
Candidates come only from authorized APIs, affiliate feeds, approved supplier feeds, or manual input. Candidates remain research/shortlisted until an admin approves them.

## AI Autopilot
Per-product controls cover price monitoring, stock monitoring, AI description/SEO, marketing content, analytics, low-margin alerts, supplier-price alerts, and optional pause-on-out-of-stock.

## AI Product Videos
video_jobs is the queue/library and video_settings controls daily generation. Android/web clients create jobs; provider credentials stay in Edge Function secrets. The video-generator function calls a configured provider and stores returned URLs. Customer flow is Preview -> Watch -> Download -> Share.

## Pricing
Target-margin pricing uses:
selling price = (supplier cost + shipping + fixed costs) / (1 - platform fee percent - target margin).
The UI should show every input and calculated result.

## Safety and reliability
No CAPTCHA/robots bypass, unauthorized scraping, fake reviews, fake engagement, spam automation, or fabricated supplier/payment/video success. External posting, messaging, supplier ordering and payments require real authorized server-side integrations.
