import React from 'react';

interface Props {
  action: string;
  explanation: string;
  score: number;
  price: number;
}

/**
 * Displays the latest trading signal along with a human‑readable explanation.
 * The action text is colour coded to make it easier to distinguish BUY,
 * SELL and HOLD.
 */
const SignalPanel: React.FC<Props> = ({ action, explanation, score, price }) => {
  const actionColour = action === 'COMPRA' ? '#2e7d32' : action === 'VENTA' ? '#c62828' : '#555555';
  return (
    <div className="signal-panel">
      <h2>
        Señal: <span style={{ color: actionColour }}>{action}</span>
      </h2>
      <p>Precio de referencia: {price.toFixed(2)}</p>
      <p>Score: {score}</p>
      <p>Explicación: {explanation}</p>
    </div>
  );
};

export default SignalPanel;