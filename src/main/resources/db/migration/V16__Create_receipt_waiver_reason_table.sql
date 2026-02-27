CREATE TABLE IF NOT EXISTS receipt_waiver_reason (
    code VARCHAR(50) PRIMARY KEY,
    label VARCHAR(200) NOT NULL,
    sort_order INTEGER NOT NULL
);

INSERT INTO receipt_waiver_reason (code, label, sort_order) VALUES
    ('LOST_RECEIPT', 'Mistet kvittering', 1),
    ('NO_RECEIPT_REQUIRED', 'Ingen kvittering krevd', 2),
    ('SMALL_AMOUNT', 'Lite belop', 3),
    ('SUPPLIER_NO_RECEIPT', 'Leverandor uten kvittering', 4),
    ('OTHER', 'Annet', 5);
