drop table PERFECTO_CLOUDS;

create table PERFECTO_CLOUDS
(
        NAME VARCHAR(50) NOT NULL,
        USER_NAME VARCHAR(100) NOT NULL,
        PASSWORD VARCHAR(100) NOT NULL,
        HOST_NAME VARCHAR(100) NOT NULL,
        PROXY_HOST VARCHAR(100),
        PROXY_PORT VARCHAR(10),
        DESCRIPTION VARCHAR(200),
        GRID_INSTANCE VARCHAR(200)
);
