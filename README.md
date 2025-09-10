# citizen-sci-io-app
The *citizen-sci-io-app* repository hosts an Android application designed for citizen science participation. Volunteers can sign in, browse available projects, and contribute observations (photos, metadat, and geotagged data) through a step‑by‑step interface. Key features include:
- Project browsing & user accounts – Users authenticate, select a project, and manage their profile settings.
- Structured data collection – A stepper-based workflow supports image capture, metadata records, and location checks.
- Background uploads – A foreground service uploads collected data (with metadata and images) to a remote API.
- Location & mapping – Integrates Google Maps, location services, and permissions for tracking and display.
- Networking stack – Uses Retrofit with token-based authentication, cookies, and JSON serialization.
- Built with Java, AndroidX components, and Gradle, the app targets SDK 35+ (compile SDK 36) and leverages libraries like Glide, Google Play Services, and the Material stepper framework.

URL backend: https://citizen-sci-io-c296af702ec9.herokuapp.com

## Login for admin
- username: admin
- password: citizen

## Login for user
- username: citizen
- password: scientist

To build the project, you need to add these files where the values can be obtained after creating a project from console.cloud.google.com and console.firebase.google.com: 
`/app/src/debug/res/values/google_maps_api.xml`
`/app/src/release/res/values/google_maps_api.xml`
`/app/google-services.json`