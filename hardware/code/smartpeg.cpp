// Do not remove the include below
#include "smartpeg.h"

// Temperature and humidity sensor HDC1080, connected to D2 (=SDA) and D1 (=SCL) via I²C
HDC1080 sensor = HDC1080(SDA, SCL);

DHT dht = DHT(PIN_DHT22, DHT22);

// Peg sensor (checks for conductivity of the peg)
Peg peg    = Peg(PIN_PEG);

// Debugging server
WiFiServer server(500);

//The setup function is called once at startup of the sketch
void setup()
{
	// Setup status LED
	pinMode(PIN_LED_WIFI, OUTPUT);

	setupSerialMonitor();
	setupDHTSensor();
	setupHDCSensor();
	setupWiFi();

}

unsigned long lastOutput = 0;

// The loop function is called in an endless loop
void loop()
{
	unsigned long ms = millis();

	if(WiFi.status() != WL_CONNECTED) {
		wifiConnectLoop();
	} else {

		if (ms - lastOutput > 5000) {

			// Show the sensor is alive
			digitalWrite(PIN_LED_WIFI, HIGH);
			delay(50);
			digitalWrite(PIN_LED_WIFI, LOW);
			delay(50);

			float temperature = sensor.getTemperature();
			float humidity    = sensor.getHumidity();

			float temperatureDht = dht.readTemperature();
			float humidityDht    = dht.readHumidity();

			float dryness     = peg.readDryness();
			Serial.print("\"DHC1080\",");
			Serial.print(humidity, 6);
			Serial.print(" ");
			Serial.print(temperature, 6);
			Serial.print(" ");
			Serial.println(dryness, 6);
			Serial.print("\"DHT22\",");
			Serial.print(humidityDht, 6);
			Serial.print(" ");
			Serial.print(temperatureDht, 6);
			Serial.print(" ");
			Serial.println(dryness, 6);
			lastOutput = millis();

			// Trigger next measurement (the sensor needs some time);
			sensor.triggerMeasurement();

			// Start transmitting only after the sensors are ready
			if (temperature + humidity > 0.001f) {

				{
					HTTPClient httpDht22;

					// TODO: Make ID customizable
					httpDht22.begin("http://smartpeg.fhessel.de/smartpeg/peg/1/readings");

					String jsonPayloadDht22   =
						String("{\"humidity\":") +
						String(humidityDht, 5) +
						String(",\"temperature\":") +
						String(temperatureDht, 5) +
						String(",\"conductance\":") +
						String(dryness, 5) +
						String(",\"sensor_type\":") +
						String("\"DHT22\"}");

					int httpStatusCode = httpDht22.POST(jsonPayloadDht22);

					if (httpStatusCode != 200) {
						Serial.print("HTTP POST for DHT22 failed. Got return code: ");
						Serial.println(httpStatusCode, DEC);
						for(int i = 1; i < 10; i++) {
							digitalWrite(PIN_LED_WIFI, HIGH);
							delay(50);
							digitalWrite(PIN_LED_WIFI, LOW);
							delay(50);
						}
					}
					httpDht22.end();
				}

				{
					HTTPClient httpHdc1080;

					// TODO: Make ID customizable
					httpHdc1080.begin("http://smartpeg.fhessel.de/smartpeg/peg/1/readings");

					String jsonPayloadHdc1080 =
						String("{\"humidity\":") +
						String(humidity, 5) +
						String(",\"temperature\":") +
						String(temperature, 5) +
						String(",\"conductance\":") +
						String(dryness, 5) +
						String(",\"sensor_type\":") +
						String("\"HDC1080\"}");

					int httpStatusCode = httpHdc1080.POST(jsonPayloadHdc1080);

					if (httpStatusCode != 200) {
						Serial.print("HTTP POST for HDC1080 failed. Got return code: ");
						Serial.println(httpStatusCode, DEC);
						for(int i = 1; i < 10; i++) {
							digitalWrite(PIN_LED_WIFI, HIGH);
							delay(50);
							digitalWrite(PIN_LED_WIFI, LOW);
							delay(50);
						}
					}

					httpHdc1080.end();
				}

			} else {
				Serial.println("Sensors are not ready, not transmitting");
			}

		}
	}
}

void setupHDCSensor() {
	// Initialize the library. Easy.
	Serial.print("Setting up HDC1080 sensor...");
	uint8_t err = sensor.begin();
	if (err) {
		Serial.print(" Failed");
		while(1) {
			delay(1000);
			Serial.print(".");
		}
	}
	Serial.println(" Done.");
	Serial.print("Triggering initial read...");
	sensor.triggerMeasurement();
	Serial.println(" Done.");
}

void setupWiFi() {
	Serial.print("Connecting to WiFi...");

	wifiConnectLoop();

	// Print some status information
	Serial.println(" Done.");
	Serial.print("My IP is ");
    Serial.println(WiFi.localIP());
}

void wifiConnectLoop() {
	digitalWrite(PIN_LED_WIFI, HIGH);

	// We count the tries (=500ms steps) and reconnect every 15 tries
	// until we get a connection
	unsigned int tries = 0;

	do {
		// This will make the led blink
		digitalWrite(PIN_LED_WIFI, (tries&1) ? HIGH : LOW);

		// Retry required
		if (tries % 15 == 0) {
			if (tries > 0) {
				Serial.print("\nRetrying...");
				WiFi.disconnect();

				tries = 0;
				delay(1000);
			}
			WiFi.begin(WIFI_SSID, WIFI_PSK);
		}
		Serial.print(".");
		delay(500);
		tries+=1;
	} while(WiFi.status() != WL_CONNECTED);

	// Set the LED low to show the connection has been established
	digitalWrite(PIN_LED_WIFI, LOW);
}

void setupSerialMonitor() {
	// Setup the serial port for debugging.
	// Include a delay to make the connection more stable
	// (without delay, some output might be missing)
	Serial.begin(115200);
	delay(2000);
}

void setupDHTSensor() {
	Serial.print("Setting up DHT22 sensor...");
	dht.begin();
	Serial.println(" Done.");
}

