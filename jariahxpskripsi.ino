//WIFI SETUP
#include <ESP8266WiFi.h>
#include <EEPROM.h>
#include <ESP8266WebServer.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>

//FIREBASE SETUP
#include <Firebase_ESP_Client.h>
#include <addons/TokenHelper.h>
#include <addons/RTDBHelper.h>
#define API_KEY "AIzaSyABPK4o5AgxjlGoMoRK8p-IbtrDitfQsZ8"
#define USER_EMAIL "adminjariahxp@gmail.com"
#define USER_PASSWORD "12345678"
#define DATABASE_URL "https://skripsijariahxp-default-rtdb.firebaseio.com"
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

//SENSOR SETUP
#include <DHT.h>
#define DHT_SENSOR_PIN  D5
#define DHT_SENSOR_TYPE DHT11
DHT dht_sensor(DHT_SENSOR_PIN, DHT_SENSOR_TYPE);

//PIN SETUP
#define MQ3_AO_PIN A0
#define DOUT  D6
LiquidCrystal_I2C lcd(0x27, 16, 2);


// Definisikan Wi-Fi dan Web Server
const char* defaultSSID = "huhu";
const char* defaultPassword = "hihihihi";
const String defaultDeviceID = "";

const int eepromSsidStart = 0;  // Tempat menyimpan SSID dan Password di EEPROM
const int eepromPasswordStart = 32;
const int eepromDeviceIDStart = 64;

bool wifiConnected = false;

ESP8266WebServer server(80);
bool isInAPMode = false;  
// Fungsi untuk membaca SSID dan Password dari EEPROM

String readEEPROM(int start, int length) {
  String value = "";
  for (int i = 0; i < length; i++) {
    char c = EEPROM.read(start + i);
    if (c == '\0') break;
    value += c;
  }
  return value;
}

// Fungsi untuk menulis SSID dan Password ke EEPROM
void writeEEPROM(int start, String value) {
  for (int i = 0; i < value.length(); i++) {
    EEPROM.write(start + i, value[i]);
  }
  EEPROM.write(start + value.length(), '\0');
  EEPROM.commit();
}

// Fungsi untuk menghubungkan ke Wi-Fi
bool connectToWiFi() {
  
  for (int attempt = 1; attempt <= 2; attempt++) { // Maksimal 2 kali percobaan
    Serial.printf("Percobaan ke-%d untuk menghubungkan ke WiFi...\n", attempt);

    String ssid = readEEPROM(eepromSsidStart, 32);
    String password = readEEPROM(eepromPasswordStart, 32);
    WiFi.begin(ssid, password); // Memulai koneksi Wi-Fi
    unsigned long startTime = millis();

    while (WiFi.status() != WL_CONNECTED && millis() - startTime < 30000) { // Tunggu hingga 30 detik
      delay(500);
      Serial.print(".");
    }

    if (WiFi.status() == WL_CONNECTED) {
      WiFi.softAPdisconnect(true);
      return true; // Berhasil terhubung
    }
    startAP();
    Serial.println("\nKoneksi WiFi gagal. Mencoba ulang...");
  }

  return false; // Gagal setelah 2 kali percobaan
}


// Fungsi untuk memulai akses point (AP) jika gagal terhubung ke Wi-Fi
void startAP() {

  WiFi.softAP("WBOX-SKRIPSI-JARIAHXP", "12345678");
  IPAddress ip = WiFi.softAPIP();
  tulisSerial("AP IP address: ", false); 
  tulisSerial(ip.toString(), true);  

  server.on("/", HTTP_GET, []() {
    String html = "<html><body><h1>Update WiFi Credentials</h1>";
    html += "<form action='/update' method='POST'>";
    html += "SSID: <input type='text' name='ssid'><br>";
    html += "Password: <input type='password' name='password' ><br>";
    html += "Device ID: <input type='text' name='device_id'><br>";
    html += "<input type='submit' value='Connect'>";
    html += "</form></body></html>";
    server.send(200, "text/html", html);
  });

  server.on("/update", HTTP_POST, []() {
    String newSSID = server.arg("ssid");
    String newPassword = server.arg("password");
    String newDeviceID = server.arg("device_id");

    // Menyimpan SSID dan password baru ke EEPROM
    writeEEPROM(eepromSsidStart, newSSID);
    writeEEPROM(eepromPasswordStart, newPassword);
    writeEEPROM(eepromDeviceIDStart, newDeviceID);

    server.send(200, "text/html", "<html><body><h1>Updating WiFi Credentials...</h1><p>Rebooting...</p></body></html>");

    delay(2000);
    ESP.restart();  // Reboot untuk mencoba koneksi dengan SSID dan password baru
  });

  server.begin();
}
void connectToFirebase() {
  tulisSerial("Firebase Client v", false); 
  tulisSerial(FIREBASE_CLIENT_VERSION, true); 

  config.api_key = API_KEY;
  auth.user.email = USER_EMAIL;
  auth.user.password = USER_PASSWORD;
  config.database_url = DATABASE_URL;
  Firebase.reconnectNetwork(true);

  
  fbdo.setBSSLBufferSize(4096, 1024);
  fbdo.setResponseSize(4096);
    config.token_status_callback = tokenStatusCallback;
      config.max_token_generation_retry = 5;

  Firebase.begin(&config, &auth);

  unsigned long startTime = millis();  // Timer
  while (!Firebase.ready()) {
    tulisSerial("Connecting to Firebase...", true);
    delay(1000);
    if (millis() - startTime > 10000) {  // Timeout setelah 10 detik
      tulisSerial("Firebase connection timeout!", true);
      return;
    }
  }

  tulisSerial("Berhasil terhubung ke Firebase!", true);
}


//FUNGSI SERIAL MONITOR + LCD
void tulisSerial(String pesan, bool newline) {
  if (newline) {
    Serial.println(pesan);  // Menambahkan newline
  } else {
    Serial.print(pesan);    // Tidak menambahkan newline
  }

  // Tampilkan ke LCD dengan memotong pesan jika lebih dari 16 karakter
  lcd.clear();
  int len = pesan.length();
  if (len <= 16) {
    lcd.setCursor(0, 0);  // Tampilkan di baris pertama
    lcd.print(pesan);
  } else {
    lcd.setCursor(0, 0);  // Baris pertama
    lcd.print(pesan.substring(0, 16));
    if (len > 32) len = 32;  // Maksimal 2 baris
    lcd.setCursor(0, 1);  // Baris kedua
    lcd.print(pesan.substring(16, len));
  }
}

void bacaSensor(){
  //bacaDHT11();
  bacaMQ3();
}
void bacaDHT11() {
  String id = readEEPROM(eepromDeviceIDStart, 32);
  float humi = dht_sensor.readHumidity();
  float temperature_C = dht_sensor.readTemperature();
  float temperature_F = dht_sensor.readTemperature(true);

  if (isnan(temperature_C) || isnan(temperature_F) || isnan(humi)) {
    tulisSerial("Gagal membaca dari sensor DHT!", true);
  } else {
    // Kirim data ke Firebase
    String pathHumi = "/data/dht11/kelembaban";
    String pathTempC = "/data/dht11/suhu";
    String patha = "/data/" + id + "/kelembapan";  // Gabungkan path dengan id
    String pathb = "/data/" + id + "/suhu";  // Gabungkan path dengan id

    sendDataSensor(patha, "DHT11", String(humi));
    sendDataSensor(pathb, "DHT11", String(temperature_C));

tulisSerial("Suhu:" + String(temperature_C) + "C Kelembapan: " + String(humi) + "%", true);
    delay(1000);
  }
}

// Fungsi untuk membaca data dari MQ-3 dan mengirimkannya ke Firebase
void bacaMQ3() {
  // Membaca ID dari EEPROM
  String id = readEEPROM(eepromDeviceIDStart, 32);

  // Membaca nilai analog dari sensor MQ-3
  int analogValue = analogRead(MQ3_AO_PIN);
  float voltage = analogValue * (3 / 1023.0);

  // Validasi nilai analog agar tidak menghasilkan persen negatif
  int alcoholPercentage;
  if (analogValue < 230) {
    alcoholPercentage = 0; // Jika nilai kurang dari input_min, set ke 0
  } else {
    alcoholPercentage = map(analogValue, 230, 1023, 0, 100);
  }

  // Kirim data ke Firebase
  String path = "/data/" + id + "/alkohol";  // Gabungkan path dengan ID
  sendDataSensor(path, "MQ-3", String(alcoholPercentage));

  // Debugging: Tampilkan nilai analog dan kadar etanol di serial monitor
  Serial.print("Analog Value: ");
  Serial.println(analogValue);
  Serial.print("Kadar Etanol: ");
  Serial.println(String(alcoholPercentage) + "%");

  tulisSerial("Kadar Etanol: " + String(alcoholPercentage) + "%", true);
  delay(1000);
}


String readDeviceID() {
  return readEEPROM(eepromDeviceIDStart, 32);
}
// Setup awal
void setup() {
  Serial.begin(115200);
  EEPROM.begin(512);
  //SETUP SENSOT
  dht_sensor.begin();
  lcd.init();  
  lcd.backlight();  

  // Menampilkan pesan
  lcd.setCursor(0, 0); // Baris pertama, kolom pertama
  lcd.print("Hallo JariahXp");
  lcd.setCursor(0, 1); // Baris kedua, kolom pertama
  lcd.print("Ahmad Ghozali");
  Serial.println("Semua sensor siap...");

  wifiConnected = connectToWiFi();
  if (wifiConnected) {
    Serial.println("Berhasil terhubung ke WiFi!");
    Serial.print("IP Address: ");
    Serial.println(WiFi.localIP());
    connectToFirebase();
  } else {
    Serial.println("Gagal menyambungkan ke WiFi setelah 2 kali percobaan.");
  }
  
}
// Loop utama
void loop() {

  if (wifiConnected) { // Periksa status koneksi Wi-Fi
    if (WiFi.status() != WL_CONNECTED) { // Jika Wi-Fi terputus
      Serial.println("WiFi terputus. Mencoba menyambung kembali...");
      wifiConnected = connectToWiFi(); // Coba menyambungkan kembali

      if (!wifiConnected) { // Jika gagal menyambung lagi
        Serial.println("Gagal menyambung kembali ke WiFi. Berhenti mencoba.");
      }
    } else {
      if (Firebase.isTokenExpired()){
        Firebase.refreshToken(&config);
        Serial.println("Refresh token");
      }
      bacaSensor();
    }
  } else {
    
    server.handleClient();
  } 
}

void sendDataSensor(String path, String jenisSensor, String dataSensor){
  
  if (Firebase.RTDB.setString(&fbdo, path, dataSensor)) {
      Serial.print(jenisSensor);
      Serial.println(" Berhasil Di kirim");
    } else {
      // Menampilkan alasan error jika gagal
      Serial.print("Gagal mengirim ");
      Serial.println(jenisSensor);
      Serial.print("Alasan: ");
      Serial.println(fbdo.errorReason());
    }
}
