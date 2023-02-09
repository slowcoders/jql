create schema if not exists starwars;
create schema if not exists starwars_jpa;

create table if not exists starwars.`character`
(
    id       int auto_increment
    primary key,
    height   float        null,
    mass     float        null,
    metadata json         null,
    name     varchar(255) not null,
    species  varchar(255) not null
);


create table if not exists starwars.character_friend_link
(
    character_id int not null,
    friend_id    int not null,
    constraint character_id__friend_id__uindex
        unique (character_id, friend_id),
    constraint fk_character_id_2_pk_character__id
        foreign key (character_id) references starwars.`character` (id),
    constraint fk_friend_id_2_pk_character__id
        foreign key (friend_id) references starwars.`character` (id)
);

create table if not exists starwars.episode
(
    title     varchar(255) not null
        primary key,
    published datetime(6)  null
);

create table if not exists starwars.character_episode_link
(
    episode_id   varchar(255) not null,
    character_id int          not null,
    constraint character_id__episode_id__uindex
        unique (character_id, episode_id),
    constraint fk_episode_id_2_pk_episode__title
        foreign key (episode_id) references starwars.episode (title),
    constraint fk_character_id_2_pk_character__id2
        foreign key (character_id) references starwars.`character` (id)
);

create table if not exists starwars.starship
(
    id       int auto_increment
        primary key,
    length   float        null,
    name     varchar(255) not null,
    pilot_id int          null,
    constraint fk_pilot_id_2_pk_character__id
        foreign key (pilot_id) references starwars.`character` (id)
);

