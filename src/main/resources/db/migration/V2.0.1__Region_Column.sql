ALTER TABLE Participant
ADD region varchar(255);

create index region_index on Participant (region, organization);
