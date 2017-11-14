/*
 * Peg.h
 *
 *  Created on: Nov 11, 2017
 *      Author: frank
 */

#ifndef SENSORS_PEG_H_
#define SENSORS_PEG_H_

#include "Arduino.h"

#define PEG_MAX_ANALOG_VALUE 1023

class Peg {
public:
	Peg(uint8_t pin);
	virtual ~Peg();
	float readDryness();
private:
	uint8_t _pin;
};

#endif /* SENSORS_PEG_H_ */
