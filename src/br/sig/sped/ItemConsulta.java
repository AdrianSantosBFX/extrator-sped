package br.sig.sped;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Consulta descritiva sobre C170 e cadastro 0200. Sem inferencia de itens ausentes. */
public final class ItemConsulta {
    private ItemConsulta() {}
    public static final class Filtro {
        public final String texto,ncm,cfop,cst,operacao;
        public Filtro(String texto,String ncm,String cfop,String cst,String operacao) {
            this.texto=limpo(texto);this.ncm=limpo(ncm);this.cfop=limpo(cfop);this.cst=limpo(cst);
            this.operacao=operacao==null?"Todas":operacao;
        }
    }
    public static final class Totais {
        public int registros,semCadastro,camposInvalidos,semDocumento,semCamposPrincipais;
        public BigDecimal valor=BigDecimal.ZERO,desconto=BigDecimal.ZERO,icms=BigDecimal.ZERO,icmsSt=BigDecimal.ZERO,ipi=BigDecimal.ZERO;
        private boolean erro(SpedParser.Item item,String nome,BigDecimal valor) {
            return valor==null && !item.ausentes.contains(nome);
        }
        void adicionar(SpedParser.Item i){
            registros++;
            if(i.produto==null)semCadastro++;
            if(i.documento==null)semDocumento++;
            if (erro(i,"VL_ITEM",i.valor)||erro(i,"VL_DESC",i.desconto)||erro(i,"VL_ICMS",i.icms)||
                erro(i,"VL_ICMS_ST",i.icmsSt)||erro(i,"VL_IPI",i.ipi)||erro(i,"QTD",i.quantidade))camposInvalidos++;
            if (i.ausentes.contains("QTD")||i.ausentes.contains("VL_ITEM"))semCamposPrincipais++;
            if(i.valor!=null)valor=valor.add(i.valor);
            if(i.desconto!=null)desconto=desconto.add(i.desconto);
            if(i.icms!=null)icms=icms.add(i.icms);
            if(i.icmsSt!=null)icmsSt=icmsSt.add(i.icmsSt);
            if(i.ipi!=null)ipi=ipi.add(i.ipi);
        }
    }
    private static String limpo(String s){return s==null?"":s.trim();}
    private static String normal(String s){return Normalizer.normalize(limpo(s),Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT);}
    public static boolean corresponde(SpedParser.Item i,Filtro f){
        if(i==null||f==null)return false;
        return (f.texto.isEmpty() || normal(i.codigo+" "+i.descricao()+" "+i.complemento).contains(normal(f.texto))) &&
               (f.ncm.isEmpty()||i.ncm().contains(f.ncm)) &&
               (f.cfop.isEmpty()||i.cfop.contains(f.cfop)) &&
               (f.cst.isEmpty()||i.cst.contains(f.cst)) &&
               ("Todas".equals(f.operacao)||i.operacao().equals(f.operacao));
    }
    public static boolean corresponde(SpedParser.Produto p,String consulta,String ncm){
        return p!=null && (limpo(consulta).isEmpty()||normal(p.codigo+" "+p.descricao).contains(normal(consulta))) &&
                (limpo(ncm).isEmpty()||p.ncm.contains(limpo(ncm)));
    }
    public static List<SpedParser.Item> filtrar(List<SpedParser.Item> itens,Filtro f){
        List<SpedParser.Item> result=new ArrayList<SpedParser.Item>();
        for(SpedParser.Item i:itens) if(corresponde(i,f))result.add(i);
        return result;
    }
    public static Totais totalizar(List<SpedParser.Item> itens){
        Totais t=new Totais();for(SpedParser.Item i:itens)t.adicionar(i);return t;
    }
    /** Ausencia de C170 nao significa, isoladamente, irregularidade. */
    public static int documentosSemItens(SpedParser.Resultado r){
        int total=0;
        for(SpedParser.Doc d:r.documentos)if(d.quantidadeC170==0)total++;
        return total;
    }
}
