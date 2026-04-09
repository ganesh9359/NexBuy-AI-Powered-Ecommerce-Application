-- ============= Test Data for Return and Refund Flow =============

-- Insert test return requests (after orders are created)
INSERT INTO return_requests (order_id, status, refund_status, reason, requested_at) 
SELECT o.id, 'requested', 'not_started', 'Product not as expected', NOW()
FROM orders o 
WHERE o.order_number = 'NBY-20260409-RETURN1'
LIMIT 1;

-- Insert test refunds for demonstrated return flow
INSERT INTO refunds (order_id, payment_id, amount_cents, currency, status, note, requested_at)
SELECT o.id, p.id, p.amount_cents, p.currency, 'processing', 'Admin approved return', NOW() - INTERVAL 1 HOUR
FROM orders o
JOIN payments p ON o.id = p.order_id
WHERE o.order_number = 'NBY-20260409-RETURN2'
LIMIT 1;

-- Update the corresponding return request to show approved status
UPDATE return_requests
SET status = 'approved', 
    refund_status = 'processing',
    reviewed_at = NOW() - INTERVAL 1 HOUR
WHERE order_id IN (
  SELECT o.id FROM orders o WHERE o.order_number = 'NBY-20260409-RETURN2'
)
LIMIT 1;
