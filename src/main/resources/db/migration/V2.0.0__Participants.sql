DROP TABLE IF EXISTS Participant;

create table Participant (
    uuid varchar(255) not null,
    email varchar(255) not null,
    engagementUuid varchar(255) not null,
    firstName varchar(255) not null,
    lastName varchar(255) not null,
    organization varchar(255) not null,
    projectId int8 not null,
    role varchar(255) not null,
    primary key (uuid)
);

create index project_id_index on Participant (projectId);
create index engagement_uuid_index on Participant (engagementUuid);