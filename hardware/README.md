# Hardware

This folder contains a description of the hardware (schematics) as well as the source code
for the ESP8266.

## Schematics

The subfolder contains the circuit design and schematics. The files can be edited with
[Fritzing](http://fritzing.org/home/).

### Parts list

The project currently makes use of:

- An ESP8266 development board, compatible to NodeMCU
  - XSource ESP8266 D1 mini ([Amazon Link](https://www.amazon.de/gp/product/B01ELFAF1S/))
  - Note: It has a slightly different pin layout as the more common Wemos D1 mini!
- A DHT22 temperature and humidity sensor
  - DHT11 should also work, if configured in the code
- A TI HDC1080 temperature and humidity sensor
  - Soldered on a 2x4 pin 0.1" breakout board for better handling
- A 20 mA blue LED
- Some resistors:
  - One 68 ohm resistor (for the LED)
  - One 1 mega ohm resistor (for measuring conductance of the clothes)
  - Two 4.7 kilo ohm resistors (pull-up for communication lines of I²C)
- A 470µF capacitor
- A switch
- A 1N4007 diode
- A peg that has wires on both of the clamp
- An 18650 LiPo battery (capacity should be irrelevant, but the one used had 2500mAh)
  - Charging controller including TP4056 (charging) and DW01x (discharge protection)
  - Holder for the battery

The 1N4007 diode is used to introduce a fixed voltage drop, because the 4.2V present while micro USB for
charging is connected would harm the ESP8266.

### Resources

- [Guide on calculating I²C pullup resistors](http://www.ti.com/lit/an/slva689/slva689.pdf) by Texas Instrucments
- [Data Sheet for HDC1080](http://www.ti.com/lit/ds/symlink/hdc1080.pdf) temperature and humidity sensor
- [Data Sheet for DHT22](https://www.sparkfun.com/datasheets/Sensors/Temperature/DHT22.pdf) temperature and humidity sensor

### Circuit Design

![Circuit on PCB](https://github.com/fhessel/smartpeg/blob/master/hardware/schematics/smartpeg-pcb_bb.png?raw=true)

The PCB layout uses female headers for most components, including sensors, the ESP8266 board and a PCB that contains the components
for control of charging and discharging. It has the dimensions of 10 by 24 and uses the common 0.1 inch distance between holes.

Both of the top-most size 8 headers are used to connect the ESP8266 board, with the micro USB port facing to the upper side of the image.
This means, the (unused) 5V pin is on the top-right corner.

The single size 4 header on the left is used to connect the DHT22, with the Vcc pin at the border of the PCB.

On the bottom right, there are two size four headers to connect the HDC1080, and on the bottom left, the charging controller is placed on the
remaining headers.

The following photo shows the assembled PCB, the battery and the peg:

![Photo of PCB](https://github.com/fhessel/smartpeg/blob/master/hardware/schematics/smartpeg-pcb_foto.jpg?raw=true)

**Note:** Only one sensor is required for the circuit, if you decide to assemble it with only one sensor, the other one can be disabled in the code.

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

