/*
 * HDC1080.h
 *
 *  Created on: Nov 21, 2017
 *      Author: frank
 */

#ifndef SENSORS_HDC1080_H_
#define SENSORS_HDC1080_H_

#include <Arduino.h>
#include <Wire.h>

class HDC1080 {
public:
	HDC1080(uint8_t sda, uint8_t scl);
	virtual ~HDC1080();

	uint8_t begin();

	uint8_t triggerMeasurement();

	float getTemperature();
	float getHumidity();

private:
	uint8_t  setupConfigurationRegisters();
	uint8_t  tryReadMeasurements();
	uint8_t  _sda, _scl;
	float    _humidity, _temperature;
	unsigned long _lastMeasurement;
};

#endif /* SENSORS_HDC1080_H_ */
