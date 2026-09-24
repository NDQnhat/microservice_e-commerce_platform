-- Database per service initialization script
-- Rule: Isolated logical database per service to enforce zero cross-service SQL, joins, or foreign keys.

CREATE DATABASE identity_access_db;
CREATE DATABASE catalog_db;
CREATE DATABASE pricing_db;
CREATE DATABASE cart_db;
CREATE DATABASE inventory_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
CREATE DATABASE fulfillment_db;
CREATE DATABASE notification_db;
CREATE DATABASE audit_compliance_db;
CREATE DATABASE business_configuration_db;
CREATE DATABASE exception_management_db;

GRANT ALL PRIVILEGES ON DATABASE identity_access_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE catalog_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE pricing_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE cart_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE inventory_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE order_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE payment_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE fulfillment_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE audit_compliance_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE business_configuration_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE exception_management_db TO postgres;
