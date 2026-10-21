package br.sig.sped;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Pesquisa de documentos declarados no C100, sem julgamento da situacao fiscal. */
public final class DocumentoConsulta {
    private DocumentoConsulta() {}

    public static final class Filtro {
        public final String numero, serie, chave, participante, emissao, operacao;
        public Filtro(String numero, String serie, String chave, String participante, String emissao, String operacao) {
            this.numero=limpo(numero);this.serie=limpo(serie);this.chave=limpo(chave);
            this.participante=limpo(participante);this.emissao=limpo(emissao);
            this.operacao=operacao==null?"Todas":operacao;
        }
    }
    private static String limpo(String value) {return value==null?"":value.trim();}
    private static String normalizar(String text) {
        String n=Normalizer.normalize(limpo(text),Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT);
    }
    public static String participante(SpedParser.Doc doc) {
        if (doc.participante!=null && !doc.participante.nome.isEmpty()) return doc.participante.nome;
        return doc.participanteCodigo.isEmpty()?"Não informado":doc.participanteCodigo;
    }
    public static String identificador(SpedParser.Doc doc) {
        return doc.participante==null?"":doc.participante.identificador();
    }
    public static boolean corresponde(SpedParser.Doc doc,Filtro f) {
        if (doc==null || f==null) return false;
        String p=participante(doc)+" "+doc.participanteCodigo+" "+identificador(doc);
        boolean matchId=normalizar(p).contains(normalizar(f.participante));
        if (!matchId && !f.participante.isEmpty() && f.participante.matches("[0-9 ./-]+")) {
            String digits=f.participante.replaceAll("\\D", "");
            matchId=!digits.isEmpty() && identificador(doc).contains(digits);
        }
        String date=textoData(doc.data);
        String emissionQuery=f.emissao.replaceAll("\\s", "");
        return doc.numero.contains(f.numero) && doc.serie.toLowerCase(Locale.ROOT).contains(f.serie.toLowerCase(Locale.ROOT)) &&
            doc.chave.contains(f.chave.replaceAll("\\s", "")) && matchId &&
            (emissionQuery.isEmpty() || date.contains(emissionQuery) || doc.data.contains(emissionQuery.replaceAll("\\D", ""))) &&
            ("Todas".equals(f.operacao) || f.operacao.equals(doc.operacao));
    }
    public static List<SpedParser.Doc> filtrar(List<SpedParser.Doc> docs,Filtro filtro) {
        List<SpedParser.Doc> out=new ArrayList<SpedParser.Doc>();
        for(SpedParser.Doc d:docs)if(corresponde(d,filtro))out.add(d);
        return out;
    }
    public static String textoData(String dt) {
        return dt!=null && dt.matches("[0-9]{8}") ? dt.substring(0,2)+"/"+dt.substring(2,4)+"/"+dt.substring(4):limpo(dt);
    }
    public static String chaveData(String dt) {
        if(dt==null)return "";
        String s=dt.replace("/","");
        return s.matches("[0-9]{8}") ? s.substring(4)+s.substring(2,4)+s.substring(0,2):"";
    }
    public static Comparator<String> datas() {
        return (a,b)->chaveData(a).compareTo(chaveData(b));
    }
    public static Comparator<String> numericos() {
        return (a,b)->{
            String as=a==null?"":a,bs=b==null?"":b;
            if(as.matches("[0-9]+")&&bs.matches("[0-9]+"))return new BigInteger(as).compareTo(new BigInteger(bs));
            return as.compareToIgnoreCase(bs);
        };
    }
    public static Comparator<String> valores() {
        return (a,b)->{
            BigDecimal av=valor(a),bv=valor(b);
            if(av==null)return bv==null?0:1;
            if(bv==null)return -1;
            return av.compareTo(bv);
        };
    }
    private static BigDecimal valor(String v) {
        if(v==null || !v.matches("-?[0-9.]+,[0-9]+"))return null;
        try{return new BigDecimal(v.replace(".","").replace(',','.'));}
        catch(NumberFormatException ex){return null;}
    }
}
