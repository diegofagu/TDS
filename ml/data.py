import pandas as pd
import numpy as np

def load_ohlcv_csv(path_or_buf) -> pd.DataFrame:
    """
    Load OHLCV data from a CSV file. Columns are matched case
    insensitively. The function expects at least a date or timestamp
    column and a close column. Additional open, high, low and volume
    columns will be used if present.

    The returned DataFrame is sorted ascending by date and contains a
    datetime index.
    """
    df = pd.read_csv(path_or_buf)
    # Map columns to lowercase for matching
    cols = {c.lower(): c for c in df.columns}
    mapping = {}
    # Identify timestamp/date column
    for key in ['timestamp', 'date']:
        if key in cols:
            mapping['date'] = cols[key]
            break
    # Identify OHLCV columns
    for key in ['open', 'high', 'low', 'close', 'volume']:
        if key in cols:
            mapping[key] = cols[key]
    if 'date' not in mapping or 'close' not in mapping:
        raise ValueError("CSV must contain at least date/timestamp and close columns.")
    # Rename columns to canonical names
    df = df.rename(columns={v: k for k, v in mapping.items()})
    # Parse dates and sort
    df['date'] = pd.to_datetime(df['date'], utc=True, errors='coerce')
    df = df.sort_values('date').reset_index(drop=True)
    df = df.dropna(subset=['close'])
    return df

def ema(series: pd.Series, span: int) -> pd.Series:
    """
    Exponential moving average.
    """
    return series.ewm(span=span, adjust=False).mean()

def rsi(series: pd.Series, period: int = 14) -> pd.Series:
    """
    Relative Strength Index (RSI) implementation based on exponential
    averaging of up/down moves.
    """
    delta = series.diff()
    up = np.where(delta > 0, delta, 0.0)
    down = np.where(delta < 0, -delta, 0.0)
    roll_up = pd.Series(up, index=series.index).ewm(alpha=1/period, adjust=False).mean()
    roll_down = pd.Series(down, index=series.index).ewm(alpha=1/period, adjust=False).mean()
    rs = roll_up / (roll_down + 1e-12)
    return 100.0 - (100.0 / (1.0 + rs))

def macd(series: pd.Series, fast: int = 12, slow: int = 26, signal: int = 9):
    """
    Moving Average Convergence Divergence (MACD) calculation.
    Returns MACD line, signal line and histogram.
    """
    ema_fast = ema(series, fast)
    ema_slow = ema(series, slow)
    macd_line = ema_fast - ema_slow
    signal_line = ema(macd_line, signal)
    hist = macd_line - signal_line
    return macd_line, signal_line, hist

def bollinger(series: pd.Series, window: int = 20, n_std: float = 2.0):
    """
    Bollinger Bands. Returns tuple of (middle, upper, lower).
    """
    ma = series.rolling(window).mean()
    std = series.rolling(window).std(ddof=0)
    upper = ma + n_std * std
    lower = ma - n_std * std
    return ma, upper, lower

def realized_vol(series: pd.Series, window: int = 14):
    """
    Annualised realised volatility estimate based on daily returns.
    """
    returns = series.pct_change()
    vol = returns.rolling(window).std(ddof=0) * np.sqrt(365)
    return vol

def add_indicators(df: pd.DataFrame) -> pd.DataFrame:
    """
    Compute a collection of indicators and return a new DataFrame with
    the additional columns appended. This includes EMAs, RSI, MACD,
    Bollinger Bands, volatility and various returns.
    """
    out = df.copy()
    out['ema_20'] = ema(out['close'], 20)
    out['ema_50'] = ema(out['close'], 50)
    out['ema_200'] = ema(out['close'], 200)
    out['rsi_14'] = rsi(out['close'], 14)
    macd_line, signal_line, hist = macd(out['close'])
    out['macd'] = macd_line
    out['macd_signal'] = signal_line
    out['macd_hist'] = hist
    ma, bb_up, bb_lo = bollinger(out['close'])
    out['bb_mid'] = ma
    out['bb_up'] = bb_up
    out['bb_lo'] = bb_lo
    out['vol_14'] = realized_vol(out['close'], 14)
    # future returns for modelling
    out['ret_1d'] = out['close'].pct_change(1)
    out['ret_7d'] = out['close'].pct_change(7)
    out['ret_30d'] = out['close'].pct_change(30)
    return out

def label_future_return(df: pd.DataFrame, horizon: int = 5, up: float = 0.03, down: float = -0.03, col: str = 'close'):
    """
    Label each row based on the future return over the specified horizon.
    Returns a Series of ints: 1 for up moves, -1 for down moves, 0 otherwise.
    """
    fut_ret = df[col].shift(-horizon) / df[col] - 1.0
    labels = pd.Series(0, index=df.index)
    labels = labels.mask(fut_ret >= up, 1)
    labels = labels.mask(fut_ret <= down, -1)
    return labels.astype(int)