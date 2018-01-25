# smartpeg
Project "Smarte Wäscheklammer" for Ambient Intelligence at TU Darmstadt, winter term 2017/18

## About the project
This repository contains hardware definitions and software to build a prototype of a _smart peg_ that you can attach to your
drying laundry. It will estimate the remaining time until the laundry is completely dry and notifcy you on your Android phone.

The project consists of the following components:

- The hardware, an actual peg, an ESP8266 microcontroller board, humidity and temperature sensors and a battery. Descriptions on
how to build the hardware are available in the `hardware/schematics` folder.
- The microcontroller software, written in C/C++, can be found at `hardware/code`, more details on how to build it are provided
by the README file in the `hardware` folder.
- The server component, written as Java Web Application (we used Tomcat for testing), can be found in the `api` directory,
together with the database and API description files
- The machine learning scripts are written in Python 3 and use TensorFlow, they are located in the `server` directory
- The Android app, written in Java, is located in the `app` folder.

## What it looks like

The final hardware prototype looked like that. The PCB as well as the battery have roughly the length of the peg, so they can be glued together easily:

![Front view of the final hardware prototype](https://github.com/fhessel/smartpeg/blob/master/hardware/schematics/smartpeg_final1.jpg?raw=true)
![Rear view of the final hardware prototype](https://github.com/fhessel/smartpeg/blob/master/hardware/schematics/smartpeg_final2.jpg?raw=true)

The following image shows a screenshot of one drying process together with the prediction of the neural network:

![Prediction and measurement values](https://github.com/fhessel/smartpeg/blob/master/server/example_trace.png?raw=true)