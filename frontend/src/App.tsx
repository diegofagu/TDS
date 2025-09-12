import { useState, useEffect } from 'react';
import Chart from './components/Chart';
import SignalPanel from './components/SignalPanel';
import { fetchSignal } from './api';
import type { SignalResponse } from './types';

/**
 * Main component orchestrating the user interface. Handles state for
 * selected symbol/interval, fetches data from the backend and renders
 * both the candlestick chart and the signal panel. Data is automatically
 * refreshed every minute.
 */
const App = () => {
  const [symbol, setSymbol] = useState('BTCUSDT');
  const [interval, setInterval] = useState('1h');
  const [data, setData] = useState<SignalResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadData = async () => {
    try {
      setLoading(true);
      const result = await fetchSignal(symbol, interval, 300);
      setData(result);
      setError(null);
    } catch (err: any) {
      setError(err?.message ?? 'Error desconocido');
    } finally {
      setLoading(false);
    }
  };

  // Load data when component mounts and whenever the symbol or interval changes
  useEffect(() => {
    loadData();
    // Set up periodic refresh every minute
    const id = setInterval(loadData, 60_000);
    return () => clearInterval(id);
  }, [symbol, interval]);

  return (
    <div className="container">
      <h1>Crypto Alerts</h1>
      <div className="controls">
        <label>
          Símbolo
          <select value={symbol} onChange={(e) => setSymbol(e.target.value)}>
            <option value="BTCUSDT">BTC/USDT</option>
            <option value="ETHUSDT">ETH/USDT</option>
          </select>
        </label>
        <label>
          Intervalo
          <select value={interval} onChange={(e) => setInterval(e.target.value)}>
            <option value="1m">1m</option>
            <option value="5m">5m</option>
            <option value="15m">15m</option>
            <option value="1h">1h</option>
            <option value="4h">4h</option>
            <option value="1d">1d</option>
          </select>
        </label>
        <button onClick={loadData} disabled={loading}>
          {loading ? 'Actualizando…' : 'Actualizar'}
        </button>
      </div>
      {error && <div className="error">{error}</div>}
      {data && (
        <>
          <Chart candles={data.candles} />
          <SignalPanel
            action={data.action}
            explanation={data.explanation}
            score={data.score}
            price={data.price}
          />
        </>
      )}
    </div>
  );
};

export default App;