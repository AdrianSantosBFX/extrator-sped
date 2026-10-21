package br.sig.sped;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

/** Regressao de pesquisa C100/0150, vinculo C190 e comparacao numerica/cronologica. */
public final class DocumentoConsultaTest {
    static void assertTrue(boolean b,String label) {if(!b)throw new AssertionError(label);}
    static int contar(SpedParser.Resultado r,String n,String s,String k,String p,String d,String o) {
        return DocumentoConsulta.filtrar(r.documentos,new DocumentoConsulta.Filtro(n,s,k,p,d,o)).size();
    }
    public static void main(String[] args) throws Exception {
        SpedParser.Resultado r=SpedParser.ler(new File(args[0]));
        assertTrue(r.participantes.size()==2,"cadastros 0150");
        assertTrue(r.documentos.size()==3,"tres documentos C100");
        assertTrue(r.documentos.get(0).serie.equals("1"),"serie C100");
        assertTrue(r.documentos.get(0).participante.nome.contains("ALFA"),"vinculo participante 0150");
        assertTrue(r.documentos.get(1).participante.identificador().equals("99887766000155"),"CNPJ do participante");
        assertTrue(r.documentos.get(0).quantidadeC190==1 && r.documentos.get(1).quantidadeC190==1 && r.documentos.get(2).quantidadeC190==0,"filhos reais por C100");
        assertTrue(contar(r,"12345","","","","","Todas")==1,"numero");
        assertTrue(contar(r,"","1","","","","Todas")==3,"serie");
        assertTrue(contar(r,"","","00000002","","","Todas")==1,"chave parcial");
        assertTrue(contar(r,"","","","FORNECEDOR ALFA","","Todas")==1,"nome participante");
        assertTrue(contar(r,"","","","99887766000155","","Todas")==2,"CNPJ participante");
        assertTrue(contar(r,"","","","99.887.766/0001-55","","Todas")==2,"CNPJ pontuado");
        assertTrue(contar(r,"","","","CLIENTE01","","Todas")==2,"codigo participante");
        assertTrue(contar(r,"","","","","10/09/2026","Todas")==1,"data formatada");
        assertTrue(contar(r,"","","","","10092026","Todas")==1,"data numerica");
        assertTrue(contar(r,"","","","","09/2026","Todas")==3,"mes e ano");
        assertTrue(contar(r,"","","","","","Entrada")==1,"operacao entrada");
        assertTrue(contar(r,"23456","1","00000002","CLIENTE BETA","15/09/2026","Saída")==1,"criterios combinados");
        assertTrue(contar(r,"23456","1","00000002","FORNECEDOR","15/09/2026","Saída")==0,"intersecao dos filtros");
        assertTrue(DocumentoConsulta.datas().compare("02/10/2026","30/09/2026")>0,"ordenacao cronologica");
        assertTrue(DocumentoConsulta.numericos().compare("100","9")>0,"ordenacao numerica de nota");
        assertTrue(DocumentoConsulta.valores().compare("10.000,00","900,00")>0,"ordenacao valor monetario");
        assertTrue(DocumentoConsulta.participante(r.documentos.get(0)).contains("ALFA"),"nome de participante no C100");
        assertTrue(r.analiticos.get(1).documento==r.documentos.get(1),"relacao analitico ao documento");
        System.out.println("PASSOU: 0150, serie, numero, chave, CPF/CNPJ, nome, COD_PART, datas, combinacao, operacao, classificacao e C190 por C100.");
    }
}
