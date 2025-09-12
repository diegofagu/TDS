from fastapi import FastAPI, UploadFile, File, Form
import io
from data import load_ohlcv_csv, add_indicators, label_future_return
from rules import rule_based_signals
from model import train_model, save_model, load_model, predict_signals

app = FastAPI(title="Crypto Alerts ML Service")

@app.post("/signal/rules")
async def signal_rules(
    file: UploadFile = File(...),
    asset: str = Form("ASSET"),
    timeframe: str = Form("1h")
):
    """
    Compute a trading signal using rule‑based heuristics. Expects
    OHLCV data as a CSV file. Returns a JSON response containing the
    last signal and a human‑readable label.
    """
    raw = await file.read()
    df = load_ohlcv_csv(io.BytesIO(raw))
    df = add_indicators(df)
    sig = rule_based_signals(df)
    last = int(sig.iloc[-1])
    return {
        "asset": asset,
        "timeframe": timeframe,
        "last": last,
        "label": {1: "BUY", 0: "HOLD", -1: "SELL"}[last]
    }

@app.post("/train/model")
async def train(
    file: UploadFile = File(...),
    horizon: int = Form(5),
    up: float = Form(0.03),
    down: float = Form(-0.03)
):
    """
    Train a machine learning model on the provided OHLCV data. The
    parameters horizon, up and down control how labels are generated.
    After training, the model is saved to disk as `model.pkl`.
    Returns a status and the classification reports from cross‑validation.
    """
    raw = await file.read()
    df = load_ohlcv_csv(io.BytesIO(raw))
    df = add_indicators(df)
    y = label_future_return(df, horizon=horizon, up=up, down=down)
    clf, reports = train_model(df, y)
    save_model(clf, "model.pkl")
    return {
        "status": "ok",
        "reports": reports
    }

@app.post("/signal/model")
async def signal_model(file: UploadFile = File(...)):
    """
    Compute a trading signal using the trained machine learning model.
    The model must have been trained and saved via `/train/model`.
    """
    raw = await file.read()
    df = load_ohlcv_csv(io.BytesIO(raw))
    df = add_indicators(df)
    clf = load_model("model.pkl")
    sig = predict_signals(df, clf)
    last = int(sig.iloc[-1])
    return {
        "last": last,
        "label": {1: "BUY", 0: "HOLD", -1: "SELL"}[last]
    }