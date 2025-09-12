import type { SignalResponse } from './types';

// Determine the base URL for API calls. The value can be overridden by
// defining VITE_API_BASE_URL in a .env file at the root of the front‑end
// project. When not specified the default points to the local backend.
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

/**
 * Retrieve a trading signal from the backend. This helper wraps the
 * underlying fetch call and parses the JSON response.
 *
 * @param symbol   the trading pair (e.g. BTCUSDT)
 * @param interval the candle interval (e.g. 1h, 15m)
 * @param limit    number of candles to fetch
 */
export async function fetchSignal(
  symbol: string,
  interval: string,
  limit: number = 300
): Promise<SignalResponse> {
  const params = new URLSearchParams({ symbol, interval, limit: String(limit) });
  const response = await fetch(`${API_BASE}/signals?${params.toString()}`);
  if (!response.ok) {
    throw new Error(`Error obteniendo datos: ${response.statusText}`);
  }
  const data = (await response.json()) as SignalResponse;
  return data;
}