CREATE TABLE public.job_listing_clusters (
	id serial primary key,
	url varchar NULL,
	cluster_type varchar NULL,
	cluster_name varchar NULL
)
WITH (
	OIDS=FALSE
) ;


CREATE TABLE public.job_listings (
	id serial primary key,
	url text null
)
WITH (
	OIDS=FALSE
) ;

CREATE TABLE public.listing_cluster (
	listing_id int4 NULL,
	cluster_id int4 NULL,
	CONSTRAINT listing_cluster_cluster_id_fkey FOREIGN KEY (cluster_id) REFERENCES public.job_listing_clusters(id),
	CONSTRAINT listing_cluster_listing_id_fkey FOREIGN KEY (listing_id) REFERENCES public.job_listings(id)
)
WITH (
	OIDS=FALSE
) ;

CREATE TABLE public.skills (
	id serial primary key,
	title text NULL
)
WITH (
	OIDS=FALSE
) ;


CREATE TABLE public.skills_listing (
	skill_id int4 NULL,
	listing_id int4 NULL,
	CONSTRAINT skills_listing_listing_id_fkey FOREIGN KEY (listing_id) REFERENCES public.job_listings(id),
	CONSTRAINT skills_listing_skill_id_fkey FOREIGN KEY (skill_id) REFERENCES public.skills(id)
)
WITH (
	OIDS=FALSE
) ;

CREATE TABLE public.clusters_pagination (
	id serial primary key,
	url varchar NULL,
	cluster_id int4 NULL,
	is_scraped BOOLEAN NULL,
	CONSTRAINT clusters_pagination_cluster_id_fkey FOREIGN KEY (cluster_id) REFERENCES public.job_listing_clusters(id)
)
WITH (
	OIDS=FALSE
) ;


alter table job_listing_clusters add column is_paginated boolean;

alter table
	listing_cluster add column pagination_id int4 null,
	add constraint listing_cluster_pagination_id_fkey foreign key(pagination_id) references public.clusters_pagination(id);
	
alter table job_listings
add column is_downloaded boolean;
