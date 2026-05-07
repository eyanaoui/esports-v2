import mysql.connector


DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "",
    "database": "esports_db",
    "port": 3306
}


def get_connection():
    return mysql.connector.connect(**DB_CONFIG)