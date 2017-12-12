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


void setupSerialMonitor();
#ifdef USE_DHT22
void setupDHTSensor();
#endif
#ifdef USE_HDC1080
void setupHDCSensor();
#endif
void setupWiFi();
void wifiConnectLoop();

//Do not add code below this line
#endif /* _smartpeg_H_ */
