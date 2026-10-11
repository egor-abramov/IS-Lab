do $$
declare
    spb  bigint;
    msk  bigint;
    kzn  bigint;
    a1   bigint;
    a2   bigint;
    a3   bigint;
    a4   bigint;
    a5   bigint;
    a6   bigint;
    c    bigint[];
begin
    if exists (select 1 from organization where name = 'ITMO Tech' and full_name = 'ITMO Technologies LLC') then
        return;
    end if;

    insert into location (x, y, name) values (30, 59.94, 'Saint Petersburg') returning id into spb;
    insert into location (x, y, name) values (37, 55.75, 'Moscow') returning id into msk;
    insert into location (x, y, name) values (49, 55.79, 'Kazan') returning id into kzn;

    insert into address (street, town_id) values ('Kronverksky pr. 49', spb) returning id into a1;
    insert into address (street, town_id) values ('Nevsky pr. 28', spb) returning id into a2;
    insert into address (street, town_id) values ('Tverskaya st. 7', msk) returning id into a3;
    insert into address (street, town_id) values ('Leninsky pr. 32', msk) returning id into a4;
    insert into address (street, town_id) values ('Baumana st. 15', kzn) returning id into a5;
    insert into address (street, town_id) values ('PO Box 101', null) returning id into a6;

    with ins as (
        insert into coordinates (x, y) values
            (10.5, 20), (-100, 5), (0, 0), (250.75, -30), (-576.5, 100),
            (42, 42), (1000, -1000), (-12.25, 7), (3.14, 15), (77.7, -77)
        returning id
    )
    select array_agg(id order by id) into c from ins;

    insert into organization (name, coordinates_id, official_address_id, annual_turnover, employees_count,
                              rating, full_name, type, postal_address_id) values
        ('ITMO Tech', c[1], a1, 1500000, 1200, 5, 'ITMO Technologies LLC', 'PRIVATE_LIMITED_COMPANY', a1),
        ('Nevsky Trade', c[2], a2, 830000, 340, 4, 'Nevsky Trade House', 'COMMERCIAL', a2),
        ('Moscow Tech Hub', c[3], a3, 5200000, 2500, 5, 'Moscow Technology Hub OJSC', 'OPEN_JOINT_STOCK_COMPANY', a3),
        ('Kazan Soft', c[4], a5, 410000, 85, 3, 'Kazan Software Development', 'PRIVATE_LIMITED_COMPANY', a5),
        ('Baltic Trust', c[5], a2, 2750000, 150, 4, 'Baltic Investment Trust', 'TRUST', a6),
        ('Green Market', c[6], null, 95000, 25, 2, 'Green Market Retail', 'COMMERCIAL', a4),
        ('Volga Energy', c[7], a5, 9800000, 4300, 5, 'Volga Energy Company OJSC', 'OPEN_JOINT_STOCK_COMPANY', a5),
        ('Small Bakery', c[8], null, null, 6, null, null, null, a6),
        ('North Logistics', c[9], a4, 1230000, 610, 3, 'Northern Logistics Group', 'COMMERCIAL', a4),
        ('Tech Startup', c[10], null, null, 12, 1, 'Tech Startup Incubator', null, a1);
end
$$;
