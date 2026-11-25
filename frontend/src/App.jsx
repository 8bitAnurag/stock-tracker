import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Activity,
  ArrowUpRight,
  Bookmark,
  BookmarkCheck,
  Database,
  ExternalLink,
  RefreshCw,
  Search,
  Trash2,
  TrendingDown,
  TrendingUp,
} from 'lucide-react';

const API_BASE_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/+$/, '');
const QUICK_TICKERS = ['IBM', 'AAPL', 'MSFT', 'NVDA', 'GOOGL', 'TSLA'];

function PriceSparkline({ history }) {
  const ordered = useMemo(() => [...history].reverse(), [history]);
  if (ordered.length < 2) {
    return (
      <div className="flex h-44 items-center justify-center rounded-lg border border-dashed border-zinc-800 text-xs text-zinc-500">
        Not enough historical points to plot chart.
      </div>
    );
  }

  const closes = ordered.map((d) => d.close);
  const min = Math.min(...closes);
  const max = Math.max(...closes);
  const span = max - min || 1;

  const width = 640;
  const height = 180;
  const padX = 12;
  const padY = 18;

  const points = ordered.map((d, i) => {
    const x = padX + (i / (ordered.length - 1)) * (width - padX * 2);
    const y = height - padY - ((d.close - min) / span) * (height - padY * 2);
    return { x, y, ...d };
  });

  const linePath = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' ');
  const areaPath = `${linePath} L ${points[points.length - 1].x.toFixed(1)} ${height} L ${points[0].x.toFixed(1)} ${height} Z`;

  const first = closes[0];
  const last = closes[closes.length - 1];
  const isUp = last >= first;
  const strokeColor = isUp ? '#10b981' : '#f43f5e';

  return (
    <div className="rounded-xl border border-zinc-800 bg-zinc-950/70 p-4">
      <div className="font-mono-tech mb-2 flex items-center justify-between text-[11px] text-zinc-400">
        <span>High: ${max.toFixed(2)}</span>
        <span>Low: ${min.toFixed(2)}</span>
      </div>
      <svg viewBox={`0 0 ${width} ${height}`} className="h-44 w-full overflow-visible">
        <defs>
          <linearGradient id="stockFill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={strokeColor} stopOpacity="0.28" />
            <stop offset="100%" stopColor={strokeColor} stopOpacity="0.0" />
          </linearGradient>
        </defs>
        <path d={areaPath} fill="url(#stockFill)" />
        <path d={linePath} fill="none" stroke={strokeColor} strokeWidth="2.2" strokeLinejoin="round" />
        {points.map((p) => (
          <circle key={p.date} cx={p.x} cy={p.y} r="3" fill={strokeColor} className="opacity-75">
            <title>{`${p.date}: $${p.close.toFixed(2)}`}</title>
          </circle>
        ))}
      </svg>
      <div className="font-mono-tech mt-1 flex justify-between text-[10.5px] text-zinc-500">
        <span>{ordered[0].date}</span>
        <span>{ordered[ordered.length - 1].date}</span>
      </div>
    </div>
  );
}

export default function App() {
  const [query, setQuery] = useState('AAPL');
  const [activeSymbol, setActiveSymbol] = useState('AAPL');
  const [days, setDays] = useState(7);

  const [quote, setQuote] = useState(null);
  const [overview, setOverview] = useState(null);
  const [history, setHistory] = useState([]);
  const [favourites, setFavourites] = useState([]);

  const [loading, setLoading] = useState(true);
  const [favBusy, setFavBusy] = useState(false);
  const [error, setError] = useState('');
  const [backendStatus, setBackendStatus] = useState('checking');

  const fetchFavourites = useCallback(async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/api/v1/stocks/favourites`);
      if (res.ok) {
        const data = await res.json();
        setFavourites(Array.isArray(data) ? data : []);
      }
    } catch {
      // handled by main load error state
    }
  }, []);

  const loadStock = useCallback(async (symbol, dayCount) => {
    const clean = symbol.trim().toUpperCase();
    if (!clean) return;
    setLoading(true);
    setError('');
    setActiveSymbol(clean);

    try {
      const [quoteRes, overviewRes, historyRes] = await Promise.all([
        fetch(`${API_BASE_URL}/api/v1/stocks/${encodeURIComponent(clean)}`),
        fetch(`${API_BASE_URL}/api/v1/stocks/${encodeURIComponent(clean)}/overview`),
        fetch(`${API_BASE_URL}/api/v1/stocks/${encodeURIComponent(clean)}/history?days=${dayCount}`),
      ]);

      if (!quoteRes.ok) {
        throw new Error(`API returned HTTP ${quoteRes.status}`);
      }

      const quoteData = await quoteRes.json();
      const overviewData = overviewRes.ok ? await overviewRes.json() : null;
      const historyData = historyRes.ok ? await historyRes.json() : [];

      setQuote(quoteData);
      setOverview(overviewData);
      setHistory(Array.isArray(historyData) ? historyData : []);
      setBackendStatus('online');
    } catch (err) {
      setBackendStatus('offline');
      setError(
        err?.message && !err.message.includes('Failed to fetch')
          ? err.message
          : 'Could not reach the Spring Boot API. On Render free tier, wait ~40s for cold start and click Retry.'
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadStock('AAPL', 7).then(() => fetchFavourites());
  }, [fetchFavourites, loadStock]);

  const handleSearch = (e) => {
    e.preventDefault();
    loadStock(query, days);
  };

  const handleDaysChange = (nextDays) => {
    setDays(nextDays);
    loadStock(activeSymbol, nextDays);
  };

  const isSaved = favourites.some((f) => f.symbol?.toUpperCase() === activeSymbol.toUpperCase());

  const toggleFavourite = async () => {
    setFavBusy(true);
    setError('');
    try {
      const method = isSaved ? 'DELETE' : 'POST';
      const res = await fetch(
        `${API_BASE_URL}/api/v1/stocks/favourites?symbol=${encodeURIComponent(activeSymbol)}`,
        { method }
      );
      if (!res.ok) {
        const msg = await res.text().catch(() => '');
        throw new Error(msg || `Watchlist update failed (${res.status})`);
      }
      await fetchFavourites();
    } catch (err) {
      setError(err.message || 'Watchlist action failed.');
    } finally {
      setFavBusy(false);
    }
  };

  const removeFavourite = async (sym) => {
    try {
      const res = await fetch(
        `${API_BASE_URL}/api/v1/stocks/favourites?symbol=${encodeURIComponent(sym)}`,
        { method: 'DELETE' }
      );
      if (res.ok) {
        await fetchFavourites();
      }
    } catch {
      // ignore transient errors
    }
  };

  const deltaPct = useMemo(() => {
    if (history.length < 2) return null;
    const latest = history[0].close;
    const oldest = history[history.length - 1].close;
    if (!oldest) return null;
    return ((latest - oldest) / oldest) * 100;
  }, [history]);

  return (
    <div className="min-h-screen bg-[#0a0a0a] text-zinc-100">
      {/* Header */}
      <header className="sticky top-0 z-30 border-b border-zinc-800/80 bg-[#0a0a0a]/85 backdrop-blur-md">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-3 px-5 py-3.5">
          <div className="flex items-center gap-3">
            <h1 className="font-serif-display text-2xl tracking-tight text-white">
              Stock Tracker
            </h1>
            <span className="font-mono-tech hidden rounded-full border border-zinc-800 bg-zinc-900 px-2.5 py-0.5 text-[11px] text-zinc-400 sm:inline-flex">
              Spring Boot · Alpha Vantage · H2
            </span>
          </div>

          <div className="flex items-center gap-3">
            <span className="font-mono-tech inline-flex items-center gap-1.5 rounded-full border border-zinc-800 bg-zinc-900 px-2.5 py-1 text-[11px] text-zinc-300">
              <span
                className={`h-2 w-2 rounded-full ${
                  backendStatus === 'online'
                    ? 'bg-emerald-400'
                    : backendStatus === 'checking'
                    ? 'bg-amber-400'
                    : 'bg-rose-400'
                }`}
              />
              {backendStatus === 'online'
                ? 'API Online'
                : backendStatus === 'checking'
                ? 'Connecting'
                : 'Sleeping / Offline'}
            </span>

            <a
              href={`${API_BASE_URL}/swagger-ui/index.html`}
              target="_blank"
              rel="noopener noreferrer"
              className="font-mono-tech inline-flex items-center gap-1 rounded-lg border border-zinc-800 bg-zinc-900 px-3 py-1.5 text-xs text-zinc-300 transition-colors hover:border-zinc-700 hover:text-white"
            >
              <span>Swagger UI</span>
              <ExternalLink className="h-3 w-3" />
            </a>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-5 py-7">
        {/* Search & Quick Tickers */}
        <section className="rounded-xl border border-zinc-800 bg-zinc-900/50 p-4">
          <form onSubmit={handleSearch} className="flex flex-col gap-3 sm:flex-row">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute top-1/2 left-3.5 h-4 w-4 -translate-y-1/2 text-zinc-500" />
              <input
                type="text"
                value={query}
                onChange={(e) => setQuery(e.target.value.toUpperCase())}
                placeholder="Enter stock symbol (e.g. IBM, AAPL, NVDA)..."
                className="font-mono-tech w-full rounded-lg border border-zinc-800 bg-zinc-950 py-2.5 pr-4 pl-10 text-sm text-white placeholder:text-zinc-500 focus:border-zinc-600 focus:outline-none"
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              className="inline-flex cursor-pointer items-center justify-center gap-2 rounded-lg bg-white px-5 py-2.5 text-sm font-medium text-zinc-950 transition-opacity hover:opacity-90 disabled:opacity-60"
            >
              {loading ? <RefreshCw className="h-4 w-4 animate-spin" /> : <Activity className="h-4 w-4" />}
              <span>{loading ? 'Fetching...' : 'Lookup Stock'}</span>
            </button>
          </form>

          <div className="mt-3 flex flex-wrap items-center gap-2">
            <span className="font-mono-tech text-[11px] text-zinc-500">Quick symbols:</span>
            {QUICK_TICKERS.map((sym) => (
              <button
                key={sym}
                type="button"
                onClick={() => {
                  setQuery(sym);
                  loadStock(sym, days);
                }}
                className={`font-mono-tech cursor-pointer rounded-md border px-2.5 py-0.5 text-xs transition-colors ${
                  activeSymbol === sym
                    ? 'border-white bg-white text-zinc-950'
                    : 'border-zinc-800 bg-zinc-950 text-zinc-400 hover:border-zinc-700 hover:text-white'
                }`}
              >
                {sym}
              </button>
            ))}
          </div>
        </section>

        {error && (
          <div className="mt-4 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-rose-500/30 bg-rose-950/30 px-4 py-3 text-sm text-rose-200">
            <span>{error}</span>
            <button
              type="button"
              onClick={() => {
                loadStock(activeSymbol, days);
                fetchFavourites();
              }}
              className="font-mono-tech cursor-pointer rounded-md border border-rose-400/40 px-3 py-1 text-xs hover:bg-rose-500/20"
            >
              Retry
            </button>
          </div>
        )}

        <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-[1fr_300px]">
          {/* Left Column: Quote + Overview + History */}
          <div className="space-y-6">
            {/* Quote & Overview Card */}
            <section className="rounded-xl border border-zinc-800 bg-zinc-900/50 p-5">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <div className="flex flex-wrap items-center gap-2.5">
                    <span className="font-mono-tech rounded-md border border-zinc-700 bg-zinc-800 px-2.5 py-0.5 text-sm font-semibold text-white">
                      {quote?.symbol || activeSymbol}
                    </span>
                    {overview?.Sector && overview.Sector !== 'N/A' && (
                      <span className="font-mono-tech rounded-full border border-zinc-800 bg-zinc-950 px-2.5 py-0.5 text-[11px] text-zinc-400">
                        {overview.Sector}
                      </span>
                    )}
                    {deltaPct !== null && (
                      <span
                        className={`font-mono-tech inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] ${
                          deltaPct >= 0
                            ? 'bg-emerald-500/15 text-emerald-400'
                            : 'bg-rose-500/15 text-rose-400'
                        }`}
                      >
                        {deltaPct >= 0 ? (
                          <TrendingUp className="h-3 w-3" />
                        ) : (
                          <TrendingDown className="h-3 w-3" />
                        )}
                        {deltaPct >= 0 ? '+' : ''}
                        {deltaPct.toFixed(2)}% ({days}D)
                      </span>
                    )}
                  </div>

                  <h2 className="font-serif-display mt-2 text-3xl text-white">
                    {overview?.Name || activeSymbol}
                  </h2>
                </div>

                <div className="flex flex-col items-end gap-2">
                  <div className="font-mono-tech text-3xl font-semibold text-white">
                    {quote && quote.price > 0 ? `$${quote.price.toFixed(2)}` : 'N/A'}
                  </div>
                  <button
                    type="button"
                    disabled={favBusy}
                    onClick={toggleFavourite}
                    className={`font-mono-tech inline-flex cursor-pointer items-center gap-1.5 rounded-lg border px-3 py-1.5 text-xs transition-colors ${
                      isSaved
                        ? 'border-emerald-500/40 bg-emerald-500/15 text-emerald-300 hover:bg-emerald-500/25'
                        : 'border-zinc-700 bg-zinc-950 text-zinc-300 hover:border-zinc-500 hover:text-white'
                    }`}
                  >
                    {isSaved ? (
                      <>
                        <BookmarkCheck className="h-3.5 w-3.5" />
                        <span>Saved in Watchlist</span>
                      </>
                    ) : (
                      <>
                        <Bookmark className="h-3.5 w-3.5" />
                        <span>Add to Watchlist</span>
                      </>
                    )}
                  </button>
                </div>
              </div>

              {quote && quote.price === 0 && !loading && (
                <p className="font-mono-tech mt-3 rounded-lg border border-amber-500/30 bg-amber-950/25 px-3 py-2 text-xs text-amber-200">
                  Alpha Vantage returned N/A for {activeSymbol} (free tier allows 25 requests/day; cached symbols load without consuming quota).
                </p>
              )}

              <div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
                <div className="rounded-lg border border-zinc-800 bg-zinc-950/70 p-3">
                  <div className="font-mono-tech text-[10.5px] text-zinc-500 uppercase">Market Cap</div>
                  <div className="mt-1 text-sm font-medium text-white">
                    {overview?.MarketCapitalization || 'N/A'}
                  </div>
                </div>
                <div className="rounded-lg border border-zinc-800 bg-zinc-950/70 p-3">
                  <div className="font-mono-tech text-[10.5px] text-zinc-500 uppercase">P/E Ratio</div>
                  <div className="font-mono-tech mt-1 text-sm font-medium text-white">
                    {overview?.PERatio || 'N/A'}
                  </div>
                </div>
                <div className="rounded-lg border border-zinc-800 bg-zinc-950/70 p-3">
                  <div className="font-mono-tech text-[10.5px] text-zinc-500 uppercase">Div Yield</div>
                  <div className="font-mono-tech mt-1 text-sm font-medium text-white">
                    {overview?.DividendYield || 'N/A'}
                  </div>
                </div>
                <div className="rounded-lg border border-zinc-800 bg-zinc-950/70 p-3">
                  <div className="font-mono-tech text-[10.5px] text-zinc-500 uppercase">Last Trading</div>
                  <div className="font-mono-tech mt-1 text-sm font-medium text-white">
                    {quote?.lastUpdates || 'N/A'}
                  </div>
                </div>
              </div>

              {overview?.Description && (
                <p className="mt-4 text-sm leading-relaxed text-zinc-400">
                  {overview.Description}
                </p>
              )}
            </section>

            {/* Price History Section */}
            <section className="rounded-xl border border-zinc-800 bg-zinc-900/50 p-5">
              <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
                <h3 className="font-serif-display text-2xl text-white">
                  Historical Performance
                </h3>
                <div className="flex items-center gap-1 rounded-lg border border-zinc-800 bg-zinc-950 p-1">
                  {[7, 14, 30].map((d) => (
                    <button
                      key={d}
                      type="button"
                      onClick={() => handleDaysChange(d)}
                      className={`font-mono-tech cursor-pointer rounded-md px-2.5 py-1 text-xs transition-colors ${
                        days === d
                          ? 'bg-white text-zinc-950'
                          : 'text-zinc-400 hover:text-white'
                      }`}
                    >
                      {d}D
                    </button>
                  ))}
                </div>
              </div>

              <PriceSparkline history={history} />

              {history.length > 0 && (
                <div className="mt-4 overflow-x-auto">
                  <table className="font-mono-tech w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-zinc-800 text-zinc-500">
                        <th className="py-2 pr-3">Date</th>
                        <th className="py-2 pr-3">Open</th>
                        <th className="py-2 pr-3">High</th>
                        <th className="py-2 pr-3">Low</th>
                        <th className="py-2 pr-3">Close</th>
                        <th className="py-2 text-right">Volume</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-800/70 text-zinc-300">
                      {history.slice(0, 10).map((row) => (
                        <tr key={row.date} className="hover:bg-zinc-900/70">
                          <td className="py-2 pr-3 text-zinc-400">{row.date}</td>
                          <td className="py-2 pr-3">${row.open.toFixed(2)}</td>
                          <td className="py-2 pr-3">${row.high.toFixed(2)}</td>
                          <td className="py-2 pr-3">${row.low.toFixed(2)}</td>
                          <td className="py-2 pr-3 font-semibold text-white">${row.close.toFixed(2)}</td>
                          <td className="py-2 text-right text-zinc-400">
                            {row.volume.toLocaleString()}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          </div>

          {/* Right Column: Watchlist */}
          <aside className="space-y-4">
            <section className="rounded-xl border border-zinc-800 bg-zinc-900/50 p-5">
              <div className="mb-3 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Database className="h-4 w-4 text-emerald-400" />
                  <h3 className="font-serif-display text-2xl text-white">Watchlist</h3>
                </div>
                <span className="font-mono-tech rounded-full border border-zinc-800 bg-zinc-950 px-2 py-0.5 text-[11px] text-zinc-400">
                  {favourites.length}
                </span>
              </div>

              {favourites.length === 0 ? (
                <p className="text-xs leading-relaxed text-zinc-500">
                  No saved stocks yet. Look up any ticker and click &ldquo;Add to Watchlist&rdquo; to persist it in H2.
                </p>
              ) : (
                <div className="space-y-2">
                  {favourites.map((fav) => (
                    <div
                      key={fav.symbol}
                      className="flex items-center justify-between rounded-lg border border-zinc-800 bg-zinc-950/80 px-3 py-2.5 transition-colors hover:border-zinc-700"
                    >
                      <button
                        type="button"
                        onClick={() => {
                          setQuery(fav.symbol);
                          loadStock(fav.symbol, days);
                        }}
                        className="flex flex-1 cursor-pointer items-center justify-between pr-2 text-left"
                      >
                        <span className="font-mono-tech text-xs font-semibold text-white">
                          {fav.symbol}
                        </span>
                        <span className="font-mono-tech inline-flex items-center gap-1 text-xs text-zinc-300">
                          {fav.price > 0 ? `$${fav.price.toFixed(2)}` : 'N/A'}
                          <ArrowUpRight className="h-3 w-3 text-zinc-500" />
                        </span>
                      </button>
                      <button
                        type="button"
                        onClick={() => removeFavourite(fav.symbol)}
                        aria-label={`Remove ${fav.symbol}`}
                        className="cursor-pointer rounded p-1 text-zinc-500 transition-colors hover:bg-zinc-800 hover:text-rose-400"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </section>
          </aside>
        </div>
      </main>
    </div>
  );
}
