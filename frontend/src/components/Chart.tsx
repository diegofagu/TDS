import { useEffect, useRef } from 'react';
import { createChart, Time } from 'lightweight-charts';
import type { Candle } from '../types';

interface ChartProps {
  candles: Candle[];
}

/**
 * Renders a candlestick chart using the lightweight‑charts library. The
 * component creates the chart when the component is mounted and updates
 * its data whenever the candle array changes. A resize observer keeps
 * the chart responsive to container size changes.
 */
const Chart = ({ candles }: ChartProps) => {
  const chartContainerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const container = chartContainerRef.current;
    if (!container || candles.length === 0) {
      return;
    }
    // Clear any previous chart
    container.innerHTML = '';
    const chart = createChart(container, {
      width: container.clientWidth,
      height: 300,
      layout: {
        background: { color: '#ffffff' },
        textColor: '#333333',
      },
      grid: {
        vertLines: { color: '#ebebeb' },
        horzLines: { color: '#ebebeb' },
      },
      timeScale: {
        timeVisible: true,
        secondsVisible: false,
      },
    });
    const series = chart.addCandlestickSeries();
    // Convert ISO timestamp into seconds for lightweight-charts
    const chartData = candles.map((c) => ({
      time: Math.floor(new Date(c.timestamp).getTime() / 1000) as Time,
      open: c.open,
      high: c.high,
      low: c.low,
      close: c.close,
    }));
    series.setData(chartData);
    // Resize observer to keep the chart responsive
    const resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        if (entry.target === container) {
          const { width, height } = entry.contentRect;
          chart.applyOptions({ width, height });
        }
      }
    });
    resizeObserver.observe(container);
    return () => {
      resizeObserver.disconnect();
      chart.remove();
    };
  }, [candles]);

  return <div ref={chartContainerRef} style={{ width: '100%', height: 300 }} />;
};

export default Chart;