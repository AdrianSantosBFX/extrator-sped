package br.sig.sped;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Regra MG-RN07 (escopo piloto): créditos destacados no SPED, CFOP 1556/2556/3556.
 *  A suspeita NÃO constitui conclusão sobre a destinação da mercadoria.
 *  A correção é restrita ao arquivo EFD (.txt/.sped); NUNCA altera XML ou sistema de origem.
 */
public final class CreditoUsoConsumo {
    private CreditoUsoConsumo() { }
    private static final Set<String> CFOPS = new HashSet<String>(Arrays.asList("1556", "2556", "3556"));
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    public static final class Caso {
        public final int linhaC190;
        public final String nota, chave, cfop, cst, aliquota, participante;
        public final BigDecimal credito, base;
        public final int itens;
        public final boolean elegivel;
        public final String motivo;
        Caso(SpedParser.Analitico a, List<SpedParser.Item> itens, boolean elegivel, String motivo) {
            linhaC190=a.linha;nota=a.documento.numero; chave=a.documento.chave;
            cfop=a.cfop;cst=a.cst;aliquota=a.aliquota;
            participante=a.documento.participante==null?a.documento.participanteCodigo:a.documento.participante.nome;
            credito=a.icms;base=a.baseIcms;this.itens=itens.size();this.elegivel=elegivel;this.motivo=motivo;
        }
    }
    public static final class Mudanca {
        public final int linha;
        public final String registro, campo, antes, depois;
        Mudanca(int linha,String reg,String campo,String antes,String depois) {
            this.linha=linha;this.registro=reg;this.campo=campo;this.antes=antes;this.depois=depois;
        }
        @Override public String toString(){return "L"+linha+" "+registro+"."+campo+" : "+antes+" -> "+depois;}
    }
    public static final class Plano {
        public final List<Mudanca> mudancas;
        public final BigDecimal valorCredito;
        public final int grupos, documentos;
        public final String resumo;
        private final String conteudo;
        private final Charset charset;
        private final File origem;
        private final String shaOrigem;
        private Plano(List<Mudanca> mudancas,BigDecimal credito,int grupos,int documentos,
                String conteudo,Charset charset,File origem,String shaOrigem) {
            this.mudancas=Collections.unmodifiableList(mudancas);this.valorCredito=credito;
            this.grupos=grupos;this.documentos=documentos;this.conteudo=conteudo;
            this.charset=charset;this.origem=origem;this.shaOrigem=shaOrigem;
            this.resumo="Correção assistida: "+grupos+" grupos C190 / "+documentos+" notas; crédito a excluir: R$ "+credito.toPlainString()+".";
        }
    }

    public static List<Caso> analisar(SpedParser.Resultado r) {
        if(r==null)return Collections.emptyList();
        List<Caso> lista=new ArrayList<Caso>();
        for (SpedParser.Analitico a:r.analiticos) {
            if(a.documento==null || !"Entrada".equals(a.documento.operacao)
                    || !CFOPS.contains(a.cfop) || a.icms==null)continue;
            boolean temCredito=a.icms.signum()>0;
            // Também reconhece o TXT produzido pela v0.1.7/0.1.8: crédito zerado,
            // mas CST e alíquota de entrada ainda pendentes de reclassificação.
            boolean legado=a.icms.signum()==0 && a.baseIcms!=null && a.baseIcms.signum()==0
                    && a.aliquota!=null && numericoIgual(a.aliquota,"0")==false
                    && a.cst.matches("[0-8](00|20)");
            if(!temCredito&&!legado)continue;
            List<SpedParser.Item> filhos=filhos(r,a);
            String motivo=verificarGrupo(r,a,filhos);
            if(motivo==null && !a.cst.matches("[0-8](00|20)"))
                motivo="CST fora do tratamento assistido (00/20): confirmar manualmente.";
            lista.add(new Caso(a,filhos,motivo==null,motivo==null
                ? (legado?"Legado 0.1.8: revisar CST/ALIQ (crédito já zerado)":"Elegível à revisão assistida")
                : "Bloqueado: "+motivo));
        }
        return lista;
    }
    private static List<SpedParser.Item> filhos(SpedParser.Resultado r,SpedParser.Analitico a) {
        List<SpedParser.Item> itens=new ArrayList<SpedParser.Item>();
        for(SpedParser.Item item:r.itens)if(item.documento==a.documento && a.cfop.equals(item.cfop)
                && a.cst.equals(item.cst) && numericoIgual(a.aliquota,item.aliquota))itens.add(item);
        return itens;
    }
    private static boolean numericoIgual(String a,String b) {
        try{return num(a).compareTo(num(b))==0;}catch(RuntimeException ex){return false;}
    }
    private static String verificarGrupo(SpedParser.Resultado r,SpedParser.Analitico a,List<SpedParser.Item> filhos) {
        if (a.documento==null || !"Entrada".equals(a.documento.operacao))return "Operação não é entrada.";
        if(!"00".equals(a.documento.situacao))return "Documento não é regular (COD_SIT distinto de 00).";
        if(a.baseIcms==null||a.icms==null)return "Campo monetário inválido.";
        if(filhos.isEmpty())return "Não há itens C170 correspondentes.";
        for(SpedParser.Item item:filhos)
            if(item.baseIcms==null || item.icms==null)return "Item C170 com base/ICMS ausente ou inválido.";
        // Existe somente um C190 para a combinação de CST, CFOP e alíquota.
        int encontrados=0;
        for(SpedParser.Analitico outro:r.analiticos)
            if(outro.documento==a.documento&&a.cst.equals(outro.cst)&&a.cfop.equals(outro.cfop)
                    && numericoIgual(a.aliquota,outro.aliquota))encontrados++;
        if(encontrados!=1)return "Combinação C190 duplicada.";
        BigDecimal base=ZERO,icms=ZERO;
        for(SpedParser.Item item:filhos){base=base.add(item.baseIcms);icms=icms.add(item.icms);}
        if(base.compareTo(a.baseIcms)!=0 || icms.compareTo(a.icms)!=0)
            return "C170 e C190 não conciliam base e crédito.";
        return null;
    }
    private static BigDecimal num(String s){return new BigDecimal(s.replace(',','.'));}
    private static BigDecimal valor(String[] v,int idx)throws IOException {
        try {if(idx>=v.length||v[idx].isEmpty())throw new NumberFormatException();return num(v[idx]);}
        catch(NumberFormatException ex){throw new IOException("Número ausente ou inválido no campo "+idx);}
    }
    private static String reg(String linha){String[] a=linha.split("\\|",-1);return a.length>1?a[1]:"";}
    private static String[] campos(String l){return l.split("\\|",-1);}
    private static void set(String[][] linhas,List<Mudanca> mudancas,int numLinha,int indice,String campo,BigDecimal novo) throws IOException {
        String[] a=linhas[numLinha-1];
        if(indice>=a.length-1)throw new IOException("Registro incompleto na linha "+numLinha);
        BigDecimal anterior=valor(a,indice);
        if(anterior.compareTo(novo)==0)return;
        String antes=a[indice];a[indice]=novo.setScale(2,java.math.RoundingMode.UNNECESSARY).toPlainString().replace('.',',');
        mudancas.add(new Mudanca(numLinha,a[1],campo,antes,a[indice]));
    }
    private static byte[] sha(byte[] data) throws IOException {
        try {return MessageDigest.getInstance("SHA-256").digest(data);}
        catch(Exception ex){throw new IOException("SHA-256 indisponível",ex);}
    }
    private static String hex(byte[] bytes){StringBuilder sb=new StringBuilder();for(byte b:bytes)sb.append(String.format("%02x", b&255));return sb.toString();}
    private static BigDecimal soma(List<SpedParser.Analitico> analiticos,SpedParser.Doc doc,boolean base) throws IOException {
        BigDecimal s=ZERO;for(SpedParser.Analitico a:analiticos)if(a.documento==doc) {
            BigDecimal n=base?a.baseIcms:a.icms;if(n==null)throw new IOException("C190 com número inválido, documento "+doc.numero);s=s.add(n);
        }return s;
    }
    private static int contar(SpedParser.Resultado r,String codigo) {
        if("E100".equals(codigo))return r.countE100;
        if("E110".equals(codigo))return r.countE110;
        return 0;
    }

    // Os índices neste editor são os índices da linha dividida por "|" (posição 1 = REG).
    private static void texto(String[][] dados,List<Mudanca> mudanças,int linha,int pos,String campo,String novo) throws IOException {
        String[] a=dados[linha-1];
        if(pos>=a.length-1)throw new IOException("Registro incompleto na linha "+linha+" ("+campo+").");
        String antigo=a[pos];
        if(antigo.equals(novo))return;
        a[pos]=novo;
        mudanças.add(new Mudanca(linha,a[1],campo,antigo,novo));
    }
    private static String formatar(BigDecimal n){
        if(n.signum()<0)throw new IllegalArgumentException("Resultado negativo não suportado.");
        return n.setScale(2,java.math.RoundingMode.HALF_UP).toPlainString().replace('.',',');
    }
    private static String chave(String[] linha){
        return linha[2]+"|"+linha[3]+"|"+formatar(num(linha[4]));
    }
    private static String grupoNovo(String[] linha){return chave(linha);}
    private static void ajustarFechamentos(String[][] linhas,boolean[] excluir,List<Mudanca> changes)throws IOException {
        int excluidos=0;
        for(boolean b:excluir)if(b)excluidos++;
        if(excluidos==0)return;
        int blocoC=0,contagemC190=0;
        for(int i=0;i<linhas.length;i++){
            String reg=linhas[i].length>1?linhas[i][1]:"";
            if(!excluir[i]&&reg.startsWith("C"))blocoC++;
            if(!excluir[i]&&"C190".equals(reg))contagemC190++;
        }
        boolean c990=false, r9900=false, total=false;
        for(int i=0;i<linhas.length;i++){
            String[] a=linhas[i];String reg=a.length>1?a[1]:"";
            if("C990".equals(reg)){
                texto(linhas,changes,i+1,2,"QTD_LIN_C",String.valueOf(blocoC));c990=true;
            }else if("9900".equals(reg)&&a.length>3&&"C190".equals(a[2])){
                texto(linhas,changes,i+1,3,"QTD_REG_BLC (C190)",String.valueOf(contagemC190));r9900=true;
            }else if("9999".equals(reg)){
                texto(linhas,changes,i+1,2,"QTD_LIN",String.valueOf(linhas.length-excluidos));total=true;
            }
        }
        if(!c990||!r9900||!total)throw new IOException("Finalizadores C990/9900-C190/9999 ausentes; não é seguro reagrupar.");
    }
    private static final class ChaveDoc {
        final SpedParser.Doc doc; final String chave;
        ChaveDoc(SpedParser.Doc doc,String chave){this.doc=doc;this.chave=chave;}
        public boolean equals(Object other){
            if(!(other instanceof ChaveDoc))return false;
            ChaveDoc o=(ChaveDoc)other;return doc==o.doc&&chave.equals(o.chave);
        }
        public int hashCode(){return System.identityHashCode(doc)*31+chave.hashCode();}
    }
    private static final int[] CAMPOS_SOMATORIOS={5,6,7,8,9,10,11};

    /**
     * Fluxo fiscal assistido: CST + alíquota + base + crédito do C170/C190,
     * consolidação C190, C100, E110 e fechamentos do bloco C se linhas forem reunidas.
     * Apenas 1556/2556/3556 com CST de tributação 00 ou 20 e documentação confirmada.
     */
    public static Plano planejar(SpedParser.Resultado r,Set<Integer> linhasC190) throws IOException {
        return planejar(r,linhasC190,false);
    }
    public static Plano planejar(SpedParser.Resultado r,Set<Integer> linhasC190,boolean substituta)throws IOException {
        if(r==null||r.arquivo==null)throw new IOException("Importe uma EFD antes da correção.");
        if(linhasC190==null||linhasC190.isEmpty())throw new IOException("Selecione pelo menos um grupo C190.");
        if(!"MG".equalsIgnoreCase(r.uf)||!"020".equals(r.codVer))
            throw new IOException("O piloto é restrito a MG, leiaute 020/2026.");
        if(!r.tem0000||r.linhasIgnoradas!=0||!r.ocorrencias.isEmpty())
            throw new IOException("Existem alertas de leitura ou linhas ignoradas; revisão manual obrigatória.");
        if(r.countE100!=1||r.countE110!=1||r.apuracoes.size()!=1)
            throw new IOException("Correção exige um único período E100/E110.");
        if(Files.size(r.arquivo.toPath())>100L*1024L*1024L)
            throw new IOException("Arquivo acima de 100 MB: editor conservador bloqueado.");
        Map<Integer,Caso> elegiveis=new HashMap<Integer,Caso>();
        for(Caso c:analisar(r))if(c.elegivel)elegiveis.put(c.linhaC190,c);
        for(Integer linha:linhasC190)if(!elegiveis.containsKey(linha))
            throw new IOException("C190 linha "+linha+" não elegível. Confira a destinação e a conciliação.");

        byte[] raw=Files.readAllBytes(r.arquivo.toPath());
        Charset cs=Charset.forName(r.charset);
        String original=new String(raw,cs);
        boolean bom=original.startsWith("﻿");if(bom)original=original.substring(1);
        String quebra=original.contains("\r\n")?"\r\n":"\n";
        if(original.replace("\r\n","").contains("\r"))throw new IOException("Quebra CR isolada não suportada.");
        String[] linhas=original.split(java.util.regex.Pattern.quote(quebra),-1);
        boolean terminador=linhas.length>0&&linhas[linhas.length-1].isEmpty();
        int qtd=linhas.length-(terminador?1:0);
        if(qtd!=r.linhasLidas)throw new IOException("Quantidade de registros difere do arquivo lido.");
        String[][] dados=new String[qtd][];
        for(int i=0;i<qtd;i++){
            dados[i]=campos(linhas[i]);
            if(dados[i].length<4||!"".equals(dados[i][0])||!"".equals(dados[i][dados[i].length-1]))
                throw new IOException("Linha fora do padrão: "+(i+1));
            String tipo=dados[i][1];
            if(Arrays.asList("E111","E112","E113","E115","E116","E200","E210","E220","E310","E311",
                   "C197","C597","D197","D597","C590","D190","D590","D730","G110","1900","1920").contains(tipo))
                throw new IOException("Registro "+tipo+" (linha "+(i+1)+") requer apuração fiscal mais complexa.");
        }
        // Bloquear documentos com filhos de C190 ou outros mecanismos cuja relação será alterada.
        for(SpedParser.Doc d:r.documentos){
            if(!"55".equals(d.modelo)||!"00".equals(d.situacao))continue;
            boolean atingida=false;for(SpedParser.Analitico a:r.analiticos)if(a.documento==d&&linhasC190.contains(a.linha))atingida=true;
            if(!atingida)continue;
            for(int i=d.linha;i<qtd;i++){
                String reg=dados[i][1];
                if("C100".equals(reg)||"C990".equals(reg))break;
                if("C191".equals(reg)||"C195".equals(reg)||"C197".equals(reg))
                    throw new IOException("Documento "+d.numero+" possui "+reg+"; reagrupamento C190 bloqueado.");
            }
        }
        String[] e110=dados[r.apuracoes.get(0).linha-1];
        if(!"E110".equals(e110[1])||e110.length<17)throw new IOException("E110 incompleto.");
        for(int f=2;f<=15;f++)if(f!=6&&f!=14&&valor(e110,f).compareTo(ZERO)!=0)
            throw new IOException("E110 contém valores/ajustes no campo "+f+"; recalculadora simplificada não se aplica.");
        BigDecimal somaCreditos=ZERO;
        for(SpedParser.Analitico a:r.analiticos)if(a.documento!=null
              &&"Entrada".equals(a.documento.operacao)&&"00".equals(a.documento.situacao)&&a.icms!=null)
              somaCreditos=somaCreditos.add(a.icms);
        if(somaCreditos.compareTo(valor(e110,6))!=0||somaCreditos.compareTo(valor(e110,14))!=0)
            throw new IOException("E110 não concilia com créditos C190 antes da correção.");
        for(SpedParser.Doc d:r.documentos){
            if(!"C100".equals(dados[d.linha-1][1])
               ||valor(dados[d.linha-1],21).compareTo(soma(r.analiticos,d,true))!=0
               ||valor(dados[d.linha-1],22).compareTo(soma(r.analiticos,d,false))!=0)
               throw new IOException("C100 divergente do C190 antes da correção: NF "+d.numero);
        }
        List<Mudanca> mudanças=new ArrayList<Mudanca>();
        if(substituta){
            if(!"0".equals(dados[0][3])&&!"1".equals(dados[0][3]))throw new IOException("COD_FIN inválido.");
            if("0".equals(dados[0][3]))texto(dados,mudanças,1,3,"COD_FIN","1");
        }
        BigDecimal removido=ZERO;
        Set<SpedParser.Doc> documentos=new HashSet<SpedParser.Doc>();
        for(SpedParser.Analitico a:r.analiticos){
            if(!linhasC190.contains(a.linha))continue;
            List<SpedParser.Item> filhos=filhos(r,a);
            if(verificarGrupo(r,a,filhos)!=null)throw new IOException("C190 não conciliado: L"+a.linha);
            if(!a.cst.matches("[0-8](00|20)"))
                throw new IOException("CST "+a.cst+" exige decisão específica; não será convertido automaticamente.");
            if(!CFOPS.contains(a.cfop))throw new IOException("CFOP fora do escopo de uso e consumo.");
            for(SpedParser.Item it:filhos){
                if(!it.cst.equals(a.cst)||!it.cfop.equals(a.cfop))throw new IOException("C170 incompatível.");
                texto(dados,mudanças,it.linha,10,"CST_ICMS",it.cst.substring(0,1)+"90");
                texto(dados,mudanças,it.linha,13,"VL_BC_ICMS", "0,00");
                texto(dados,mudanças,it.linha,14,"ALIQ_ICMS","0");
                texto(dados,mudanças,it.linha,15,"VL_ICMS","0,00");
            }
            texto(dados,mudanças,a.linha,2,"CST_ICMS",a.cst.substring(0,1)+"90");
            texto(dados,mudanças,a.linha,4,"ALIQ_ICMS","0");
            texto(dados,mudanças,a.linha,6,"VL_BC_ICMS","0,00");
            texto(dados,mudanças,a.linha,7,"VL_ICMS","0,00");
            documentos.add(a.documento);
            removido=removido.add(a.icms);
        }
        // Reagrupar C190 se o novo CST+CFOP+alíquota coincidir com grupo preexistente.
        // Importante: apenas soma linhas C190 equivalentes e COM o mesmo COD_OBS.
        boolean[] excluir=new boolean[qtd];
        Map<ChaveDoc,Integer> conhecidos=new LinkedHashMap<ChaveDoc,Integer>();
        for(SpedParser.Analitico a:r.analiticos){
            if(!documentos.contains(a.documento))continue;
            int linha=a.linha;
            String[] atual=dados[linha-1];
            ChaveDoc chave=new ChaveDoc(a.documento,grupoNovo(atual));
            Integer anterior=conhecidos.get(chave);
            if(anterior==null){conhecidos.put(chave,linha);continue;}
            String[] principal=dados[anterior-1];
            if(!principal[12].equals(atual[12]))
                throw new IOException("C190 L"+anterior+" e L"+linha+" têm COD_OBS diferentes; fusão bloqueada.");
            for(int pos:CAMPOS_SOMATORIOS){
                BigDecimal novo=valor(principal,pos).add(valor(atual,pos));
                texto(dados,mudanças,anterior,pos,"C190 soma "+pos,formatar(novo));
            }
            excluir[linha-1]=true;
            mudanças.add(new Mudanca(linha,"C190","REGISTRO",String.join("|",atual),"REMOVIDO NA CONSOLIDAÇÃO"));
        }
        // Recalcular C100 e E110 após fusões.
        for(SpedParser.Doc d:documentos){
            BigDecimal base=ZERO,credito=ZERO;
            for(SpedParser.Analitico a:r.analiticos)if(a.documento==d&&!excluir[a.linha-1]){
                base=base.add(valor(dados[a.linha-1],6));credito=credito.add(valor(dados[a.linha-1],7));
            }
            texto(dados,mudanças,d.linha,21,"VL_BC_ICMS",formatar(base));
            texto(dados,mudanças,d.linha,22,"VL_ICMS",formatar(credito));
        }
        BigDecimal novoCredito=valor(e110,6).subtract(removido);
        if(novoCredito.signum()<0)throw new IOException("Novo crédito negativo.");
        texto(dados,mudanças,r.apuracoes.get(0).linha,6,"VL_TOT_CREDITOS",formatar(novoCredito));
        texto(dados,mudanças,r.apuracoes.get(0).linha,14,"VL_SLD_CREDOR_TRANSPORTAR",formatar(novoCredito));
        // Verificação pós-alteração: todos os pares C170/C190 dos documentos modificados.
        for(SpedParser.Doc d:documentos){
            Map<String,BigDecimal[]> acumulado=new HashMap<String,BigDecimal[]>();
            for(SpedParser.Item item:r.itens)if(item.documento==d){
                String[] l=dados[item.linha-1];String chave=grupoNovoItem(l);
                BigDecimal[] s=acumulado.get(chave);
                if(s==null){s=new BigDecimal[]{ZERO,ZERO};acumulado.put(chave,s);}
                s[0]=s[0].add(valor(l,13));s[1]=s[1].add(valor(l,15));
            }
            for(SpedParser.Analitico a:r.analiticos)if(a.documento==d&&!excluir[a.linha-1]){
                String[] l=dados[a.linha-1];BigDecimal[] s=acumulado.remove(grupoNovo(l));
                if(s==null||s[0].compareTo(valor(l,6))!=0||s[1].compareTo(valor(l,7))!=0)
                    throw new IOException("C170/C190 incompatíveis após correção: NF "+d.numero+" C190 L"+a.linha);
            }
            if(!acumulado.isEmpty())throw new IOException("C170 sem C190 correspondente após correção: NF "+d.numero);
        }
        BigDecimal totalC190=ZERO;
        for(SpedParser.Analitico a:r.analiticos)if(a.documento!=null
             &&"Entrada".equals(a.documento.operacao)&&"00".equals(a.documento.situacao)&&!excluir[a.linha-1])
             totalC190=totalC190.add(valor(dados[a.linha-1],7));
        if(totalC190.compareTo(novoCredito)!=0)throw new IOException("E110 não concilia após atualização.");
        ajustarFechamentos(dados,excluir,mudanças);
        List<String> novasLinhas=new ArrayList<String>();
        for(int i=0;i<qtd;i++)if(!excluir[i])novasLinhas.add(String.join("|",dados[i]));
        String novo=(bom?"﻿":"")+String.join(quebra,novasLinhas)+(terminador?quebra:"");
        return new Plano(mudanças,removido,linhasC190.size(),documentos.size(),novo,cs,r.arquivo,hex(sha(raw)));
    }
    private static String grupoNovoItem(String[] l) throws IOException{
        if(l.length<=15)throw new IOException("C170 incompleto.");
        return l[10]+"|"+l[11]+"|"+formatar(num(l[14]));
    }
    /** Grava em temporário, valida estrutura e só então publica a saída (sem sobrescrever). */
    public static File salvar(Plano plano,File destino) throws IOException {
        if(plano==null||destino==null)throw new IOException("Plano ou destino ausente.");
        String nome=destino.getName().toLowerCase(java.util.Locale.ROOT);
        if(!nome.endsWith(".sped")&&!nome.endsWith(".txt"))
            throw new IOException("A saída deve ser .SPED ou .TXT.");
        Path origem=plano.origem.toPath().toRealPath();
        Path saida=destino.toPath().toAbsolutePath().normalize();
        if(origem.equals(saida))throw new IOException("Não é permitido sobrescrever a EFD original.");
        if(Files.exists(saida))throw new IOException("Destino já existe; escolha outro nome.");
        if(!Files.isDirectory(saida.getParent()))throw new IOException("Diretório de destino não existe.");
        byte[] atual=Files.readAllBytes(origem);
        if(!hex(sha(atual)).equals(plano.shaOrigem))
            throw new IOException("Arquivo de origem mudou; reimporte antes de salvar.");
        byte[] novo=plano.conteudo.getBytes(plano.charset);
        if(Arrays.equals(atual,novo))throw new IOException("Plano sem alterações.");
        Path temporario=Files.createTempFile(saida.getParent(),".extrator-sped-", ".tmp");
        try {
            Files.write(temporario,novo,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE);
            verificarArquivoTemporario(temporario);
            if(!hex(sha(Files.readAllBytes(origem))).equals(plano.shaOrigem))
                throw new IOException("Arquivo de origem alterado durante a exportação.");
            if(Files.exists(saida))throw new IOException("Destino criado por outro processo; não será sobrescrito.");
            try {Files.move(temporario,saida,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException ex){Files.move(temporario,saida);}
            return saida.toFile();
        } finally {Files.deleteIfExists(temporario);}
    }
    private static void verificarArquivoTemporario(Path arquivo)throws IOException{
        SpedParser.Resultado testado=SpedParser.ler(arquivo.toFile());
        if(!testado.tem0000||testado.linhasIgnoradas>0||!testado.ocorrencias.isEmpty())
            throw new IOException("Falhou o teste estrutural de reimportação do TXT gerado.");
        Charset cs=Charset.forName(testado.charset);
        int c=0, total=0, nC190=0;
        int encerramentoC=-1, totalReg=-1, contador190=-1;
        for(String l:Files.readAllLines(arquivo,cs)){
            if(l.isEmpty())continue;
            c++;String[] f=l.split("\\|",-1);
            if(f.length<4)throw new IOException("Registro gerado incompleto.");
            if(f[1].startsWith("C"))total++;
            if("C190".equals(f[1]))nC190++;
            if("C990".equals(f[1]))encerramentoC=Integer.parseInt(f[2]);
            if("9999".equals(f[1]))totalReg=Integer.parseInt(f[2]);
            if("9900".equals(f[1])&&"C190".equals(f[2]))contador190=Integer.parseInt(f[3]);
        }
        if(encerramentoC!=total||totalReg!=c||contador190!=nC190)
            throw new IOException("Finalizadores C990/9900-C190/9999 inconsistentes na exportação.");
    }
    /** Arquivo de auditoria legível, com hashes e alterações concretas. */
    public static File salvarRelatorio(Plano plano,File arquivoSalvo)throws IOException{
        if(plano==null||arquivoSalvo==null||!arquivoSalvo.isFile())
            throw new IOException("Primeiro salve a EFD corrigida.");
        byte[] bytes=Files.readAllBytes(arquivoSalvo.toPath());
        if(!Arrays.equals(bytes,plano.conteudo.getBytes(plano.charset)))
            throw new IOException("O arquivo de destino já não corresponde ao plano de correção.");
        SpedParser.Resultado resultado=SpedParser.ler(arquivoSalvo);
        StringBuilder report=new StringBuilder();
        report.append("EXTRATOR SPED FISCAL – RELATÓRIO DE AUDITORIA v0.1.11\n")
              .append("=================================================\n")
              .append("Origem: ").append(plano.origem.getAbsolutePath()).append('\n')
              .append("Saída: ").append(arquivoSalvo.getAbsolutePath()).append('\n')
              .append("SHA256 origem: ").append(plano.shaOrigem).append('\n')
              .append("SHA256 saída: ").append(hex(sha(bytes))).append('\n')
              .append("COD_FIN: ").append(resultado.codFin).append('\n')
              .append(plano.resumo).append("\n\nALTERAÇÕES EFETUADAS:\n");
        for(Mudanca m:plano.mudancas)report.append(m).append('\n');
        report.append("\nPENDÊNCIAS APÓS A CORREÇÃO:\n")
              .append(PendenciasTributarias.analisar(resultado).relatorio)
              .append("\nVALIDAÇÃO: passou apenas nos testes internos. PVA não executado.\n")
              .append("Nenhum XML ou banco do software contábil foi modificado.\n");
        Path saida=java.nio.file.Paths.get(arquivoSalvo.getAbsolutePath()+"-auditoria.txt");
        Files.write(saida,report.toString().getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
        return saida.toFile();
    }
}
