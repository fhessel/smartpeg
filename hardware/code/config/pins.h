/*
 * pins.h
 *
 *  Created on: Nov 14, 2017
 *      Author: frank
 */

#ifndef CONFIG_PINS_H_
#define CONFIG_PINS_H_

// Required for access to pin constants
#include "Arduino.h"
// Required to define sensor type
#include <DHT.h>


// Temperature and humidity sensor
#define PIN_SENSOR      D5
#define PIN_SENSOR_TYPE DHT22

// Status LED for WiFi connection
#define PIN_LED_WIFI    D2

// Peg sensor
#define PIN_PEG         A0

#endif /* CONFIG_PINS_H_ */
