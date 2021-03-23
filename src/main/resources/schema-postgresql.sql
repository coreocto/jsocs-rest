CREATE TABLE public.taccounts
(
    cusername text COLLATE pg_catalog."default",
    cpassword text COLLATE pg_catalog."default",
    ccrtdt timestamp without time zone,
    cid integer NOT NULL DEFAULT nextval('taccounts_cid_seq'::regclass),
    cinit integer,
    ctoken text COLLATE pg_catalog."default",
    cauth_token text COLLATE pg_catalog."default",
    ctype text COLLATE pg_catalog."default",
    ccrtoken text COLLATE pg_catalog."default",
    cauthtoken character varying(255) COLLATE pg_catalog."default",
    cactive integer
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

CREATE TABLE public.tblock
(
    cid integer NOT NULL DEFAULT nextval('tfiletable_cid_seq'::regclass),
    cname text COLLATE pg_catalog."default",
    cuse integer,
    csize integer,
    cremoteid text COLLATE pg_catalog."default",
    cowner text COLLATE pg_catalog."default",
    cdirectlink text COLLATE pg_catalog."default",
    caccid integer,
    CONSTRAINT tfiletable_pkey PRIMARY KEY (cid)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

CREATE TABLE public.tfiles
(
    cid integer NOT NULL DEFAULT nextval('tfile_cid_seq'::regclass),
    cname text COLLATE pg_catalog."default",
    ccrtdt timestamp without time zone,
    csize bigint,
    cisdir integer,
    cparent integer,
    CONSTRAINT tfile_pkey PRIMARY KEY (cid)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

CREATE TABLE public.tfiletable
(
    cid integer NOT NULL DEFAULT nextval('tfiletable_cid_seq1'::regclass),
    cfileid integer,
    cblkid integer
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;