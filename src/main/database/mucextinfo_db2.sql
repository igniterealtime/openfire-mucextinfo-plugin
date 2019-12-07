CREATE TABLE mucextinfo (
    room          VARCHAR(306)  NOT NULL,
    formtypename  VARCHAR(255)  NOT NULL,
    varname       VARCHAR(255)  NULL,
    label         VARCHAR(255)  NULL,
    varvalue      VARCHAR(1023) NULL
);

INSERT INTO ofVersion (name, version) VALUES ('mucextinfo', 0);
