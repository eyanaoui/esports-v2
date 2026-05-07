@echo off
echo === Running Database Migration V003 ===
echo.
mvn exec:java -Dexec.mainClass=com.esports.db.MigrationV003Standalone
echo.
pause
