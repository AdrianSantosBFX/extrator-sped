package br.sig.sped;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Demonstrativos descritivos: dados declarados C100, C190 e E110 SEM cruzar totais. */
public final class RelatorioFiscal {
    private RelatorioFiscal() {}
    public static final String AVISO="Valores extraídos do SPED, sem consulta à SEFAZ, sem recálculo de tributos e sem validação fiscal.";
    public static final String RESSALVA="C190 não é apuração; não somar C100, C190 e E110. C100 inclui situações especiais/canceladas. Registros não suportados não entram nos totais.";
    public static final String INVALIDOS="Campos numéricos inválidos não são somados. Quando houver valores inválidos, o total correspondente pode estar incompleto.";
    public static final class Linha {
        public String operacao, cfop, cst;
        public int quantidade, invalidas;
        public BigDecimal valor, base, icms, baseSt, icmsSt, ipi;
    }
    public static final class ResumoOperacao {
        public String operacao;
        public int c100;
        public BigDecimal valorC100=BigDecimal.ZERO;
        public int c100Invalidos;
    }
    public static final class Dados {
        public SpedParser.Resultado sped;
        public String filtro;
        public final List<Linha> grupos=new ArrayList<Linha>();
        public final List<ResumoOperacao> documentos=new ArrayList<ResumoOperacao>();
        public SpedConsultas.Totais totais;
        public int c190Considerados, c190Originais;
        public int situacoesEspeciais;
        public boolean filtroAtivo;
        public boolean temValoresInvalidos() {return totais.linhasInvalidas>0 || contarC100Invalidos()>0;}
        public int contarC100Invalidos() {int n=0;for(ResumoOperacao d:documentos)n+=d.c100Invalidos;return n;}
    }
    public static Dados construir(SpedParser.Resultado r,SpedConsultas.Filtro filtro) {
        if(r==null)throw new IllegalArgumentException("Importe um SPED antes de emitir relatórios.");
        if(filtro==null) filtro=new SpedConsultas.Filtro("","","Todas");
        Dados d=new Dados();d.sped=r;
        d.filtro="Operação: "+filtro.operacao+" | CFOP exato: "+(filtro.cfopExato.isEmpty()?"Todos":filtro.cfopExato)+
             " | CST exato: "+(filtro.cstExato.isEmpty()?"Todos":filtro.cstExato)+
             " | CFOP contém: "+(filtro.cfop.isEmpty()?"—":filtro.cfop)+" | CST contém: "+(filtro.cst.isEmpty()?"—":filtro.cst);
        d.filtroAtivo=!"Todas".equals(filtro.operacao)||!filtro.cfop.isEmpty()||!filtro.cst.isEmpty()||!filtro.cfopExato.isEmpty()||!filtro.cstExato.isEmpty();
        List<SpedParser.Analitico> filtrados=SpedConsultas.filtrar(r.analiticos,filtro);
        d.c190Considerados=filtrados.size();d.c190Originais=r.analiticos.size();
        d.totais=SpedConsultas.totalizar(filtrados);
        for(SpedConsultas.Grupo g:SpedConsultas.agrupar(filtrados)) {
            Linha l=new Linha();l.operacao=g.operacao;l.cfop=g.cfop;l.cst=g.cst;
            l.quantidade=g.totais.quantidade;l.invalidas=g.totais.linhasInvalidas;
            l.valor=g.totais.operacoes;l.base=g.totais.baseIcms;l.icms=g.totais.icms;
            l.baseSt=g.totais.baseSt;l.icmsSt=g.totais.icmsSt;l.ipi=g.totais.ipi;
            d.grupos.add(l);
        }
        Collections.sort(d.grupos,new Comparator<Linha>() {
            public int compare(Linha a,Linha b) {
                int v=a.operacao.compareTo(b.operacao);if(v!=0)return v;
                v=a.cfop.compareTo(b.cfop);return v!=0?v:a.cst.compareTo(b.cst);
            }
        });
        for(String op:new String[]{"Entrada","Saída","Indefinida"}) {
            ResumoOperacao resumo=new ResumoOperacao();resumo.operacao=op;
            for(SpedParser.Doc doc:r.documentos)if(op.equals(doc.operacao)) {
                resumo.c100++;
                if(doc.valor==null)resumo.c100Invalidos++; else resumo.valorC100=resumo.valorC100.add(doc.valor);
                if(!"00".equals(doc.situacao))d.situacoesEspeciais++;
            }
            d.documentos.add(resumo);
        }
        return d;
    }
    public static String br(BigDecimal v) {
        DecimalFormatSymbols symbols=new DecimalFormatSymbols(new Locale("pt","BR"));
        DecimalFormat format=new DecimalFormat("#,##0.00",symbols);return format.format(v);
    }
    public static String data(String s) {
        return s!=null && s.matches("[0-9]{8}")?s.substring(0,2)+"/"+s.substring(2,4)+"/"+s.substring(4):s==null?"":s;
    }
}
