create schema if not exists starwars;
create schema if not exists starwars_jpa;
create schema if not exists jql_demo;

GRANT ALL PRIVILEGES ON jql_demo.* TO 'jql_demo'@'%';
GRANT ALL PRIVILEGES ON starwars.* TO 'jql_demo'@'%';
GRANT ALL PRIVILEGES ON starwars_jpa.* TO 'jql_demo'@'%';
