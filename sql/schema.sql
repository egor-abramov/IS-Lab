create table if not exists coordinates (
    id bigserial primary key,
    x  real    not null check (x > -577),
    y  integer not null
);

create table if not exists location (
    id   bigserial primary key,
    x    bigint not null,
    y    real   not null,
    name text   not null
);

create table if not exists address (
    id      bigserial primary key,
    street  text not null,
    town_id bigint references location (id)
);

create table if not exists organization (
    id                  bigserial primary key check (id > 0),
    name                text      not null check (btrim(name) <> ''),
    coordinates_id      bigint    not null references coordinates (id),
    creation_date       timestamp not null default now(),
    official_address_id bigint references address (id),
    annual_turnover     real check (annual_turnover > 0),
    employees_count     integer   not null check (employees_count > 0),
    rating              bigint check (rating > 0),
    full_name           text,
    type                varchar(32) check (type in ('COMMERCIAL', 'TRUST', 'PRIVATE_LIMITED_COMPANY', 'OPEN_JOINT_STOCK_COMPANY')),
    postal_address_id   bigint    not null references address (id)
);
