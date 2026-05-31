#!/bin/bash
set -e

echo "Starting Database-per-Service provisioning script..."

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE discovery_db;
    CREATE DATABASE booking_db;
    CREATE DATABASE identity_db;
    CREATE DATABASE marketplace_db;
    CREATE DATABASE ticket_db;
EOSQL

echo "All isolated databases successfully provisioned!"