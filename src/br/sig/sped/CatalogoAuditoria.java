package br.sig.sped;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Cadastro declarativo de ferramentas: distingue execução real de funcionalidades futuras. */
public final class CatalogoAuditoria {
    private CatalogoAuditoria() { }
    public enum Estado { DISPONIVEL, PLANEJADA }
    public static final class Ferramenta {
        public final String id, titulo, descricao, abrangencia, aviso, pagina, topicoAjuda, icone;
        public final Estado estado;
        public final boolean exigeArquivo;
        private Ferramenta(String id,String titulo,String descricao,String abrangencia,String aviso,
                           String pagina,String topicoAjuda,String icone,Estado estado,boolean exigeArquivo) {
            this.id=id;this.titulo=titulo;this.descricao=descricao;this.abrangencia=abrangencia;
            this.aviso=aviso;this.pagina=pagina;this.topicoAjuda=topicoAjuda;
            this.icone=icone;this.estado=estado;this.exigeArquivo=exigeArquivo;
        }
        public boolean executavel(boolean carregado) {
            return estado==Estado.DISPONIVEL && (!exigeArquivo || carregado);
        }
    }
    private static final List<Ferramenta> FERRAMENTAS;
    static {
        List<Ferramenta> itens=new ArrayList<Ferramenta>();
        itens.add(new Ferramenta("RN07","Créditos de uso e consumo",
            "Localiza créditos em CFOP 1.556, 2.556 e 3.556 e oferece correção assistida.",
            "C170 · C190 · C100 · E110", "Exige comprovação de uso/consumo e consistência da apuração.",
            "audit-credit","creditos","alert",Estado.DISPONIVEL,true));
        itens.add(new Ferramenta("DIAG01","Bases, CST e classificação 0200",
            "Examina bases sem crédito, alíquotas residuais e possível classificação inconsistente de produtos.",
            "C170 · C190 · 0200", "Diagnóstico informativo; não altera o arquivo.",
            "audit-diagnostic","diagnostico","analysis",Estado.DISPONIVEL,true));
        itens.add(new Ferramenta("LEIT01","Ocorrências de leitura",
            "Consulta problemas de estrutura ou campos encontrados na importação do SPED.",
            "Importação · registros", "Não representa validação oficial do PVA.",
            "issues","ocorrencias","info",Estado.DISPONIVEL,true));
        itens.add(new Ferramenta("ARQ01","Cópia fiel de EFD",
            "Salva uma cópia byte a byte em .SPED ou .TXT, preservando a finalidade original ou substituta.",
            "Arquivo · COD_FIN", "Cópia não corrige nem valida a escrituração.",
            "copy","arquivos","file",Estado.DISPONIVEL,true));
        itens.add(new Ferramenta("ST01","Créditos e substituição tributária",
            "Futura verificação de créditos em operações sujeitas à substituição tributária.",
            "ST · crédito", "Sem detecção ou correção nesta versão.",
            "","futuras","calculator",Estado.PLANEJADA,true));
        itens.add(new Ferramenta("CIAP01","Ativo imobilizado e CIAP",
            "Futura análise de crédito de ativo imobilizado e apropriação parcelada.",
            "CIAP · apuração", "Sem detecção ou correção nesta versão.",
            "","futuras","database",Estado.PLANEJADA,true));
        FERRAMENTAS=Collections.unmodifiableList(itens);
    }
    public static List<Ferramenta> todas(){return FERRAMENTAS;}
    public static Ferramenta porId(String id){
        for(Ferramenta ferramenta:FERRAMENTAS)if(ferramenta.id.equals(id))return ferramenta;
        return null;
    }
}
