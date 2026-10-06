-- V2: Business Configuration Service Seed Data

INSERT INTO business_configurations (id, config_key, config_value, description, version, is_active, effective_from, created_by, created_at, updated_at) VALUES
  ('bc000000-0000-0000-0000-000000000001',
   'free_shipping_threshold',
   '500000',
   'Minimum order subtotal (VND) to qualify for free shipping (BR-019)',
   1, TRUE, '2026-01-01T00:00:00Z', 'system', NOW(), NOW()),

  ('bc000000-0000-0000-0000-000000000002',
   'standard_shipping_fee',
   '30000',
   'Standard flat-rate shipping fee (VND) applied when order is below free shipping threshold',
   1, TRUE, '2026-01-01T00:00:00Z', 'system', NOW(), NOW()),

  ('bc000000-0000-0000-0000-000000000003',
   'reservation_timeout_minutes',
   '15',
   'Duration (minutes) that inventory is reserved after order creation before automatic release (BR-015)',
   1, TRUE, '2026-01-01T00:00:00Z', 'system', NOW(), NOW());
