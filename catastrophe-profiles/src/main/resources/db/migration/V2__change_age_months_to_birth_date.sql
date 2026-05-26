-- V2: Cambiar age_months por birth_date para calcular la edad automáticamente
-- y poder felicitar al gato en su cumpleaños.

ALTER TABLE cats ADD COLUMN birth_date DATE;

-- Migrar datos existentes: convertir age_months aproximado a fecha de nacimiento
UPDATE cats SET birth_date = CURRENT_DATE - (age_months * INTERVAL '1 month')
WHERE age_months IS NOT NULL;

ALTER TABLE cats DROP COLUMN age_months;
