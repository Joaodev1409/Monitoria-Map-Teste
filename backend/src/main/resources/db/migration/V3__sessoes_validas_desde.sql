-- Ao sair (ou ao ser desativado), todo token emitido antes deste instante deixa de valer.
ALTER TABLE usuario ADD COLUMN sessoes_validas_desde TIMESTAMPTZ;
