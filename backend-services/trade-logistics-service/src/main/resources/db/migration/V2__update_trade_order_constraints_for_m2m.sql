DO $$
BEGIN
    IF to_regclass('public.trade_orders') IS NOT NULL THEN
        ALTER TABLE trade_orders
            DROP CONSTRAINT IF EXISTS trade_orders_order_type_check;

        ALTER TABLE trade_orders
            ADD CONSTRAINT trade_orders_order_type_check
            CHECK (order_type IN (
                'MANUFACTURER_TO_SUPPLIER',
                'MANUFACTURER_TO_MANUFACTURER',
                'RETAILER_TO_MANUFACTURER'
            ));

        ALTER TABLE trade_orders
            DROP CONSTRAINT IF EXISTS trade_orders_status_check;

        ALTER TABLE trade_orders
            ADD CONSTRAINT trade_orders_status_check
            CHECK (status IN (
                'PENDING',
                'PROCESSING',
                'ACCEPTED',
                'ERROR',
                'REJECTED',
                'CANCELLED',
                'ASSIGNED_TO_CARRIER',
                'PICKED_UP_FROM_SELLER',
                'DELIVERED'
            ));
    END IF;
END
$$;
