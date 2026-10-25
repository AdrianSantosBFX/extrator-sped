package br.sig.sped;

import java.math.BigDecimal;
import java.util.*;

/** Diagnóstico não mutável. Alertas são pontos de revisão, não conclusões jurídicas. */
public final class PendenciasTributarias {
    private PendenciasTributarias() { }
    private static final Set<String> USO=new HashSet<String>(Arrays.asList("1556","2556","3556","1407","2407"));
    public static final class Resumo {
        public final int basesSemCredito;
        public final BigDecimal valorBases;
        public final int produtosClassificacaoDuvidosa;
        public final int c170AliquotaSemCredito;
        public final String relatorio;
        Resumo(int bases, BigDecimal base, int produtos,int aliq,String rel){
            basesSemCredito=bases;valorBases=base;produtosClassificacaoDuvidosa=produtos;
            c170AliquotaSemCredito=aliq;relatorio=rel;
        }
    }
    public static Resumo analisar(SpedParser.Resultado r){
        if(r==null)return new Resumo(0,BigDecimal.ZERO,0,0,"Importe um arquivo SPED.");
        StringBuilder texto=new StringBuilder();
        texto.append("DIAGNÓSTICO COMPLEMENTAR DE EFD – ").append(r.nome).append("\n");
        texto.append("Período ").append(r.periodoInicio).append(" a ").append(r.periodoFim);
        texto.append("   COD_FIN=").append(r.codFin).append("\n\n");
        int bases=0;BigDecimal valor=BigDecimal.ZERO;
        texto.append("1) BASE DE ICMS POSITIVA SEM VALOR DE CRÉDITO NO C190\n");
        for(SpedParser.Analitico a:r.analiticos){
            if(a.documento==null||!"Entrada".equals(a.documento.operacao)
                ||a.baseIcms==null||a.icms==null||a.baseIcms.signum()<=0||a.icms.signum()!=0)continue;
            bases++;valor=valor.add(a.baseIcms);
            texto.append("  NF ").append(a.documento.numero).append("  C190 L").append(a.linha)
                 .append("  CFOP ").append(a.cfop).append(" CST ").append(a.cst)
                 .append("  BC=R$ ").append(a.baseIcms).append("\n");
        }
        if(bases==0)texto.append("  Nenhuma ocorrência.\n");
        texto.append("TOTAL: ").append(bases).append(" C190; R$ ").append(valor).append(" de bases.\n")
             .append("ATENÇÃO: base sem crédito é sinal para CONFERÊNCIA, não prova de erro.\n\n");
        int aliq=0;
        texto.append("2) C170 DE USO/CONSUMO COM ALÍQUOTA POSITIVA, MAS ICMS ZERO\n");
        for(SpedParser.Item it:r.itens){
            if(!USO.contains(it.cfop)||it.documento==null||!"Entrada".equals(it.documento.operacao)
                ||it.icms==null||it.icms.signum()!=0)continue;
            BigDecimal taxa=BigDecimal.ZERO;
            try{taxa=new BigDecimal(it.aliquota.replace(',','.'));}catch(RuntimeException ignore){}
            if(taxa.signum()<=0)continue;
            aliq++;
            texto.append("  NF ").append(it.documento.numero).append("  C170 L").append(it.linha)
                 .append("  CFOP ").append(it.cfop).append(" CST ").append(it.cst)
                 .append("  ALIQ ").append(it.aliquota).append("\n");
        }
        if(aliq==0)texto.append("  Nenhuma ocorrência.\n");
        texto.append("TOTAL: ").append(aliq).append(" itens.\n\n");
        Map<String,Set<String>> usoPorProduto=new HashMap<String,Set<String>>();
        for(SpedParser.Item it:r.itens){
            if(it.codigo==null||it.codigo.isEmpty())continue;
            Set<String> cfops=usoPorProduto.get(it.codigo);
            if(cfops==null){cfops=new HashSet<String>();usoPorProduto.put(it.codigo,cfops);}
            cfops.add(it.cfop);
        }
        int suspeitos=0;
        texto.append("3) PRODUTOS 0200 COM TIPO_ITEM=00 MAS USADOS EXCLUSIVAMENTE COMO USO/CONSUMO\n");
        for(SpedParser.Produto p:r.produtos.values()){
            Set<String> cfops=usoPorProduto.get(p.codigo);
            if(cfops==null||cfops.isEmpty()||!"00".equals(p.tipo))continue;
            if(!USO.containsAll(cfops))continue;
            suspeitos++;
            texto.append("  0200 L").append(p.linha).append("  ").append(p.codigo).append("  ")
                 .append(p.descricao).append(" (TIPO_ITEM 00; avaliar 07)\n");
        }
        if(suspeitos==0)texto.append("  Nenhuma ocorrência.\n");
        texto.append("TOTAL: ").append(suspeitos).append(" produtos a confirmar.\n")
            .append("O cadastro 0200 NÃO será alterado automaticamente: classificação depende da finalidade efetiva,\n")
            .append("e o mesmo produto pode ter usos diferentes fora do arquivo inspecionado.\n\n")
            .append("O DIAGNÓSTICO NÃO SUBSTITUI O PVA OU UMA REVISÃO TRIBUTÁRIA.\n");
        return new Resumo(bases,valor,suspeitos,aliq,texto.toString());
    }
}
