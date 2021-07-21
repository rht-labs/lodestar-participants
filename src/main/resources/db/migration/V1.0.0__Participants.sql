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

create index IDXfq09vs7pigm2h4yhq9swh6dnr on Participant (projectId);
create index IDX2hjvqmn8r102s4sembwkfsi1a on Participant (engagementUuid);