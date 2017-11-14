/*
 * Peg.cpp
 *
 *  Created on: Nov 11, 2017
 *      Author: frank
 */

#include "Peg.h"

Peg::Peg(uint8_t pin):
	_pin(pin) {

}

Peg::~Peg() {

}

float Peg::readDryness() {
	int rawResult = analogRead(_pin);
	return (float)(rawResult * 100)/(float)PEG_MAX_ANALOG_VALUE;
}
