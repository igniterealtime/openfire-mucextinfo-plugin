CREATE TABLE mucextinfo (
    room          VARCHAR2(306)  NOT NULL,
    formtypename  VARCHAR2(255)  NOT NULL,
    varname       VARCHAR2(255)  NULL,
    label         VARCHAR2(255)  NULL,
    varvalue      VARCHAR2(1023) NULL
);

INSERT INTO ofVersion (name, version) VALUES ('mucextinfo', 0);
