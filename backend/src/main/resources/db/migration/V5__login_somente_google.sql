-- O login passou a ser só com a conta Google da UEPB: senha e código por e-mail deixam de existir.
DROP TABLE codigo_verificacao;
ALTER TABLE usuario DROP COLUMN senha_hash;
ALTER TABLE usuario DROP COLUMN email_verificado;
