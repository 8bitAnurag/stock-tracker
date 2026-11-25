# Deploying Stock Tracker (Render + Vercel)

## 1. Deploy Backend on Render (Free Blueprint)
1. Push this repo to GitHub (`8bitAnurag/stock-tracker`).
2. In **Render Dashboard -> New + -> Blueprint**, select `8bitAnurag/stock-tracker` (uses `render.yaml` & `Dockerfile`).
3. Enter `ALPHA_VANTAGE_API_KEY` (get a free key at https://www.alphavantage.co/support/#api-key or use `demo` for IBM testing).
4. Once `stock-tracker-api` is **Live**, copy its public `https://...onrender.com` URL.

## 2. Deploy Frontend on Vercel (Free)
1. In **Vercel -> Add New Project**, import `8bitAnurag/stock-tracker`.
2. Set **Root Directory** to `frontend`.
3. Add Environment Variable:
   - `VITE_API_URL` = your Render `stock-tracker-api` URL (no trailing slash).
4. Click **Deploy** and share your live Vercel URL!
