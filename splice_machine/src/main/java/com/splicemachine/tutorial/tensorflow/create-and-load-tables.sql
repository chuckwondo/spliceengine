create schema CENSUS;

create table CENSUS.ADULT_TRAIN_DATA (
	AGE INT,
	WORKCLASS VARCHAR(20),
	FNLWGT INT,
	EDUCATION VARCHAR(20),
	EDUCATION_NUM INT,
	MARITAL_STATUS VARCHAR(20),
	OCCUPATION VARCHAR(20),
	RELATIONSHIP VARCHAR(20),
	RACE VARCHAR(20),
	GENDER VARCHAR(20),
	CAPITAL_GAIN INT,
	CAPITAL_LOSS INT,
	HOURS_PER_WEEK INT,
	NATIVE_COUNTRY VARCHAR(20),
	INCOME_BRACKET VARCHAR(20),
	LABEL VARCHAR(20)
);

create table CENSUS.ADULT_TEST_DATA (
	AGE INT,
	WORKCLASS VARCHAR(20),
	FNLWGT INT,
	EDUCATION VARCHAR(20),
	EDUCATION_NUM INT,
	MARITAL_STATUS VARCHAR(20),
	OCCUPATION VARCHAR(20),
	RELATIONSHIP VARCHAR(20),
	RACE VARCHAR(20),
	GENDER VARCHAR(20),
	CAPITAL_GAIN INT,
	CAPITAL_LOSS INT,
	HOURS_PER_WEEK INT,
	NATIVE_COUNTRY VARCHAR(20),
	INCOME_BRACKET VARCHAR(20),
	LABEL VARCHAR(20)
);

create table INPUT_DICTIONARY (
	ID int generated by default as identity (START WITH 2, INCREMENT BY 1),
	DICTIONARY_NAME varchar(50),
	COLUMN_NAME varchar(50),
	TYPE varchar(50),
	SEQUENCE int,
	GROUPING int,
	LABEL varchar(50),
	GROUPING_DETAILS varchar(50),
	GROUPING_DETAILS_TYPE varchar(50)
);

insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','AGE','CONTINUOUS',1,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','WORKCLASS','CATEGORICAL',1,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','EDUCATION','CATEGORICAL',2,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','EDUCATION_NUM','CONTINUOUS',2,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','MARITAL_STATUS','CATEGORICAL',3,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','OCCUPATION','CATEGORICAL',4,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','RELATIONSHIP','CATEGORICAL',5,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','RACE','CATEGORICAL',6,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','GENDER','CATEGORICAL',7,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','CAPITAL_GAIN','CONTINUOUS',3,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','CAPITAL_LOSS','CONTINUOUS',4,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','HOURS_PER_WEEK','CONTINUOUS',5,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','NATIVE_COUNTRY','CATEGORICAL',8,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','AGE','COLUMN',1,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','WORKCLASS','COLUMN',2,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','FNLWGT','COLUMN',3,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','EDUCATION','COLUMN',4,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','EDUCATION_NUM','COLUMN',5,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','MARITAL_STATUS','COLUMN',6,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','OCCUPATION','COLUMN',7,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','RELATIONSHIP','COLUMN',8,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','RACE','COLUMN',9,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','GENDER','COLUMN',10,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','CAPITAL_GAIN','COLUMN',11,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','CAPITAL_LOSS','COLUMN',12,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','HOURS_PER_WEEK','COLUMN',13,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','NATIVE_COUNTRY','COLUMN',14,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','INCOME_BRACKET','COLUMN',15,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','LABEL','LABEL',1,null,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','EDUCATION','CROSSED',1,1,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','OCCUPATION','CROSSED',2,1,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','AGE_BUCKETS','CROSSED',1,2,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','EDUCATION','CROSSED',2,2,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','OCCUPATION','CROSSED',3,2,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','NATIVE_COUNTRY','CROSSED',1,3,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS) values ('CENSUS','OCCUPATION','CROSSED',2,3,null,null);
insert into INPUT_DICTIONARY (DICTIONARY_NAME,COLUMN_NAME, TYPE, SEQUENCE, GROUPING, LABEL, GROUPING_DETAILS, GROUPING_DETAILS_TYPE) values ('CENSUS','AGE','BUCKET',null,null,'AGE_BUCKETS','18, 25, 30, 35, 40, 45,50, 55, 60, 65', 'INTEGER');

CALL SYSCS_UTIL.IMPORT_DATA( 'CENSUS','ADULT_TRAIN_DATA','AGE,WORKCLASS,FNLWGT,EDUCATION,EDUCATION_NUM,MARITAL_STATUS,OCCUPATION,RELATIONSHIP,RACE,GENDER,CAPITAL_GAIN,CAPITAL_LOSS,HOURS_PER_WEEK,NATIVE_COUNTRY,INCOME_BRACKET','/Users/admin/dev/tensor/Census/src/main/resources/data/adult.data.txt',null,null,null,null,null,-1,null, true, null);
CALL SYSCS_UTIL.IMPORT_DATA( 'CENSUS','ADULT_TEST_DATA','AGE,WORKCLASS,FNLWGT,EDUCATION,EDUCATION_NUM,MARITAL_STATUS,OCCUPATION,RELATIONSHIP,RACE,GENDER,CAPITAL_GAIN,CAPITAL_LOSS,HOURS_PER_WEEK,NATIVE_COUNTRY,INCOME_BRACKET','/Users/admin/dev/tensor/Census/src/main/resources/data/adult.test.txt',null,null,null,null,null,-1,null, true, null);
