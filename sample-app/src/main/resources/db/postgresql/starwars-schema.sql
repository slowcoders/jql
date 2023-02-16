create schema if not exists starwars;
create schema if not exists starwars_jpa;

create table if not exists starwars.character
(
    id bigserial not null
        constraint character_pkey primary key,
    species varchar(31) not null,
    name varchar(255) not null,
    height real,
    mass real,
    metadata jsonb
);
alter table starwars.character owner to jql_demo;

--------------------------------------------------------
create table if not exists starwars.character_friend_link
(
    character_id bigint not null
        constraint fk_character_id_2_pk_character__id
            references starwars.character,
    friend_id bigint not null
        constraint fk_friend_id_2_pk_character__id
            references starwars.character
);
alter table starwars.character_friend_link owner to jql_demo;
create unique index if not exists character_id__friend_id__uindex
    on starwars.character_friend_link (character_id, friend_id);


--------------------------------------------------------
create table if not exists starwars.episode
(
    title varchar(255) not null
        constraint episode_pkey
            primary key,
    published timestamp
);
alter table starwars.episode owner to jql_demo;

--------------------------------------------------------
create table if not exists starwars.character_episode_link
(
    character_id bigint not null
        constraint fk_character_id_2_pk_character__id
            references starwars.character,
    episode_id varchar(255) not null
        constraint fk_episode_id_2_pk_episode__title
            references starwars.episode
);
alter table starwars.character_episode_link owner to jql_demo;
create unique index if not exists character_id__episode_id__uindex
    on starwars.character_episode_link (character_id, episode_id);


--------------------------------------------------------
create table if not exists starwars.starship
(
    id bigserial not null
        constraint starship_pkey
            primary key,
    pilot_id bigint
        constraint fk_pilot_id_2_pk_character__id
            references starwars.character,
    length real,
    name varchar(255) not null
);
alter table starwars.starship owner to jql_demo;

--------------------------------------------------------

