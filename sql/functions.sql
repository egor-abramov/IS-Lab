create or replace function count_full_name_greater(p_full_name text) returns bigint as $$
    select count(*) from organization where full_name > p_full_name;
$$ language sql stable;

create or replace function find_name_contains(p_sub text) returns setof organization as $$
    select * from organization where strpos(LOWER(name), LOWER(p_sub)) > 0 order by id;
$$ language sql stable;

create or replace function unique_ratings() returns setof bigint as $$
    select distinct rating from organization where rating is not null order by rating;
$$ language sql stable;

create or replace function top5_by_turnover() returns setof organization as $$
    select * from organization where annual_turnover is not null
    order by annual_turnover desc, id limit 5;
$$ language sql stable;

create or replace function avg_employees_top10() returns double precision as $$
    select coalesce(avg(employees_count), 0)::double precision from (
        select employees_count from organization where annual_turnover is not null
        order by annual_turnover desc, id limit 10
    ) t;
$$ language sql stable;
