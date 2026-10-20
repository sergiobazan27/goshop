-- Existing development databases created when OPEN was the only order status.
-- Fresh databases already get both values from Hibernate's current enum mapping.
BEGIN;
ALTER TABLE restaurant_order DROP CONSTRAINT IF EXISTS restaurant_order_status_check;
ALTER TABLE restaurant_order ADD CONSTRAINT restaurant_order_status_check
  CHECK (status IN ('OPEN', 'CLOSED'));
COMMIT;
