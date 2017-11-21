CREATE TABLE peg (
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    bat_status INT
);

CREATE TABLE prediction (
    peg_id INT NOT NULL,
    nr INT NOT NULL,
    dry_at TIMESTAMP,
    PRIMARY KEY (peg_id, nr),
    FOREIGN KEY (peg_id) REFERENCES peg(id)
);

CREATE TABLE measurement (
    peg_id INT NOT NULL,
    nr INT NOT NULL,
    temperature FLOAT,
    humidity FLOAT,
    conductance INT,
    timestamp TIMESTAMP,
    PRIMARY KEY (peg_id, nr),
    FOREIGN KEY (peg_id) REFERENCES peg(id)
);

-- some dummy data

INSERT INTO peg(bat_status) VALUES (1);
INSERT INTO peg(bat_status) VALUES (2);

INSERT INTO measurement(peg_id, nr, temperature, humidity, conductance, timestamp) VALUES (1, 2, 21.4, 45.7, 36, NOW());
INSERT INTO measurement(peg_id, nr, temperature, humidity, conductance, timestamp) VALUES (1, 1, 23.4, 50.3, 13, NOW());

