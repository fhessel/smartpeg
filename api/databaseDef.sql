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