-- =====================================================================
-- Sexta-Feira — Migração manual da Etapa 3 (isolamento multiusuário)
-- Banco: MySQL 8.x   |   NÃO é executado automaticamente.
--
-- Por que existe:
--   * Com spring.jpa.hibernate.ddl-auto=update, o Hibernate ADICIONA a coluna
--     usuario_id, mas NÃO remove as unique constraints antigas (sem usuario_id).
--     Resultado: dois usuários com o mesmo ticker/categoria ainda colidiriam.
--   * Linhas antigas ficam com usuario_id = NULL e passam a ser invisíveis
--     (todas as consultas agora filtram por usuário).
--
-- Como usar (passo a passo, revisando cada resultado):
--   0. FAÇA BACKUP:  mysqldump -u <user> -p sexta_feira_db > backup_pre_etapa3.sql
--   1. Suba a API uma vez com a versão nova (ddl-auto=update cria usuario_id e as novas UKs).
--   2. Rode os blocos abaixo manualmente, na ordem.
-- =====================================================================

-- 1) Garante o usuário dono dos dados legados (ajuste o identificador).
--    Use o MESMO valor de sexta.auth.default-whatsapp (padrão: local-dev-user)
--    ou o número de WhatsApp real do dono, se preferir.
SET @dono_whatsapp = 'local-dev-user';

INSERT INTO usuarios (whatsapp_number, personalidade, criado_em)
SELECT @dono_whatsapp, 'SEXTA_FEIRA', NOW()
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE whatsapp_number = @dono_whatsapp);

SET @dono_id = (SELECT id FROM usuarios WHERE whatsapp_number = @dono_whatsapp);
SELECT @dono_id AS dono_id;  -- confira antes de continuar

-- 2) Backfill: atribui as linhas legadas ao dono.
UPDATE transacoes          SET usuario_id = @dono_id WHERE usuario_id IS NULL;
UPDATE orcamentos          SET usuario_id = @dono_id WHERE usuario_id IS NULL;
UPDATE alertas_orcamento   SET usuario_id = @dono_id WHERE usuario_id IS NULL;
UPDATE ativos_carteira     SET usuario_id = @dono_id WHERE usuario_id IS NULL;
UPDATE movimentos_carteira SET usuario_id = @dono_id WHERE usuario_id IS NULL;
UPDATE snapshots_carteira  SET usuario_id = @dono_id WHERE usuario_id IS NULL;

-- 3) Liste as unique constraints atuais. As ANTIGAS são as que NÃO contêm usuario_id.
SELECT tc.TABLE_NAME, tc.CONSTRAINT_NAME,
       GROUP_CONCAT(kcu.COLUMN_NAME ORDER BY kcu.ORDINAL_POSITION) AS colunas
FROM information_schema.TABLE_CONSTRAINTS tc
JOIN information_schema.KEY_COLUMN_USAGE kcu
  ON kcu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
 AND kcu.TABLE_SCHEMA = tc.TABLE_SCHEMA
 AND kcu.TABLE_NAME = tc.TABLE_NAME
WHERE tc.TABLE_SCHEMA = DATABASE()
  AND tc.CONSTRAINT_TYPE = 'UNIQUE'
  AND tc.TABLE_NAME IN ('orcamentos','alertas_orcamento','ativos_carteira','snapshots_carteira')
GROUP BY tc.TABLE_NAME, tc.CONSTRAINT_NAME;

-- 4) Para cada constraint ANTIGA (sem usuario_id) listada acima, remova-a:
--    ALTER TABLE <tabela> DROP INDEX <nome_da_constraint_antiga>;
--    Exemplo:
--    ALTER TABLE ativos_carteira DROP INDEX UKxxxxxxxxxxxxxxxxxxxxxxxx;

-- 5) (Opcional, recomendado após validar o backfill) torne usuario_id obrigatório:
-- ALTER TABLE transacoes          MODIFY usuario_id BIGINT NOT NULL;
-- ALTER TABLE orcamentos          MODIFY usuario_id BIGINT NOT NULL;
-- ALTER TABLE alertas_orcamento   MODIFY usuario_id BIGINT NOT NULL;
-- ALTER TABLE ativos_carteira     MODIFY usuario_id BIGINT NOT NULL;
-- ALTER TABLE movimentos_carteira MODIFY usuario_id BIGINT NOT NULL;
-- ALTER TABLE snapshots_carteira  MODIFY usuario_id BIGINT NOT NULL;

-- 6) Verificação final: deve retornar 0 em todas as linhas.
SELECT 'transacoes' t, COUNT(*) FROM transacoes WHERE usuario_id IS NULL
UNION ALL SELECT 'orcamentos', COUNT(*) FROM orcamentos WHERE usuario_id IS NULL
UNION ALL SELECT 'alertas_orcamento', COUNT(*) FROM alertas_orcamento WHERE usuario_id IS NULL
UNION ALL SELECT 'ativos_carteira', COUNT(*) FROM ativos_carteira WHERE usuario_id IS NULL
UNION ALL SELECT 'movimentos_carteira', COUNT(*) FROM movimentos_carteira WHERE usuario_id IS NULL
UNION ALL SELECT 'snapshots_carteira', COUNT(*) FROM snapshots_carteira WHERE usuario_id IS NULL;
