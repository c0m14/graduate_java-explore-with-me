--create types
do $$
begin
    if not exists (select 1 from pg_type where typname = 'event_state') then
        create type event_state as enum ('PENDING', 'PUBLISHED', 'CANCELED');
    end if;
end$$;;
do $$
begin
    if not exists (select 1 from pg_type where typname = 'request_status') then
        create type request_status as enum ('PENDING', 'CONFIRMED', 'REJECTED');
    end if;
end$$;;

--create tables
create table if not exists users (
    user_id bigint generated always as identity not null,
    user_name varchar not null,
    email varchar not null,
    constraint pk_users primary key (user_id),
    constraint user_email_unique unique (email)
);;

create table if not exists category (
    category_id int generated always as identity not null,
    category_name varchar(50) not null,
    constraint pk_category primary key (category_id),
    constraint category_name_unique unique(category_name)
);;

create table if not exists event (
    event_id bigint generated always as identity not null,
    title varchar not null,
    annotation varchar not null,
    description varchar,
    category_id int not null,
    event_date timestamp not null,
    initiator_id bigint not null,
    paid boolean not null,
    latitude numeric not null,
    longitude numeric not null,
    participant_limit int,
    request_moderation boolean,
    created_on timestamp,
    published_on timestamp,
    state event_state,
    constraint pk_event primary key (event_id),
    constraint fk_event_category foreign key (category_id) references category (category_id)
        on delete restrict,
    constraint fk_even_users foreign key (initiator_id) references users (user_id)
        on delete cascade
);;

create table if not exists event_participation_request (
    request_id bigint generated always as identity not null,
    created timestamp,
    event_id bigint,
    requester_id bigint,
    request_status request_status,
    constraint pk_event_participation_request primary key (request_id),
    constraint fk_event_participation_request__event foreign key (event_id) references event (event_id)
        on delete cascade,
    constraint fk_event_participation_request__users foreign key (requester_id) references users (user_id)
        on delete cascade
);;

create table if not exists compilation (
    compilation_id bigint generated always as identity not null,
    pinned boolean not null,
    title varchar not null,
    constraint pk_compilation primary key (compilation_id)
);;

create table if not exists compilations_events (
    compilation_id bigint not null,
    event_id bigint not null,
    constraint pk_compilations_events primary key (compilation_id, event_id),
    constraint fk_compilations_events__compilation foreign key (compilation_id) references compilation (compilation_id)
        on delete cascade,
    constraint fk_compilations_events__event foreign key (event_id) references event (event_id)
        on delete cascade
);;