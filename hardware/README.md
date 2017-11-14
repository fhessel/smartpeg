# Hardware

This folder contains a description of the hardware (schematics) as well as the source code
for the ESP8266.

## Schematics

The subfolder contains the circuit design and schematics. The files can be edited with
[Fritzing](http://fritzing.org/home/).

### Parts list

The project currently makes use of:

- An ESP8266 development board, compatible to NodeMCU
- A DHT22 temperature and humidity sensor
- A 20 mA blue LED
- A 47 ohm resistor
- A 1 mega ohm resistor
- Some wires
- A peg

## Code

The code subfolder contains the project that generates the program running on the ESP8266
microcontroller.

### Setup

While the ESP8266 is Arduino-compatible, for bigger projects a full IDE is easier to use.
For that reason, the project is developed using the Eclipse-based [Sloeber IDE](http://eclipse.baeyens.it/).

The code folder is an Eclipse project folder that can be imported into a normal workspace.

To setup the development environment:

- Download [Sloeber](http://eclipse.baeyens.it/)
- Get the [ESP8266 Arduino Core](https://github.com/esp8266/Arduino) and follow the instructions to install it.
- Checkout the project into a new workspace
- Copy all foo.example.h files in the /config folder to get an individual configuration (instructions included in the files)
- Compile the project and upload it to the board
