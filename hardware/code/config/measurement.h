/*
 * measurement.h
 *
 *  Created on: Dec 9, 2017
 *      Author: frank
 */

#ifndef CONFIG_MEASUREMENT_H_
#define CONFIG_MEASUREMENT_H_

// Peg ID
#define PEG_ID 1

// (De-)Activate Sensors
#define USE_HDC1080
#define USE_DHT22



// Measurement period in ms
#define MEASUREMENT_PERIOD       5000
// Transmission period in ms
#define MEASUREMENT_BULKPERIOD 180000
// Percentage of the buffer that has to be full before transmitting
#define TRANSMIT_PERCENTAGE 85

#ifdef USE_HDC1080
#ifdef USE_DHT22
#define MEASUREMENTS_PER_STEP 2
#else
#define MEASUREMENTS_PER_STEP 1
#endif
#else
#ifdef USE_DHT22
#define MEASUREMENTS_PER_STEP 1
#else
#define MEASUREMENTS_PER_STEP 0
#endif
#endif

#define MEASUREMENT_STORAGE_SIZE (MEASUREMENT_BULKPERIOD/MEASUREMENT_PERIOD)*MEASUREMENTS_PER_STEP

#endif /* CONFIG_MEASUREMENT_H_ */
