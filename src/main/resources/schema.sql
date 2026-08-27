CREATE TABLE IF NOT EXISTS technology (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(90)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_technology_name UNIQUE (name)
);
