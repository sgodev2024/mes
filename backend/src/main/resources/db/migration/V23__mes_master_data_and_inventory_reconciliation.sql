alter table mes.canonical_product
  add column version int not null default 1 check(version > 0),
  add column resolved_by uuid,
  add column resolved_at timestamptz,
  add constraint mes_product_resolver_tenant_fk foreign key(resolved_by, tenant_id)
    references identity.account(id, tenant_id),
  add constraint mes_product_resolution_consistent check(
    (resolution_status = 'RESOLVED' and resolved_by is not null and resolved_at is not null)
    or (resolution_status = 'UNVERIFIED' and resolved_by is null and resolved_at is null)
  );

alter table mes.canonical_partner
  add column version int not null default 1 check(version > 0),
  add column resolved_by uuid,
  add column resolved_at timestamptz,
  add constraint mes_partner_resolver_tenant_fk foreign key(resolved_by, tenant_id)
    references identity.account(id, tenant_id),
  add constraint mes_partner_resolution_consistent check(
    (resolution_status = 'RESOLVED' and resolved_by is not null and resolved_at is not null)
    or (resolution_status = 'UNVERIFIED' and resolved_by is null and resolved_at is null)
  );

create index mes_product_resolution_idx
  on mes.canonical_product(tenant_id, resolution_status, product_code);
create index mes_partner_resolution_idx
  on mes.canonical_partner(tenant_id, resolution_status, partner_type, partner_key);

insert into identity.policy(tenant_id, code, resource_type, action, effect, condition_json)
select tenant.id, policy.code, 'MES_MASTER_DATA', policy.action, 'ALLOW', '{}'::jsonb
from platform.tenant tenant
cross join (values
  ('mes-master-data-read', 'READ'),
  ('mes-master-data-approve', 'APPROVE')
) as policy(code, action)
on conflict(tenant_id, code, version) do nothing;

insert into identity.role_policy(tenant_id, role_id, policy_id)
select role.tenant_id, role.id, policy.id
from identity.role role
join identity.policy policy on policy.tenant_id = role.tenant_id
where (role.code in ('mes-report-preparer','mes-report-approver','mes-report-viewer')
       and policy.code = 'mes-master-data-read')
   or (role.code = 'mes-report-approver' and policy.code = 'mes-master-data-approve')
on conflict do nothing;

update identity.permission_revision set revision = revision + 1, updated_at = now();
