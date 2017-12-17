// Only modify this file to include
// - function definitions (prototypes)
// - include files
// - extern variable definitions
// In the appropriate section

#ifndef _smartpeg_H_
#define _smartpeg_H_
#include "Arduino.h"

// WiFi Settings
#include "config/wificonfig.h"

// Measurement configuration
#include "config/measurement.h"

// Pin configuration
#include "config/pins.h"

// WiFi
#include <ESP8266WiFi.h>

// HTTP
#include <ESP8266HTTPClient.h>

// Temperature and humidity sensors
#ifdef USE_HDC1080
#include "sensors/HDC1080.h"
#endif
#ifdef USE_DHT22
#include <DHT.h>
#endif

// Peg sensor
#include "sensors/Peg.h"

// Stores the data of one measurement
struct measurement_t {
	// The internal time on which the measurement has been taken
	// (will be converted to timeOffset on transmission)
	unsigned long time_ms;
	// Name of the reporting sensor
	int           sensor_type;
	// temperature value
	float         temperature;
	// humidity value
	float         humidity;
	// conductance value
	float         dryness;
} storedMeasurements[MEASUREMENT_STORAGE_SIZE];

// Buffer pointers for the value storage
int storedMeasurementsPointer = 0;
int transmittedMeasurementsPointer = 0;

void setupSerialMonitor();
#ifdef USE_DHT22
#define SENSOR_TYPE_DHT22 1
void setupDHTSensor();
#endif
#define SENSOR_TYPE_HDC1080 2
#ifdef USE_HDC1080
void setupHDCSensor();
#endif
void setupWiFi();
void wifiConnectLoop();
void transmitMeasurements();
void storeMeasurement(float temp, float hum, float dryness, int sensor_type);

//Do not add code below this line
#endif /* _smartpeg_H_ */
