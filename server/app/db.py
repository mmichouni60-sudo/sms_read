import pyodbc
from .config import config

def connect():
    connection_string, _, _ = config()
    connection = pyodbc.connect(connection_string, autocommit=False, timeout=10)
    connection.timeout = 15
    return connection
