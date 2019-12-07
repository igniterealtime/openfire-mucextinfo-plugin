CREATE TABLE mucextinfo (
    room          NVARCHAR(306)  NOT NULL,
    formtypename  NVARCHAR(255)  NOT NULL,
    varname       NVARCHAR(255)  NULL,
    label         NVARCHAR(255)  NULL,
    varvalue      NVARCHAR(1023) NULL
);

INSERT INTO ofVersion (name, version) VALUES ('mucextinfo', 0);
