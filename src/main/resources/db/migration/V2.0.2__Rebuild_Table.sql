DROP TABLE IF EXISTS Participant;

create sequence hibernate_sequence start 1 increment 1;

create table Participant (
    id int8 not null,
    email varchar(255) not null,
    engagementUuid varchar(255) not null,
    firstName varchar(255) not null,
    lastName varchar(255) not null,
    organization varchar(255) not null,
    projectId int8 not null,
    region varchar(255),
    role varchar(255) not null,
    uuid varchar(255) not null,
    primary key (id)
);

alter table if exists Participant
  add constraint UK_participant_uuid unique (uuid);

DROP INDEX IF EXISTS project_id_index;
DROP INDEX IF EXISTS engagement_uuid_index;
DROP INDEX IF EXISTS region_index;

create index project_id_index on Participant (projectId);
create index engagement_uuid_index on Participant (engagementUuid);
create index region_index on Participant (region, organization);
