# 📱 Crypto Alert App

Final Project – CFGS Desarrollo de Aplicaciones Multiplataforma (DAM)

Crypto Alert App is a native Android application developed in Java that allows users to monitor cryptocurrency prices, configure custom price alerts, and receive push notifications when target prices are reached — even when the app is closed.

The app also includes a Whale Alerts module to display large cryptocurrency transactions between wallets and exchanges.

---

## 🎯 Project Objectives

- Display cryptocurrency market data
- Allow users to create and manage price alerts
- Store alerts locally using Room
- Run background checks using a Foreground Service
- Send local push notifications
- Integrate Whale transaction tracking
- Implement light / dark mode

---

## 🏗 Architecture

The application follows a layered Android architecture:

- Activities (UI layer)
- Adapters (RecyclerView)
- Repository pattern (MarketRepository)
- Local database (Room)
- Background processing (Foreground Service / WorkManager)
- REST API consumption (Retrofit)

---

## 📌 Main Features

### 📊 Market Screen
- List of cryptocurrencies
- Filters: Hot / Gainers / Losers
- Search by symbol
- 24h variation display

### 📈 Coin Detail Screen
- Candlestick chart (Binance API)
- Detailed price view
- Alert creation dialog

### 🔔 Alerts System
- Create custom price alerts
- Local persistence with Room
- Edit / Delete alerts
- Background periodic checking
- Automatic push notifications

### 🐋 Whale Alerts Module
- Large crypto transaction tracking
- Simplified transaction summary
- JSON fallback for offline mode

### 🌙 UI & UX
- Bottom Navigation
- Dark / Light Mode
- Responsive layouts
- RecyclerView optimization

---

## 🛠 Technologies Used

- Android Studio
- Java
- XML
- Room Database
- Retrofit
- Gson
- RecyclerView
- Foreground Service
- WorkManager
- SharedPreferences
- CoinPaprika API
- Binance API

---

## 📂 Project Structure

```
com.cryptoalertapp
│
├── activities
├── adapters
├── database (Room)
├── models
├── repository
├── services
├── workers
└── api
```

---

## 🚀 Installation

1. Clone the repository:
```
git clone https://github.com/yourusername/crypto-alert-app.git
```

2. Open in Android Studio

3. Sync Gradle

4. Run on emulator or physical device

---

## 🔐 Permissions

- Internet access
- Foreground service
- Notifications

---

## 📷 Screenshots

<img width="1080" height="2400" alt="CoinChartActivity-notify" src="https://github.com/user-attachments/assets/770bf18f-f14d-4e7c-a087-97ef75eb244c" />


---

## 🔮 Future Improvements

- User accounts & cloud sync
- Real-time WebSocket updates
- Configurable alert intervals
- Advanced analytics dashboard
- Portfolio tracking system

---

## 📄 License

Academic project – Educational use.
