-- Corregir el tipo de user_id en oauth_tokens para que sea BIGINT
-- Este script ejecuta solo si user_id fue creado como VARCHAR por error

DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    -- Verificar si la columna user_id existe y es de tipo character varying
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'oauth_tokens'
        AND column_name = 'user_id'
        AND data_type = 'character varying'
    ) THEN
        -- Obtener el nombre exacto de la restricción de clave foránea
        SELECT constraint_name INTO constraint_name
        FROM information_schema.table_constraints
        WHERE table_name = 'oauth_tokens'
        AND constraint_type = 'FOREIGN KEY'
        AND constraint_name LIKE '%user_id%'
        LIMIT 1;

        -- Si no encuentra una con user_id en el nombre, intenta la primera clave foránea
        IF constraint_name IS NULL THEN
            SELECT constraint_name INTO constraint_name
            FROM information_schema.table_constraints
            WHERE table_name = 'oauth_tokens'
            AND constraint_type = 'FOREIGN KEY'
            LIMIT 1;
        END IF;

        -- Eliminar la restricción de clave foránea si existe
        IF constraint_name IS NOT NULL THEN
            EXECUTE 'ALTER TABLE oauth_tokens DROP CONSTRAINT ' || constraint_name;
        END IF;

        -- Cambiar el tipo de columna con CAST
        ALTER TABLE oauth_tokens ALTER COLUMN user_id TYPE BIGINT USING user_id::BIGINT;

        -- Re-agregar la restricción de clave foránea
        ALTER TABLE oauth_tokens
        ADD CONSTRAINT oauth_tokens_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

        RAISE NOTICE 'Columna user_id en oauth_tokens convertida correctamente a BIGINT';
    ELSE
        RAISE NOTICE 'La columna user_id ya es de tipo BIGINT o no existe. No se requieren cambios.';
    END IF;
END $$;


