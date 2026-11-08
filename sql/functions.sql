drop function if exists count_full_name_greater(text);
drop function if exists find_name_contains(text);
drop function if exists unique_ratings();
drop function if exists top5_by_turnover();
drop function if exists avg_employees_top10();

create or replace function count_full_name_greater(p_full_name text, p_owner bigint) returns bigint as $$
    select count(*) from organization where full_name > p_full_name and (p_owner is null or owner_id = p_owner);
$$ language sql stable;

create or replace function find_name_contains(p_sub text, p_owner bigint) returns setof organization as $$
    select * from organization where strpos(LOWER(name), LOWER(p_sub)) > 0 and (p_owner is null or owner_id = p_owner) order by id;
$$ language sql stable;

create or replace function unique_ratings(p_owner bigint) returns setof bigint as $$
    select distinct rating from organization where rating is not null and (p_owner is null or owner_id = p_owner) order by rating;
$$ language sql stable;

create or replace function top5_by_turnover(p_owner bigint) returns setof organization as $$
    select * from organization where annual_turnover is not null and (p_owner is null or owner_id = p_owner)
    order by annual_turnover desc, id limit 5;
$$ language sql stable;

create or replace function avg_employees_top10(p_owner bigint) returns double precision as $$
    select coalesce(avg(employees_count), 0)::double precision from (
        select employees_count from organization where annual_turnover is not null and (p_owner is null or owner_id = p_owner)
        order by annual_turnover desc, id limit 10
    ) t;
$$ language sql stable;
