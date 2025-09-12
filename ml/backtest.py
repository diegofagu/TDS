import pandas as pd
import numpy as np

def backtest(df: pd.DataFrame, signals: pd.Series, fee_bps: float = 10.0) -> pd.DataFrame:
    """
    Simple vectorised backtester. Assumes positions are either long (+1),
    flat (0) or short (−1) as provided in the `signals` series. Trades
    occur whenever the signal changes. Transaction fees are accounted for
    via basis points (fee_bps).

    Returns a DataFrame with daily equity curve and stores summary
    metrics (CAGR, max drawdown, Sharpe ratio) in the DataFrame's
    attributes for convenience.
    """
    out = df[['date', 'close']].copy()
    out = out.join(signals.rename('signal'))
    # Forward fill signals and cap at ±1
    out['signal'] = out['signal'].ffill().fillna(0).clip(-1, 1).astype(int)
    trades = out['signal'].diff().abs().fillna(0)
    fee = trades * (fee_bps / 10000.0)
    ret = out['close'].pct_change().fillna(0.0)
    strat_ret = out['signal'].shift(1).fillna(0) * ret - fee
    equity = (1 + strat_ret).cumprod()
    metrics = {
        'CAGR': (equity.iloc[-1]) ** (365 / len(equity)) - 1 if len(equity) > 0 else 0.0,
        'MaxDrawdown': float((equity / equity.cummax() - 1).min()),
        'Sharpe': float(np.sqrt(365) * (strat_ret.mean() / (strat_ret.std(ddof=0) + 1e-12))),
    }
    res = pd.DataFrame({
        'date': out['date'],
        'close': out['close'],
        'signal': out['signal'],
        'strat_ret': strat_ret,
        'equity': equity,
    })
    res.attrs['metrics'] = metrics
    return res