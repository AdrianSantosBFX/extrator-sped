package br.sig.sped;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Consultas deterministicas. Filtros sao aplicados apenas aos analiticos C190. */
public final class SpedConsultas {
    private SpedConsultas() {}

    public static final class Filtro {
        public final String cfop, cst, operacao, cfopExato, cstExato;
        public Filtro(String cfop, String cst, String operacao) {
            this.cfop = cfop == null ? "" : cfop.trim();
            this.cst = cst == null ? "" : cst.trim();
            this.operacao = operacao == null ? "Todas" : operacao;
            this.cfopExato = ""; this.cstExato = "";
        }
        public Filtro(String cfop, String cst, String operacao, String cfopExato, String cstExato) {
            this.cfop = cfop == null ? "" : cfop.trim();
            this.cst = cst == null ? "" : cst.trim();
            this.operacao = operacao == null ? "Todas" : operacao;
            this.cfopExato = cfopExato == null ? "" : cfopExato;
            this.cstExato = cstExato == null ? "" : cstExato;
        }
    }
    public static final class Totais {
        public int quantidade, linhasInvalidas;
        public BigDecimal operacoes = BigDecimal.ZERO;
        public BigDecimal baseIcms = BigDecimal.ZERO;
        public BigDecimal icms = BigDecimal.ZERO;
        public BigDecimal baseSt = BigDecimal.ZERO;
        public BigDecimal icmsSt = BigDecimal.ZERO;
        public BigDecimal ipi = BigDecimal.ZERO;
        public void adicionar(SpedParser.Analitico a) {
            quantidade++;
            boolean invalida = a.valorOperacao == null || a.baseIcms == null || a.icms == null ||
                    a.baseSt == null || a.icmsSt == null || a.ipi == null;
            if (invalida) linhasInvalidas++;
            if (a.valorOperacao != null) operacoes = operacoes.add(a.valorOperacao);
            if (a.baseIcms != null) baseIcms = baseIcms.add(a.baseIcms);
            if (a.icms != null) icms = icms.add(a.icms);
            if (a.baseSt != null) baseSt = baseSt.add(a.baseSt);
            if (a.icmsSt != null) icmsSt = icmsSt.add(a.icmsSt);
            if (a.ipi != null) ipi = ipi.add(a.ipi);
        }
    }
    public static final class Grupo {
        public final String operacao, cfop, cst;
        public final Totais totais = new Totais();
        Grupo(String operacao, String cfop, String cst) {
            this.operacao=operacao; this.cfop=cfop; this.cst=cst;
        }
    }
    public static String operacao(SpedParser.Analitico a) {
        return a.documento == null ? "Indefinida" : a.documento.operacao;
    }
    public static boolean corresponde(SpedParser.Analitico a, Filtro filtro) {
        if (a == null) return false;
        return (filtro.cfop.isEmpty() || a.cfop.contains(filtro.cfop)) &&
                (filtro.cst.isEmpty() || a.cst.contains(filtro.cst)) &&
                (filtro.cfopExato.isEmpty() || a.cfop.equals(filtro.cfopExato)) &&
                (filtro.cstExato.isEmpty() || a.cst.equals(filtro.cstExato)) &&
                ("Todas".equals(filtro.operacao) || filtro.operacao.equals(operacao(a)));
    }
    public static List<SpedParser.Analitico> filtrar(List<SpedParser.Analitico> registros, Filtro filtro) {
        List<SpedParser.Analitico> saida = new ArrayList<SpedParser.Analitico>();
        for (SpedParser.Analitico a : registros) if (corresponde(a, filtro)) saida.add(a);
        return saida;
    }
    public static Totais totalizar(List<SpedParser.Analitico> registros) {
        Totais t = new Totais();
        for (SpedParser.Analitico a : registros) t.adicionar(a);
        return t;
    }
    public static List<Grupo> agrupar(List<SpedParser.Analitico> registros) {
        Map<String,Grupo> grupos = new LinkedHashMap<String,Grupo>();
        for (SpedParser.Analitico a : registros) {
            String op=operacao(a), chave=op+'\u001f'+a.cfop+'\u001f'+a.cst;
            Grupo g=grupos.get(chave);
            if (g==null) { g=new Grupo(op,a.cfop,a.cst); grupos.put(chave,g); }
            g.totais.adicionar(a);
        }
        return new ArrayList<Grupo>(grupos.values());
    }
}
