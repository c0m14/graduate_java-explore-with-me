create table if not exists endpoint_hit (
    hit_id bigint generated always as identity not null,
    app_name varchar not null,
    app_uri varchar not null,
    ip varchar not null,
    timestamp timestamp not null,
    constraint pk_endpoint_hit primary key (hit_id)
);
create index app_name_index on endpoint_hit (app_name);
create index app_uri_index on endpoint_hit (app_uri);
create index timestamp_index on endpoint_hit (timestamp);