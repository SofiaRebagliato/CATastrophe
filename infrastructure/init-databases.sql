-- Crear una base de datos independiente por cada microservicio
-- Patrón "database-per-service" de microservicios

CREATE DATABASE catastrophe_profiles;
CREATE DATABASE catastrophe_social;
CREATE DATABASE catastrophe_adventures;
CREATE DATABASE catastrophe_notifications;
CREATE DATABASE catastrophe_analytics;

-- Permisos
GRANT ALL PRIVILEGES ON DATABASE catastrophe_profiles TO catastrophe;
GRANT ALL PRIVILEGES ON DATABASE catastrophe_social TO catastrophe;
GRANT ALL PRIVILEGES ON DATABASE catastrophe_adventures TO catastrophe;
GRANT ALL PRIVILEGES ON DATABASE catastrophe_notifications TO catastrophe;
GRANT ALL PRIVILEGES ON DATABASE catastrophe_analytics TO catastrophe;
