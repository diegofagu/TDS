import pandas as pd

def rule_based_signals(df: pd.DataFrame) -> pd.Series:
    """
    Generate trading signals based on simple heuristic rules combining trend,
    momentum, oscillator and volatility conditions. The returned series
    contains values 1 (BUY), -1 (SELL) or 0 (HOLD).

    Rules include:
      - Trend direction using EMA50 and EMA200
      - MACD crossovers
      - RSI thresholds
      - Volatility filters via quantiles
      - Mean reversion at Bollinger band extremes

    These rules are illustrative and can be tuned or extended as needed.
    """
    s = pd.Series(0, index=df.index, dtype=int)
    bull_trend = (df['close'] > df['ema_50']) & (df['ema_50'] > df['ema_200'])
    bear_trend = (df['close'] < df['ema_50']) & (df['ema_50'] < df['ema_200'])
    macd_cross_up = (df['macd'] > df['macd_signal']) & (df['macd'].shift(1) <= df['macd_signal'].shift(1))
    macd_cross_dn = (df['macd'] < df['macd_signal']) & (df['macd'].shift(1) >= df['macd_signal'].shift(1))
    rsi_buy_zone = (df['rsi_14'] < 60) & (df['rsi_14'] > 35)
    rsi_sell_zone = (df['rsi_14'] > 40) & (df['rsi_14'] < 70)
    # Volatility filter: below 85th percentile is considered low
    low_vol = df['vol_14'] < df['vol_14'].quantile(0.85)
    # Primary trend + momentum + volatility rule
    buy = bull_trend & macd_cross_up & rsi_buy_zone & low_vol
    sell = (bear_trend & macd_cross_dn & rsi_sell_zone) | (~low_vol & macd_cross_dn)
    s[buy] = 1
    s[sell] = -1
    # Secondary mean reversion rule at Bollinger extremes
    mean_revert_sell = df['close'] > df['bb_up']
    mean_revert_buy = df['close'] < df['bb_lo']
    s[mean_revert_buy] = 1
    s[mean_revert_sell] = -1
    # Carry forward the last non‑zero signal for a couple of periods to reduce noise
    k = 2
    s = s.replace(0, pd.NA)
    for _ in range(k):
        s = s.ffill()
    s = s.fillna(0).astype(int)
    return s