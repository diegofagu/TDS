export interface Candle {
  timestamp: string; // ISO timestamp of the start of the candle
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export interface SignalResponse {
  symbol: string;
  interval: string;
  price: number;
  action: string;
  explanation: string;
  score: number;
  candles: Candle[];
}