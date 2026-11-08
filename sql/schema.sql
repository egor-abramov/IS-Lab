create table if not exists app_user (
    id bigserial primary key,
    login varchar(64) not null unique check (login ~ '^[a-zA-Z0-9_]{3,64}$'),
    password_hash text not null,
    role varchar(16) not null default 'USER' check (role in ('USER', 'ADMIN'))
);

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

alter table organization add column if not exists source varchar(32)
    check (source in ('USER', 'FILE'));

create table if not exists import_operation (
    id bigserial primary key,
    user_id bigint not null references app_user (id),
    status varchar(16) not null check (status in ('IN_PROGRESS', 'SUCCESS', 'FAILED')),
    added_count integer check (added_count >= 0),
    check ((status = 'SUCCESS' and added_count is not null)
        or (status <> 'SUCCESS' and added_count is null))
);

create index if not exists import_operation_user_id_idx on import_operation (user_id, id desc);

alter table coordinates add column if not exists owner_id bigint references app_user (id);
alter table location add column if not exists owner_id bigint references app_user (id);
alter table address add column if not exists owner_id bigint references app_user (id);
alter table organization add column if not exists owner_id bigint references app_user (id);

create index if not exists coordinates_owner_idx on coordinates (owner_id);
create index if not exists location_owner_idx on location (owner_id);
create index if not exists address_owner_idx on address (owner_id);
create index if not exists organization_owner_idx on organization (owner_id);
