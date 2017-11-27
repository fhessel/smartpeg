// Only modify this file to include
// - function definitions (prototypes)
// - include files
// - extern variable definitions
// In the appropriate section

#ifndef _smartpeg_H_
#define _smartpeg_H_
#include "Arduino.h"

// WiFi
#include <ESP8266WiFi.h>

// Temperature and humidity sensors
#include "sensors/HDC1080.h"
#include <DHT.h>

// Peg sensor
#include "sensors/Peg.h"

// WiFi Settings
#include "config/wificonfig.h"

// Pin configuration
#include "config/pins.h"

void setupSerialMonitor();
void setupDHTSensor();
void setupHDCSensor();
void setupWiFi();

//Do not add code below this line
#endif /* _smartpeg_H_ */
