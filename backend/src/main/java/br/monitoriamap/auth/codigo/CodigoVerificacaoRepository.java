package br.monitoriamap.auth.codigo;

import br.monitoriamap.auth.codigo.CodigoVerificacao.Finalidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodigoVerificacaoRepository extends JpaRepository<CodigoVerificacao, Long> {

    Optional<CodigoVerificacao> findFirstByUsuarioIdAndFinalidadeOrderByCriadoEmDesc(Long usuarioId, Finalidade finalidade);

    List<CodigoVerificacao> findByUsuarioIdAndFinalidadeAndUsadoFalse(Long usuarioId, Finalidade finalidade);
}
