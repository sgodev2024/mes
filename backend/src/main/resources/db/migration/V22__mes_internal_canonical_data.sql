create unique index if not exists mes_import_record_id_tenant_batch_uidx
  on mes.import_record(id, tenant_id, batch_id);

create table mes.internal_dataset_release(
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references platform.tenant(id),
  batch_id uuid not null,
  template_id uuid not null,
  reporting_date date not null,
  domain varchar(40) not null,
  template_code varchar(100) not null,
  contract_version int not null check(contract_version > 0),
  contract_hash char(64) not null check(contract_hash ~ '^[0-9a-f]{64}$'),
  status varchar(20) not null default 'PUBLISHED' check(status = 'PUBLISHED'),
  source_rows int not null check(source_rows >= 0),
  metric_points int not null check(metric_points > 0),
  published_by uuid not null,
  published_at timestamptz not null default now(),
  unique(tenant_id, batch_id),
  unique(id, tenant_id),
  constraint mes_internal_release_batch_tenant_fk foreign key(batch_id, tenant_id)
    references mes.import_batch(id, tenant_id),
  constraint mes_internal_release_template_tenant_fk foreign key(template_id, tenant_id)
    references mes.template_definition(id, tenant_id),
  constraint mes_internal_release_publisher_tenant_fk foreign key(published_by, tenant_id)
    references identity.account(id, tenant_id)
);

create index mes_internal_release_date_idx
  on mes.internal_dataset_release(tenant_id, reporting_date desc, template_code);

create table mes.canonical_product(
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references platform.tenant(id),
  product_code varchar(100) not null check(btrim(product_code) <> ''),
  product_name varchar(240) not null check(btrim(product_name) <> ''),
  unit_code varchar(40) not null check(btrim(unit_code) <> ''),
  resolution_status varchar(20) not null check(resolution_status in ('UNVERIFIED','RESOLVED')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(tenant_id, product_code),
  unique(id, tenant_id)
);

create table mes.canonical_partner(
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references platform.tenant(id),
  partner_type varchar(20) not null check(partner_type in ('SUPPLIER','CUSTOMER')),
  partner_key varchar(180) not null check(btrim(partner_key) <> ''),
  partner_code varchar(100),
  partner_name varchar(240),
  resolution_status varchar(20) not null check(resolution_status in ('UNVERIFIED','RESOLVED')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(tenant_id, partner_type, partner_key),
  unique(id, tenant_id)
);

create table mes.canonical_daily_metric(
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references platform.tenant(id),
  release_id uuid not null,
  batch_id uuid not null,
  source_record_id uuid not null,
  reporting_date date not null,
  domain varchar(40) not null,
  template_code varchar(100) not null,
  metric_code varchar(100) not null check(btrim(metric_code) <> ''),
  metric_label varchar(240) not null,
  metric_value numeric(30,10) not null,
  unit_code varchar(40) not null,
  product_id uuid,
  partner_id uuid,
  quality_status varchar(20) not null check(quality_status in ('VALID','WARNING','UNVERIFIED')),
  source_sheet varchar(120) not null,
  source_row int not null check(source_row > 0),
  contract_version int not null check(contract_version > 0),
  contract_hash char(64) not null check(contract_hash ~ '^[0-9a-f]{64}$'),
  published_at timestamptz not null,
  unique(tenant_id, batch_id, source_record_id, metric_code),
  constraint mes_metric_release_tenant_fk foreign key(release_id, tenant_id)
    references mes.internal_dataset_release(id, tenant_id),
  constraint mes_metric_batch_tenant_fk foreign key(batch_id, tenant_id)
    references mes.import_batch(id, tenant_id),
  constraint mes_metric_source_tenant_batch_fk foreign key(source_record_id, tenant_id, batch_id)
    references mes.import_record(id, tenant_id, batch_id),
  constraint mes_metric_product_tenant_fk foreign key(product_id, tenant_id)
    references mes.canonical_product(id, tenant_id),
  constraint mes_metric_partner_tenant_fk foreign key(partner_id, tenant_id)
    references mes.canonical_partner(id, tenant_id)
);

create index mes_canonical_metric_query_idx
  on mes.canonical_daily_metric(tenant_id, reporting_date desc, domain, metric_code);
create index mes_canonical_metric_product_idx
  on mes.canonical_daily_metric(tenant_id, product_id, reporting_date desc)
  where product_id is not null;
create index mes_canonical_metric_partner_idx
  on mes.canonical_daily_metric(tenant_id, partner_id, reporting_date desc)
  where partner_id is not null;

create or replace function mes.reject_published_dataset_mutation()
returns trigger language plpgsql as $$
begin
  raise exception 'Published MES internal dataset is immutable'
    using errcode = '23514';
end $$;

create trigger mes_internal_release_immutable
before update or delete on mes.internal_dataset_release
for each row execute function mes.reject_published_dataset_mutation();

create trigger mes_canonical_metric_immutable
before update or delete on mes.canonical_daily_metric
for each row execute function mes.reject_published_dataset_mutation();

alter table mes.internal_dataset_release enable row level security;
alter table mes.internal_dataset_release force row level security;
create policy tenant_isolation on mes.internal_dataset_release
  using (tenant_id = platform.current_tenant_id())
  with check (tenant_id = platform.current_tenant_id());

alter table mes.canonical_product enable row level security;
alter table mes.canonical_product force row level security;
create policy tenant_isolation on mes.canonical_product
  using (tenant_id = platform.current_tenant_id())
  with check (tenant_id = platform.current_tenant_id());

alter table mes.canonical_partner enable row level security;
alter table mes.canonical_partner force row level security;
create policy tenant_isolation on mes.canonical_partner
  using (tenant_id = platform.current_tenant_id())
  with check (tenant_id = platform.current_tenant_id());

alter table mes.canonical_daily_metric enable row level security;
alter table mes.canonical_daily_metric force row level security;
create policy tenant_isolation on mes.canonical_daily_metric
  using (tenant_id = platform.current_tenant_id())
  with check (tenant_id = platform.current_tenant_id());

do $$
begin
  if exists(select 1 from pg_roles where rolname='core_app') then
    grant select, insert, update, delete on mes.internal_dataset_release to core_app;
    grant select, insert, update, delete on mes.canonical_product to core_app;
    grant select, insert, update, delete on mes.canonical_partner to core_app;
    grant select, insert, update, delete on mes.canonical_daily_metric to core_app;
  end if;
end $$;
