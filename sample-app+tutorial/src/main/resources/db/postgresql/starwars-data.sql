INSERT INTO starwars.episode (title, published) values
('NEWHOPE', '2022-12-10T00:00:00'), ('EMPIRE', '2021-12-10T00:00:00'), ('JEDI', '2020-12-10T00:00:00')
on conflict DO NOTHING;


INSERT INTO starwars.character (species, id, name, height, mass, metadata) values
('Human', 1000, 'Luke Skywalker', 1.72, 77, '{ "homePlanet": "Tatooine", "memo": { "favoriteFood": "kimchi", "shoeSize": 260 } }'),
('Human', 1001, 'Darth Vader', 2.02, 136, '{ "homePlanet": "Tatooine", "memo": { "favoriteFood": "pork", "shoeSize": 370 } }'),
('Human', 1002, 'Han Solo', 1.8, 80, '{ "memo": { "favoriteFood": "apple", "shoeSize": 270 } }'),
('Human', 1003, 'Leia Organa', 1.5, 49, '{ "homePlanet": "Alderaan" }'),
('Human', 1004, 'Wilhuff Tarkin', 1.8, null, '{ "memo": { "favoriteFood": "fish", "shoeSize": 350 } }'),
('Human', 1005, 'Extra-1', 1.19, 50, null),
('Human', 1006, 'Extra-2', 1.8, 121, null),
('Droid', 2000, 'C-3PO', 1.71, 75, '{ "primaryFunction": "protocol" }'),
('Droid', 2001, 'R2-D2', 1.09, 32, '{ "primaryFunction": "Astromech" }')
on conflict DO NOTHING;


INSERT INTO starwars.starship (id, name, length, pilot_id) values
(3000, 'Millenium Falcon', 34.37, 1002),
(3001, 'X-Wing', 12.5, 1000),
(3002, 'TIE Advanced x1', 9.2, 1001),
(3003, 'Imperial shuttle', 20, NULL)
on conflict DO NOTHING;


INSERT INTO starwars.character_episode_link (character_id, episode_id) values
(1000, 'NEWHOPE'),
(1000, 'EMPIRE'),
(1000, 'JEDI'),
(1001, 'NEWHOPE'),
(1001, 'EMPIRE'),
(1001, 'JEDI'),
(1002, 'NEWHOPE'),
(1002, 'EMPIRE'),
(1002, 'JEDI'),
(1003, 'NEWHOPE'),
(1003, 'EMPIRE'),
(1003, 'JEDI'),
(1004, 'NEWHOPE'),
(2000, 'NEWHOPE'),
(2000, 'EMPIRE'),
(2000, 'JEDI'),
(2001, 'NEWHOPE'),
(2001, 'EMPIRE'),
(2001, 'JEDI')
on conflict DO NOTHING;


INSERT INTO starwars.character_friend_link (character_id, friend_id)
values
(1000, 1002),
(1000, 1003),
(1000, 2000),
(1000, 2001),

(1001, 1004),

(1002, 1000),
(1002, 1003),
(1002, 2001),

(1003, 1000),
(1003, 1002),
(1003, 2000),
(1003, 2001),

(1004, 1001),

(2000, 1000),
(2000, 1002),
(2000, 1003),
(2000, 2001),

(2001, 1000),
(2001, 1002),
(2001, 1003)
on conflict DO NOTHING;
