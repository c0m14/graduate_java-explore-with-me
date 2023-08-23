create table if not exists users (
    user_id bigint generated always as identity not null,
    user_name varchar not null,
    email varchar not null,
    constraint pk_users primary key (user_id),
    constraint user_email_unique unique (email)
);

create table if not exists category (
    category_id int generated always as identity not null,
    category_name varchar(50) not null,
    constraint pk_category primary key (category_id),
    constraint category_name_unique unique(category_name)
);

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
    state varchar,
    constraint pk_event primary key (event_id),
    constraint fk_event_category foreign key (category_id) references category (category_id)
        on delete restrict,
    constraint fk_even_users foreign key (initiator_id) references users (user_id)
        on delete cascade
);
create index if not exists event_date_index on event (event_date);
create index if not exists event_participant_limit_index on event (participant_limit);
create index if not exists event_state_index on event (state);

create table if not exists event_participation_request (
    request_id bigint generated always as identity not null,
    created timestamp,
    event_id bigint,
    requester_id bigint,
    request_status varchar,
    constraint pk_event_participation_request primary key (request_id),
    constraint fk_event_participation_request__event foreign key (event_id) references event (event_id)
        on delete cascade,
    constraint fk_event_participation_request__users foreign key (requester_id) references users (user_id)
        on delete cascade,
    constraint event_user_unique unique(event_id, requester_id)
);
create index if not exists request_status_index on event_participation_request (request_status);

create table if not exists compilation (
    compilation_id bigint generated always as identity not null,
    pinned boolean not null,
    title varchar not null,
    constraint pk_compilation primary key (compilation_id),
    constraint compilation_title_unique unique (title)
);

create table if not exists compilations_events (
    compilation_id bigint not null,
    event_id bigint not null,
    constraint pk_compilations_events primary key (compilation_id, event_id),
    constraint fk_compilations_events__compilation foreign key (compilation_id) references compilation (compilation_id)
        on delete cascade,
    constraint fk_compilations_events__event foreign key (event_id) references event (event_id)
        on delete cascade
);

create table if not exists user_event_rate (
    user_id bigint not null,
    event_id bigint not null,
    rate int not null,
    constraint fk_rate_users foreign key (user_id) references users (user_id)
    on delete cascade,
    constraint fk_rate_event foreign key (event_id) references event (event_id)
    on delete cascade,
    constraint user_event_combination_unique unique (user_id, event_id)
)