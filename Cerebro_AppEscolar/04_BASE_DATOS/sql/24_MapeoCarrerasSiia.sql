-- 24_MapeoCarrerasSiia.sql
-- Vincula el catálogo local de carreras con el id que usa el SIIA real
-- (campo "idCarrera" que devuelve POST /login.ashx), para que un alumno
-- nuevo que entra por primera vez vía SIIA no se quede en "cuenta
-- pendiente" solo porque nadie mapeó su carrera todavía.
--
-- El SIIA no tiene un endpoint de catálogo — el único idCarrera que se
-- conoce de una carrera es el que trae el login de alguien que la cursa.
-- Por eso esto se llena poco a poco: cada vez que un alumno de una carrera
-- sin mapear intenta entrar, el backend registra en el log su idCarrera
-- (ver [AUTH-SIIA][SIIA-OK]) y basta correr un UPDATE como los de ejemplo
-- de abajo para completar el mapeo — no hace falta otra migración.

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.CatalogoCarreras') AND name = N'IdCarreraSiia'
)
BEGIN
    ALTER TABLE dbo.CatalogoCarreras ADD IdCarreraSiia INT NULL;
END
GO

-- Único solo cuando no es NULL — puede haber varias carreras locales sin
-- mapear todavía (mismo patrón que UQ_Alumnos_CorreoInstitucional).
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UQ_CatalogoCarreras_IdCarreraSiia')
    CREATE UNIQUE NONCLUSTERED INDEX UQ_CatalogoCarreras_IdCarreraSiia
        ON dbo.CatalogoCarreras (IdCarreraSiia)
        WHERE IdCarreraSiia IS NOT NULL;
GO

-- Ejemplo de cómo completar un mapeo una vez que se conoce el idCarrera
-- real (viene en el log [AUTH-SIIA][SIIA-OK] la primera vez que alguien de
-- esa carrera intenta entrar):
--
-- UPDATE dbo.CatalogoCarreras SET IdCarreraSiia = 12 WHERE Clave = N'ISC';
