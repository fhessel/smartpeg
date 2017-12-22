-- Table for pegs that are known to the server
CREATE TABLE peg (
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    bat_status INT
);

-- Table for predictions
CREATE TABLE prediction (
    peg_id INT NOT NULL,
    nr INT NOT NULL,
    dry_at TIMESTAMP,
    PRIMARY KEY (peg_id, nr),
    FOREIGN KEY (peg_id) REFERENCES peg(id)
);

-- Table for measurements
CREATE TABLE measurement (
    peg_id INT NOT NULL,
    nr INT NOT NULL,
    sensor_type VARCHAR(16) DEFAULT NULL,
    temperature FLOAT,
    humidity FLOAT,
    conductance FLOAT,
    timestamp TIMESTAMP,
    PRIMARY KEY (peg_id, nr),
    FOREIGN KEY (peg_id) REFERENCES peg(id)
);

-- Table that stores the drying_periods (they are used to create training data)
CREATE TABLE `drying_period` (
  `peg_id` int(11) NOT NULL,
  `period_id` int(11) NOT NULL,
  `ts_start` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `ts_end` timestamp NULL,
  `ts_dry` timestamp NULL,
  PRIMARY KEY (`peg_id`,`period_id`),
  CONSTRAINT `drying_period_fk_pegid` FOREIGN KEY (`peg_id`) REFERENCES `peg` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table that stores the moving average training values
CREATE TABLE `measurement_train` (
  `peg_id` int(11) NOT NULL,
  `nr` int(11) NOT NULL,
  `sensor_type` varchar(16) default null,
  `vec_data_in` text not null,
  `vec_data_out` text not null,
  PRIMARY KEY (`peg_id`,`nr`),
  CONSTRAINT `measurement_train_fk_measurement`
    FOREIGN KEY(`peg_id`,`nr`)
    REFERENCES `measurement` (`peg_id`,`nr`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Index for creating training data
create index idx_measurement_ts on measurement(timestamp);

-- Create first peg
INSERT INTO peg(bat_status) VALUES (0);

