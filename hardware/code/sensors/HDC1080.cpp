/*
 * HDC1080.cpp
 *
 *  Created on: Nov 21, 2017
 *      Author: frank
 */

#include "HDC1080.h"

#define HDC_DEVICE_ADDRESS        0b1000000 // see manual
#define HDC_CLOCK_FREQ            100000    // 100 kHz

// Measure temperature and humidity together
#define HDC_CONF_MEASURE_TEMPHUM  (0x01 << 12)

// Measure temperature with 14 bit precision
#define HDC_CONF_TEMPRES_14BIT    (0x00 << 10)
// Measure temperature with 11 bit precision
#define HDC_CONF_TEMPRES_11BIT    (0x01 << 10)

// Measure humidity with 14 bit precision
#define HDC_CONF_HUMRES_14BIT     (0x00 << 9)
// Measure humidity with 11 bit precision
#define HDC_CONF_HUMRES_11BIT     (0x01 << 9)
// Measure humidity with 8 bit precision
#define HDC_CONF_HUMRES_8BIT      (0x02 << 9)

// Activate heater during measurement
#define HDC_CONF_ACTIVATE_HEATER  (0x01 << 13)

// Address pointer value to trigger measurements
#define HDC_ADDR_TRIGGER_MEASURE  0x00

// Register to read temperature from
#define HDC_ADDR_TEMPERATURE      0x00
// Register to read humidity from
#define HDC_ADDR_HUMIDITY         0x01

// Address of the configuraton register
#define HDC_ADDR_CONFIGURATION    0x02

// Duration of a measurement in ms
#define HDC_MEASUREMENT_DURATION 1000

HDC1080::HDC1080(uint8_t sda, uint8_t scl):
	_sda(sda),
	_scl(scl) {
	_lastMeasurement = 0;
	_temperature = 0.0f;
	_humidity = 0.0f;
}

HDC1080::~HDC1080() {

}

uint8_t HDC1080::begin() {
	// Configure the I²C bus
	Wire.begin(_sda, _scl);
	Wire.setClock(HDC_CLOCK_FREQ); // 100kHz (HDC supports 10-400)

	// The sensor has a device startup time of 10-15ms
	// We wait for that (could be done better but will work for the beginning)
	while(millis() < 15) {
		delay(1);
	}

	// Configure the sensor by the configuration registers
	return setupConfigurationRegisters();
}

uint8_t HDC1080::setupConfigurationRegisters() {

	uint8_t  pointerRegister = HDC_ADDR_CONFIGURATION;
	uint16_t registerValue   =
			HDC_CONF_MEASURE_TEMPHUM |
			HDC_CONF_ACTIVATE_HEATER |
			HDC_CONF_TEMPRES_14BIT |
			HDC_CONF_HUMRES_14BIT;

	Wire.beginTransmission(HDC_DEVICE_ADDRESS);

		Wire.write(pointerRegister);
		Wire.write((uint8_t)(registerValue >> 8));
		Wire.write((uint8_t)(registerValue));

	uint8_t err = Wire.endTransmission();
	Serial.println(err, HEX);
	return err;
}

/**
 * Triggers a measurement on the sensor.
 *
 * This will require some time before the values can actually be called
 */
uint8_t HDC1080::triggerMeasurement() {
	unsigned long ms = millis();

	// Only start a new measurement if there is not one ongoing
	if (_lastMeasurement < (millis() - HDC_MEASUREMENT_DURATION)) {

		// Trigger measurement
		Wire.beginTransmission(HDC_DEVICE_ADDRESS);
			Wire.write(HDC_ADDR_TRIGGER_MEASURE);
		uint8_t err = Wire.endTransmission();

		if (!err) {
			// Set the time that this measurement has been triggered
			_lastMeasurement = ms;
		}

		return err;
	} else {
		// No transmission, no error, return 0
		return 0;
	}
}

/**
 * Gets the latest value for humidity.
 *
 * If a measurement has been triggered some time ago, this method might
 * first fetch the humidity
 */
float HDC1080::getHumidity() {
	// Check whether there is a newer measurement
	tryReadMeasurements();

	// return the newest measurement
	return _humidity;
}

/**
 * Gets the latest value for humidity.
 *
 * If a measurement has been triggered some time ago, this method might
 * first fetch the temperature
 */
float HDC1080::getTemperature() {
	// Check whether there is a newer measurement
	tryReadMeasurements();

	// return the newest measurement
	return _temperature;
}

/**
 * Reads a measurement register from the sensor
 */
uint8_t HDC1080::tryReadMeasurements() {
	if (_lastMeasurement > 0 && _lastMeasurement < millis() - HDC_MEASUREMENT_DURATION) {

		// Request values (4 Bytes: temp msb, temp lsb, hum msb, hum lsb)
		uint8_t bytesRead = Wire.requestFrom(HDC_DEVICE_ADDRESS, 4);

		// Only continue if we received an answer (and the sensor was ready)
		if (bytesRead == 4) {

			uint16_t temp_raw = (Wire.read() << 8) + Wire.read();
			uint16_t hum_raw  = (Wire.read() << 8) + Wire.read();

			// See data sheet page 14
			_temperature = (((float)temp_raw)*165.0f)/(65536.0f)-40.0f;
			_humidity = ((float)hum_raw)/(655.36f);

			// Mark this value as read
			_lastMeasurement = 0;
		}

	}

	// Nothing to read, no error
	return 0;
}
