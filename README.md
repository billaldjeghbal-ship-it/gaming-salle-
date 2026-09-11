# Gaming Salle

Professional Android MVP for managing a PlayStation / gaming hall.

## Features
- Hall setup: name, device count, default hourly price
- PS4 / PS5 / PC devices
- Start/stop sessions with live elapsed time
- Automatic DZD session calculation
- Dashboard: active devices, hours, sessions, revenue
- Expenses and approximate profit
- Device editing
- Reports and settings
- Dark/neon gaming theme
- Local persistence with SharedPreferences

## Cloud build
GitHub Actions workflow builds a debug APK and an unsigned release AAB.

> Release signing and Google Play App Signing still need to be configured before production publishing.

## Next commercial phase
- Owner/worker accounts and permissions
- PIN lock
- Products, stock, sales and invoices
- Bookings
- Advanced daily/weekly/monthly reports
- Backup/restore and cloud sync
- Arabic/French localization
- Google Play Billing / licensing
