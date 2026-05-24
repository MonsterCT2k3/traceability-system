-- Bổ sung trạng thái PICKED_UP_FROM_SELLER (Hibernate ddl-auto không sửa CHECK cũ trên PostgreSQL).
-- Đặt V2 để vẫn chạy khi bật baseline-on-migrate (baseline mặc định = 1).
DO $$
BEGIN
  IF to_regclass('public.trade_orders') IS NOT NULL THEN
    ALTER TABLE trade_orders DROP CONSTRAINT IF EXISTS trade_orders_status_check;
    ALTER TABLE trade_orders ADD CONSTRAINT trade_orders_status_check CHECK (
      status IN (
        'PENDING',
        'ACCEPTED',
        'REJECTED',
        'CANCELLED',
        'ASSIGNED_TO_CARRIER',
        'PICKED_UP_FROM_SELLER',
        'DELIVERED'
      )
    );
  END IF;
END $$;
