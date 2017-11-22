// Do not remove the include below
#include "smartpeg.h"

// Temperature and humidity sensor HDC1080, connected to D2 (=SDA) and D1 (=SCL) via I²C
HDC1080 sensor = HDC1080(SDA, SCL);

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
	setupHDCSensor();
	setupWiFi();

	// FIXME: As long as the REST API is not functional, the peg will
	// provide its measurements either through serial console or on
	// TCP port 500, where 1 CLIENT AT A TIME can connect. A CSV-like
	// output is provided.
    server.begin();
}

WiFiClient client;
bool hasConnection = false;
unsigned long lastOutput = 0;

// The loop function is called in an endless loop
void loop()
{
	unsigned long ms = millis();

	if (!hasConnection) {
		client = server.available();
		if (client) {
			hasConnection = true;
			client.setNoDelay(false);
			client.println("\"time_ms\",\"humidity_percent\",\"temperature_degc\",\"dryness_raw\"");
		}
	}

	if (ms - lastOutput > 5000) {
		float temperature = sensor.getTemperature();
		float humidity    = sensor.getHumidity();
		float dryness     = peg.readDryness();
		Serial.print(humidity, 6);
		Serial.print(" ");
		Serial.print(temperature, 6);
		Serial.print(" ");
		Serial.println(dryness, 6);
		lastOutput = millis();

		// Trigger next measurement (the sensor needs some time);
		sensor.triggerMeasurement();


		if (client){

			while(client.available()>0) {
				client.read();
			}

			if (client.connected()) {

				client.print(ms, DEC);
				client.print(",");
				client.print(humidity, 6);
				client.print(",");
				client.print(temperature, 6);
				client.print(",");
				client.println(dryness, 6);
				client.flush();

			} else {
				client.stop();
				hasConnection = false;
			}
		}
	}
}

void setupHDCSensor() {
	// Initialize the library. Easy.
	Serial.print("Setting up sensor...");
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

	// Set the LED high to show the connection has been established
	// TODO: For the final release, set this to LOW to save energy
	digitalWrite(PIN_LED_WIFI, HIGH);

	// Print some status information
	Serial.println(" Done.");
	Serial.print("My IP is ");
    Serial.println(WiFi.localIP());
}

void setupSerialMonitor() {
	// Setup the serial port for debugging.
	// Include a delay to make the connection more stable
	// (without delay, some output might be missing)
	Serial.begin(115200);
	delay(2000);
}
