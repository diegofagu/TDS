import pandas as pd
import numpy as np
from typing import Tuple
import pickle

try:
    from sklearn.model_selection import TimeSeriesSplit
    from sklearn.ensemble import GradientBoostingClassifier
    from sklearn.metrics import classification_report
    SKLEARN = True
except Exception:
    # scikit‑learn may not be installed in some environments
    SKLEARN = False

# List of features used for the model. This subset can be adjusted or
# extended to include additional technical indicators.
FEATURES = [
    'ret_1d', 'ret_7d', 'ret_30d',
    'ema_20', 'ema_50', 'ema_200',
    'rsi_14', 'macd', 'macd_signal', 'macd_hist',
    'bb_mid', 'bb_up', 'bb_lo', 'vol_14'
]

def make_feature_matrix(df: pd.DataFrame) -> pd.DataFrame:
    """
    Construct the feature matrix by selecting relevant columns and
    removing rows with NaNs or infinite values. This ensures the
    classifier receives clean input.
    """
    X = df[FEATURES].copy()
    X = X.replace([np.inf, -np.inf], np.nan).dropna()
    return X

def align_features_labels(df: pd.DataFrame, labels: pd.Series) -> Tuple[pd.DataFrame, pd.Series]:
    """
    Align the feature matrix and label vector so that they have
    matching indices. Rows dropped in the feature matrix are also
    removed from the labels.
    """
    X = make_feature_matrix(df)
    y = labels.reindex(df.index).loc[X.index]
    return X, y

def train_model(df: pd.DataFrame, labels: pd.Series, n_splits: int = 5, random_state: int = 42):
    """
    Train a Gradient Boosting classifier on the provided features and
    labels. Performs time series cross‑validation and returns the final
    fitted model along with classification reports for each fold.
    Requires scikit‑learn to be installed.
    """
    if not SKLEARN:
        raise RuntimeError("scikit-learn is required for training. Please install scikit-learn.")
    X, y = align_features_labels(df, labels)
    tscv = TimeSeriesSplit(n_splits=n_splits)
    reports = []
    for train_idx, test_idx in tscv.split(X):
        Xtr, Xte = X.iloc[train_idx], X.iloc[test_idx]
        ytr, yte = y.iloc[train_idx], y.iloc[test_idx]
        clf = GradientBoostingClassifier(random_state=random_state)
        clf.fit(Xtr, ytr)
        pred = clf.predict(Xte)
        reports.append(classification_report(yte, pred, output_dict=True, zero_division=0))
    final = GradientBoostingClassifier(random_state=random_state)
    final.fit(X, y)
    return final, reports

def save_model(clf, path: str):
    """Persist a trained model to disk using pickle."""
    with open(path, 'wb') as f:
        pickle.dump(clf, f)

def load_model(path: str):
    """Load a model from disk."""
    with open(path, 'rb') as f:
        return pickle.load(f)

def predict_signals(df: pd.DataFrame, clf) -> pd.Series:
    """
    Use a trained classifier to predict signals for each row in the
    feature matrix. Returns a Series indexed like the input DataFrame.
    """
    X = make_feature_matrix(df)
    return pd.Series(clf.predict(X), index=X.index)