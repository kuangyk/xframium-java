drop table PERFECTO_SITES;
drop table PERFECTO_PAGES;
drop table PERFECTO_ELEMENTS;

create table PERFECTO_SITES
(
        NAME VARCHAR(50) NOT NULL
);

create table PERFECTO_PAGES
(
        SITE_NAME VARCHAR(50) NOT NULL,
        NAME VARCHAR(50) NOT NULL
);

create table PERFECTO_ELEMENTS
(
        SITE_NAME VARCHAR(50) NOT NULL,
        PAGE_NAME VARCHAR(50) NOT NULL,
        NAME VARCHAR(50) NOT NULL,
        DESCRIPTOR VARCHAR(50) NOT NULL,
        VALUE VARCHAR(200) NOT NULL,
        CONTEXT_NAME VARCHAR(50)
);

