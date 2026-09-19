-- Corrige 5 alumnos de prueba (2024PRUEBA1..5) cuyo NombreCompleto quedó con
-- el carácter de reemplazo Unicode (U+FFFD) en vez de la vocal acentuada —
-- se insertaron por fuera de los scripts versionados de este repo (no hay
-- rastro de ellos en database/sql/), probablemente con una herramienta que
-- no respetó UTF-8 al escribir el INSERT (mismo tipo de bug ya documentado
-- en DEC-014). Se comprobó con un INSERT real vía el backend (mssql +
-- sql.NVarChar) que el código actual NO corrompe acentos — no hace falta
-- ningún cambio de código, solo reparar estos 5 registros existentes.
-- Idempotente: solo actualiza si el nombre actual sigue corrupto.

SET QUOTED_IDENTIFIER ON;
GO

UPDATE dbo.Alumnos SET NombreCompleto = N'Ana López Martínez'
  WHERE Matricula = N'2024PRUEBA1' AND NombreCompleto <> N'Ana López Martínez';
UPDATE dbo.Alumnos SET NombreCompleto = N'Carlos Hernández Ruiz'
  WHERE Matricula = N'2024PRUEBA2' AND NombreCompleto <> N'Carlos Hernández Ruiz';
UPDATE dbo.Alumnos SET NombreCompleto = N'María Fernanda Torres'
  WHERE Matricula = N'2024PRUEBA3' AND NombreCompleto <> N'María Fernanda Torres';
UPDATE dbo.Alumnos SET NombreCompleto = N'Jorge Luis Ramírez'
  WHERE Matricula = N'2024PRUEBA4' AND NombreCompleto <> N'Jorge Luis Ramírez';
UPDATE dbo.Alumnos SET NombreCompleto = N'Sofía Jiménez Castro'
  WHERE Matricula = N'2024PRUEBA5' AND NombreCompleto <> N'Sofía Jiménez Castro';
GO
