#Revenus par client (avec montants facturés et payés)
CREATE OR REPLACE VIEW chart1 AS
SELECT 
    c.external_id,
    c.id AS client_id,
    c.company_name,
    COUNT(DISTINCT i.id) AS total_invoices,
    SUM(il.price * il.quantity) AS total_invoiced_amount,
    SUM(p.amount) AS total_paid_amount,
    SUM(il.price * il.quantity) - SUM(COALESCE(p.amount, 0)) AS outstanding_amount
FROM 
    clients c
    LEFT JOIN invoices i ON c.id = i.client_id
    LEFT JOIN invoice_lines il ON i.id = il.invoice_id
    LEFT JOIN payments p  ON i.id = p.invoice_id
WHERE 
    c.deleted_at IS NULL AND
    i.deleted_at IS NULL
GROUP BY 
    c.id, c.company_name
ORDER BY 
    total_invoiced_amount DESC;



CREATE OR REPLACE VIEW chart2 AS
SELECT 
    DATE_FORMAT(p.payment_date, '%Y-%m') AS payment_month,
    COUNT(DISTINCT p.id) AS total_payments,
    COUNT(DISTINCT i.id) AS total_invoices,
    SUM(p.amount) AS total_paid_amount,
    SUM(il.price * il.quantity) AS total_invoiced_amount,
    SUM(il.price * il.quantity) - SUM(COALESCE(p.amount, 0)) AS outstanding_amount
FROM 
    payments p
    LEFT JOIN invoices i ON p.invoice_id = i.id
    LEFT JOIN invoice_lines il ON i.id = il.invoice_id
WHERE 
    p.deleted_at IS NULL 
    AND i.deleted_at IS NULL
GROUP BY 
    DATE_FORMAT(p.payment_date, '%Y-%m')
ORDER BY 
    payment_month DESC;



CREATE OR REPLACE VIEW chart3 AS
SELECT 
    i.status AS invoice_status,
    COUNT(DISTINCT i.id) AS total_invoices,
    COUNT(DISTINCT p.id) AS total_payments,
    SUM(p.amount) AS total_paid_amount,
    SUM(il.price * il.quantity) AS total_invoiced_amount,
    SUM(il.price * il.quantity) - SUM(COALESCE(p.amount, 0)) AS outstanding_amount
FROM 
    invoices i
    LEFT JOIN invoice_lines il ON i.id = il.invoice_id
    LEFT JOIN payments p ON i.id = p.invoice_id
WHERE 
    i.deleted_at IS NULL
GROUP BY 
    i.status
ORDER BY 
    total_invoiced_amount DESC;