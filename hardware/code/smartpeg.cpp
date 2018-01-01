// Do not remove the include below
#include "smartpeg.h"

#ifdef USE_HDC1080
// Temperature and humidity sensor HDC1080, connected to D2 (=SDA) and D1 (=SCL) via I²C
HDC1080 sensor = HDC1080(SDA, SCL);
#endif

#ifdef USE_DHT22
DHT dht = DHT(PIN_DHT22, DHT22);
#endif

// Peg sensor (checks for conductivity of the peg)
Peg peg    = Peg(PIN_PEG);

String url = "http://smartpeg.fhessel.de/smartpeg/peg/" + String(PEG_ID) + "/readings";

//The setup function is called once at startup of the sketch
void setup()
{
	// Setup status LED
	pinMode(PIN_LED_WIFI, OUTPUT);

	setupSerialMonitor();

	// Signal initiation of setup
	for (int i = 0; i < 3; i++) {
		delay(500);
		digitalWrite(PIN_LED_WIFI, HIGH);
		delay(500);
		digitalWrite(PIN_LED_WIFI, LOW);
	}

// Configure sensors depending on the configuration in config/measurement.h
#ifdef USE_DHT22
	setupDHTSensor();
#endif
#ifdef USE_HDC1080
	setupHDCSensor();
#endif
#ifndef USE_HDC1080
#ifndef USE_DHT22
	Serial.println("ERROR: NO SENSOR CONFIGURED. PLEASE ACTIVATE EITHER \"USE_DHT22\" OR");
	Serial.println("       \"USE_HDC1080\" IN config/measurement.h!");
#endif
#endif
	setupWiFi();

	Serial.println(String("Trasmitting values to ") + url);

	// Signal finish of setup
	for (int i = 0; i < 3; i++) {
		delay(800);
		digitalWrite(PIN_LED_WIFI, HIGH);
		delay(200);
		digitalWrite(PIN_LED_WIFI, LOW);
	}
}

// The loop function is called in an endless loop
void loop()
{
	// Show the sensor is alive
	digitalWrite(PIN_LED_WIFI, HIGH);
	delay(50);
	digitalWrite(PIN_LED_WIFI, LOW);
	delay(50);

	float dryness     = peg.readDryness();

#ifdef USE_HDC1080
	float temperature = sensor.getTemperature();
	float humidity    = sensor.getHumidity();

	// Start transmitting only after the sensors are ready
	if (temperature + humidity > 0.001f) {
		// humidity, temperature, dryness
		storeMeasurement(temperature, humidity, dryness, SENSOR_TYPE_HDC1080);
	} else {
		Serial.println("HDC Sensor is not ready, not recording any values");
	}
#endif

#ifdef USE_DHT22
	float temperatureDht = dht.readTemperature();
	float humidityDht    = dht.readHumidity();

	storeMeasurement(temperatureDht, humidityDht, dryness, SENSOR_TYPE_DHT22);
#endif

#ifdef USE_HDC1080
	// Trigger next measurement (the sensor needs some time);
	sensor.triggerMeasurement();
#endif

	// The following code is the strategy to keep WiFi off for most of the time to save energy.
	// We store all measurements in an array and only transmit them if the array is nearly full
	// (nearly full to avoid dismissing values if the transmission fails)

	// Check remaining storage space (in periods, that's why we devide by PER_STEP).
	int storageSpaceLeft = (MEASUREMENT_STORAGE_SIZE - storedMeasurementsPointer) / MEASUREMENTS_PER_STEP;

	Serial.print("Measurements have been taken. ");
	Serial.print(storedMeasurementsPointer);Serial.print(" of ");Serial.print(MEASUREMENT_STORAGE_SIZE);
	Serial.print(" are used. ");Serial.print(storageSpaceLeft);Serial.println(" periods until buffer is full");

	// If less than TRANSMIT_PERCENTAGE% space is left, prepare value transmission
	if (storedMeasurementsPointer >= (MEASUREMENT_STORAGE_SIZE*TRANSMIT_PERCENTAGE)/100) {
		// This value is required to calculate the time that has been used for communication
		unsigned long wifiTime = millis();

		// At first, try to establish a wifi connection
		if (WiFi.status() != WL_CONNECTED) {
			WiFi.forceSleepWake();
			wifiConnectLoop();

			// If the connection could not be established
			if (WiFi.status() != WL_CONNECTED) {
				// Randomly use disconnect() sometimes, as the esp8266 somehow "needs" this
				if (random(100) < 33) {
					WiFi.disconnect();
				}
			}
		} else if (WiFi.status() == WL_CONNECTED) {
			// If WiFi has been connected, transmit values
			// We use else if above because connecting takes some seconds and this would mean
			// transmission is interrupted short after it has started.
			transmitMeasurements();
			if (storedMeasurementsPointer == 0) {
				// This means success, so we cann disable wifi again.
				WiFi.disconnect();
				WiFi.forceSleepBegin();
			}
		}

		// check how much time we used for communcation etc.
		wifiTime = millis()-wifiTime;
		if (wifiTime < MEASUREMENT_PERIOD) {
			// If communication took less than measurement_period, then sleep
			// for the remaining time
			delay(MEASUREMENT_PERIOD - wifiTime);
		}
	} else {
		// Sleep just until the next measurement
		delay(MEASUREMENT_PERIOD);
	}

}

void transmitMeasurements() {
	unsigned long ms = millis();



	// We need to batch the data transmissions as HTTPClient does not like IP segmentation.
	// With an MTU of ~1400 byte payload, using 8 entries per request leaves roughly 175 byte
	// per entry, which should suffice. One could make this dynamic by checking the string length
	// but this seens to be a fairly good approach.
	const int valuesPerTransmission = 8;
	while(transmittedMeasurementsPointer < storedMeasurementsPointer) {
		int lastIdx = (transmittedMeasurementsPointer+valuesPerTransmission < storedMeasurementsPointer ?
				transmittedMeasurementsPointer+valuesPerTransmission :
				storedMeasurementsPointer);

		HTTPClient httpClient;
		httpClient.begin(url);
		httpClient.addHeader("Content-Type", "application/json");

		// Start the json array
		String jsonPayload = "[";
		for(int i = transmittedMeasurementsPointer; i < lastIdx; i++) {
			// For all but the first entry, add a comma to the json string
			if (i > transmittedMeasurementsPointer) {
				jsonPayload+=",";
			}

			// Create the JSON for a single entry
			measurement_t * measurement = (&storedMeasurements[i]);

			String sensorType = "unknown";
#ifdef USE_DHT22
			if (measurement->sensor_type == SENSOR_TYPE_DHT22) sensorType="DHT22";
#endif
#ifdef USE_HDC1080
			if (measurement->sensor_type == SENSOR_TYPE_HDC1080) sensorType="HDC1080";
#endif
			jsonPayload +=
				String("{\"humidity\":") +
				String(measurement->humidity, 5) +
				String(",\"temperature\":") +
				String(measurement->temperature, 5) +
				String(",\"conductance\":") +
				String(measurement->dryness, 5) +
				String(",\"sensor_type\":\"") +
				sensorType +
				String("\",\"timeOffset\":-") +
				String( (ms-(measurement->time_ms))/1000 ) +
				String("}");

			// If this was the last entry, terminate the json array
			if (i == lastIdx -1) {
				jsonPayload+="]";
			}
		}

		// Send the request
		int httpStatusCode = httpClient.POST(jsonPayload);

		if (httpStatusCode >= 200 && httpStatusCode < 300) {
			// Successful, use the next batch
			transmittedMeasurementsPointer = lastIdx;
			Serial.print("Partial Transmission successful.");
		} else {
			// An error occured
			Serial.print("HTTP POST failed. Got return code: ");
			Serial.print(httpStatusCode, DEC);

			// Show the error using the LED
			for(int i = 1; i < 10; i++) {
				digitalWrite(PIN_LED_WIFI, HIGH);
				delay(50);
				digitalWrite(PIN_LED_WIFI, LOW);
				delay(50);
			}

			if (httpStatusCode >= 400 && httpStatusCode < 500) {
				Serial.print("\n -> Skipping this batch (4xx = Client error, retry will not help)\n");
				Serial.print(" -> JSON was:\n    ");
				Serial.print(jsonPayload);
				Serial.print("\n   ");
				transmittedMeasurementsPointer = lastIdx;
			}
		}
		Serial.print(" (");
		Serial.print(transmittedMeasurementsPointer);
		Serial.print(" of ");
		Serial.print(storedMeasurementsPointer);
		Serial.println(" transmitted)");
		httpClient.end();


		if (
				// Interrupt if the measurement_period has been exceeded badly.
				(millis() - ms > MEASUREMENT_PERIOD * 2) &&
				// Only if transmission isn't complete
				(storedMeasurementsPointer > transmittedMeasurementsPointer) &&
				// Only if there's space left in the buffer (where should we store the measurement otherwise?!)
				(storedMeasurementsPointer < MEASUREMENT_STORAGE_SIZE - MEASUREMENTS_PER_STEP)
		) {
			Serial.println("Interrupting transmission to take a measurement");
			return;
		}
	}
	// Reset buffer pointers
	Serial.println("All values have been transmitted successfully");
	storedMeasurementsPointer = 0;
	transmittedMeasurementsPointer = 0;

}

void storeMeasurement(float temp, float hum, float dryness, int sensor_type) {
	// Validation (check NaN)
	if (temp != temp || hum != hum) {
		Serial.println("Ommitting measurement (some value was NaN)");
		return;
	}

	if (storedMeasurementsPointer < MEASUREMENT_STORAGE_SIZE) {
		measurement_t * measurement = (&storedMeasurements[storedMeasurementsPointer]);

		measurement->dryness     = dryness;
		measurement->humidity    = hum;
		measurement->temperature = temp;
		measurement->sensor_type = sensor_type;
		measurement->time_ms     = millis();

		storedMeasurementsPointer += 1;
	} else {
		Serial.println("Cannot store more measurements. Dropping current measurement!");
		for(int i = 1; i < 4; i++) {
			digitalWrite(PIN_LED_WIFI, HIGH);
			delay(150);
			digitalWrite(PIN_LED_WIFI, LOW);
			delay(50);
		}
	}
}

#ifdef USE_HDC1080
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
#endif

void setupWiFi() {
	Serial.print("Setting up WiFi...");

	// Turn of system clock between transmissions
	WiFi.mode(WIFI_STA);
	WiFi.setSleepMode(WIFI_LIGHT_SLEEP);
	WiFi.disconnect();
	WiFi.forceSleepBegin();

	// Print some status information
	Serial.println(" Done.");
}

void wifiConnectLoop() {
	Serial.println("Trying to connect to WiFi");
	digitalWrite(PIN_LED_WIFI, HIGH);

	WiFi.begin(WIFI_SSID, WIFI_PSK);
	for(int tries = 0; tries < MEASUREMENT_PERIOD/250 && WiFi.status() != WL_CONNECTED; tries++) {
		// This will make the led blink
		digitalWrite(PIN_LED_WIFI, (tries&1) ? HIGH : LOW);

		Serial.print(".");
		delay(250);
	}

	if (WiFi.status() == WL_CONNECTED) {
		Serial.println(" Connected!");
	} else {
		Serial.println(" No connection...");
	}

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

#ifdef USE_DHT22
void setupDHTSensor() {
	Serial.print("Setting up DHT22 sensor...");
	dht.begin();
	Serial.println(" Done.");
}
#endif
