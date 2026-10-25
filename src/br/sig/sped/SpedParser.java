package br.sig.sped;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Leitor conservador de dados declarados na EFD ICMS/IPI. Não faz validação fiscal. */
public final class SpedParser {
    private SpedParser() { }

    public static final class Doc {
        public int linha, sequencia;
        public String operacao = "", emissor = "", participanteCodigo = "", serie = "", modelo = "", situacao = "", numero = "", data = "", dataEntradaSaida = "", chave = "";
        public Participante participante;
        public BigDecimal valor = BigDecimal.ZERO;
        public int quantidadeC190, quantidadeC170;
    }
    public static final class Participante {
        public String codigo = "", nome = "", cnpj = "", cpf = "", ie = "";
        public String identificador() { return !cnpj.isEmpty() ? cnpj : cpf; }
    }
    /** Cadastro declarado no 0200, sem completar campos ausentes por inferencia. */
    public static final class Produto {
        public int linha;
        public String codigo="", descricao="", unidadeInventario="", tipo="", ncm="", ean="", exIpi="";
    }
    /** C170 corresponde aos itens escriturados; nao necessariamente todos os itens de NF-e. */
    public static final class Item {
        public int linha;
        public Doc documento;
        public Produto produto;
        /** Campos C170 vazios sao distintos de zero declarado (inclusive QTD em casos especiais de 2026). */
        public final java.util.Set<String> ausentes = new java.util.HashSet<String>();
        public String numero="", codigo="", complemento="", unidade="", movimenta="", cst="", cfop="", aliquota="";
        public BigDecimal quantidade=BigDecimal.ZERO, valor=BigDecimal.ZERO, desconto=BigDecimal.ZERO;
        public BigDecimal baseIcms=BigDecimal.ZERO, icms=BigDecimal.ZERO, baseSt=BigDecimal.ZERO, icmsSt=BigDecimal.ZERO;
        public BigDecimal baseIpi=BigDecimal.ZERO, ipi=BigDecimal.ZERO;
        public String descricao() {
            if (produto!=null && !produto.descricao.isEmpty()) return produto.descricao;
            return complemento.isEmpty()?"(não localizado no 0200)":complemento;
        }
        public String ncm() {return produto==null?"":produto.ncm;}
        public String operacao() {return documento==null?"Indefinida":documento.operacao;}
    }
    public static final class Analitico {
        public int linha;
        public Doc documento;
        public String cst = "", cfop = "", aliquota = "";
        public BigDecimal valorOperacao = BigDecimal.ZERO;
        public BigDecimal baseIcms = BigDecimal.ZERO;
        public BigDecimal icms = BigDecimal.ZERO;
        public BigDecimal baseSt = BigDecimal.ZERO;
        public BigDecimal icmsSt = BigDecimal.ZERO;
        public BigDecimal ipi = BigDecimal.ZERO;
    }
    public static final class Apuracao {
        public int linha;
        public String inicio = "", fim = "";
        public BigDecimal debitos = BigDecimal.ZERO;
        public BigDecimal creditos = BigDecimal.ZERO;
        public BigDecimal saldoApurado = BigDecimal.ZERO;
        public BigDecimal deducoes = BigDecimal.ZERO;
        public BigDecimal recolher = BigDecimal.ZERO;
        public BigDecimal credorTransportar = BigDecimal.ZERO;
    }
    public static final class Ocorrencia {
        public final int linha;
        public final String registro, descricao;
        public Ocorrencia(int linha, String registro, String descricao) {
            this.linha=linha; this.registro=registro; this.descricao=descricao;
        }
    }
    public static final class Resultado {
        public File arquivo;
        public String codVer = "", codFin = "", nome = "", cnpj = "", uf = "", periodoInicio = "", periodoFim = "";
        public String charset = "";
        public long linhasLidas;
        public int linhasIgnoradas;
        public final List<Doc> documentos = new ArrayList<Doc>();
        public final Map<String,Produto> produtos = new LinkedHashMap<String,Produto>();
        public final List<Item> itens = new ArrayList<Item>();
        public final Map<String,Participante> participantes = new LinkedHashMap<String,Participante>();
        public final List<Analitico> analiticos = new ArrayList<Analitico>();
        public final List<Apuracao> apuracoes = new ArrayList<Apuracao>();
        public final List<Ocorrencia> ocorrencias = new ArrayList<Ocorrencia>();
        public boolean tem0000;
        public int countE100, countE110;
    }

    /** Supõe UTF-8 somente quando todo conteúdo é UTF-8 válido; caso contrário ISO-8859-1. */
    private static Charset detectarCharset(Path caminho) throws IOException {
        // A EFD é texto plano. Ler em UTF-8 com decodificação rígida evita caracteres quebrados;
        // a leitura com ISO-8859-1 cobre arquivos legados sem BOM.
        byte[] probe = new byte[8192];
        int lidos;
        try (java.io.InputStream in = Files.newInputStream(caminho)) { lidos = in.read(probe); }
        if (lidos < 0) return StandardCharsets.UTF_8;
        int offset = (lidos >= 3 && (probe[0]&0xff)==0xef && (probe[1]&0xff)==0xbb && (probe[2]&0xff)==0xbf) ? 3 : 0;
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(probe, offset, lidos-offset));
            return StandardCharsets.UTF_8;
        } catch (CharacterCodingException ex) {
            return StandardCharsets.ISO_8859_1;
        }
    }

    public static Resultado ler(File arquivo) throws IOException {
        if (arquivo == null || !arquivo.isFile()) throw new IOException("Arquivo EFD/SPED não encontrado.");
        Charset charset = detectarCharset(arquivo.toPath());
        try {
            return lerComCharset(arquivo, charset);
        } catch (java.nio.charset.MalformedInputException ex) {
            // A amostra inicial parecia UTF-8, mas havia bytes legados depois dela.
            if (charset.equals(StandardCharsets.UTF_8))
                return lerComCharset(arquivo, StandardCharsets.ISO_8859_1);
            throw ex;
        }
    }

    private static Resultado lerComCharset(File arquivo, Charset charset) throws IOException {
        Resultado resultado = new Resultado();
        resultado.arquivo = arquivo;
        resultado.charset = charset.name();
        Doc atual = null;
        String inicioE100 = "", fimE100 = "";
        try (BufferedReader reader = Files.newBufferedReader(arquivo.toPath(), charset)) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                resultado.linhasLidas++;
                int n = (int)Math.min(Integer.MAX_VALUE, resultado.linhasLidas);
                if (n == 1 && linha.startsWith("\uFEFF")) linha = linha.substring(1);
                if (linha.trim().isEmpty()) { resultado.linhasIgnoradas++; continue; }
                if (!linha.startsWith("|") || !linha.endsWith("|")) {
                    adicionar(resultado, n, "?", "Linha sem delimitadores inicial/final '|'; ignorada.");
                    resultado.linhasIgnoradas++;
                    continue;
                }
                String[] campos = linha.split("\\|", -1);
                String reg = campo(campos,1);
                if ("0000".equals(reg)) {
                    resultado.tem0000 = true;
                    if (campos.length < 16) adicionar(resultado,n,reg,"Quantidade de campos menor que a esperada no registro 0000.");
                    resultado.codVer = campo(campos,2);
                    resultado.codFin = campo(campos,3);
                    if (!"0".equals(resultado.codFin) && !"1".equals(resultado.codFin))
                        adicionar(resultado,n,reg,"COD_FIN deve ser 0 (original) ou 1 (substituta).");
                    resultado.periodoInicio = campo(campos,4);
                    resultado.periodoFim = campo(campos,5);
                    resultado.nome = campo(campos,6);
                    resultado.cnpj = campo(campos,7);
                    resultado.uf = campo(campos,9);
                } else if ("0150".equals(reg)) {
                    if (campos.length < 9) adicionar(resultado,n,reg,"Registro 0150 aparentemente incompleto; confira os campos.");
                    Participante p = new Participante();
                    p.codigo = campo(campos,2);
                    p.nome = campo(campos,3);
                    p.cnpj = campo(campos,5);
                    p.cpf = campo(campos,6);
                    p.ie = campo(campos,7);
                    if (!p.codigo.isEmpty()) resultado.participantes.put(p.codigo,p);
                } else if ("0200".equals(reg)) {
                    if (campos.length < 13) adicionar(resultado,n,reg,"0200 aparentemente incompleto; campos nao informados serao exibidos em branco.");
                    Produto p = new Produto();
                    p.linha=n;
                    p.codigo=campo(campos,2); p.descricao=campo(campos,3); p.ean=campo(campos,4);
                    p.unidadeInventario=campo(campos,6);p.tipo=campo(campos,7);p.ncm=campo(campos,8);p.exIpi=campo(campos,9);
                    if(p.codigo.isEmpty()) adicionar(resultado,n,reg,"0200 com COD_ITEM vazio; cadastro ignorado.");
                    else {
                        if(resultado.produtos.containsKey(p.codigo)) adicionar(resultado,n,reg,"COD_ITEM repetido no cadastro 0200: "+limitar(p.codigo,45)+". Ultima definicao exibida.");
                        resultado.produtos.put(p.codigo,p);
                    }
                } else if ("C100".equals(reg)) {
                    if (campos.length < 30) adicionar(resultado,n,reg,"Registro C100 aparentemente incompleto; confira os campos.");
                    atual = new Doc();
                    atual.linha = n;
                    atual.sequencia = resultado.documentos.size()+1;
                    atual.operacao = "0".equals(campo(campos,2)) ? "Entrada" : "1".equals(campo(campos,2)) ? "Saída" : "Indefinida";
                    atual.emissor = "0".equals(campo(campos,3)) ? "Própria" : "1".equals(campo(campos,3)) ? "Terceiros" : "Indefinido";
                    atual.participanteCodigo = campo(campos,4);
                    atual.modelo = campo(campos,5);
                    atual.situacao = campo(campos,6);
                    atual.serie = campo(campos,7);
                    atual.numero = campo(campos,8);
                    atual.chave = campo(campos,9);
                    atual.data = campo(campos,10);
                    atual.dataEntradaSaida = campo(campos,11);
                    atual.valor = numero(resultado,n,reg,"VL_DOC",campo(campos,12));
                    resultado.documentos.add(atual);
                } else if ("C170".equals(reg)) {
                    if (campos.length < 40) adicionar(resultado,n,reg,"C170 aparentemente incompleto; campos ausentes serao exibidos em branco.");
                    Item item = new Item();
                    item.linha=n;item.documento=atual;
                    item.numero=campo(campos,2);item.codigo=campo(campos,3);item.complemento=campo(campos,4);
                    item.quantidade=numeroItem(resultado,item,n,"QTD",campo(campos,5));item.unidade=campo(campos,6);
                    item.valor=numeroItem(resultado,item,n,"VL_ITEM",campo(campos,7));item.desconto=numeroItem(resultado,item,n,"VL_DESC",campo(campos,8));
                    item.movimenta=campo(campos,9);item.cst=campo(campos,10);item.cfop=campo(campos,11);
                    item.baseIcms=numeroItem(resultado,item,n,"VL_BC_ICMS",campo(campos,13));item.aliquota=campo(campos,14);
                    item.icms=numeroItem(resultado,item,n,"VL_ICMS",campo(campos,15));
                    item.baseSt=numeroItem(resultado,item,n,"VL_BC_ICMS_ST",campo(campos,16));
                    item.icmsSt=numeroItem(resultado,item,n,"VL_ICMS_ST",campo(campos,18));
                    item.baseIpi=numeroItem(resultado,item,n,"VL_BC_IPI",campo(campos,22));
                    item.ipi=numeroItem(resultado,item,n,"VL_IPI",campo(campos,24));
                    if(atual==null) adicionar(resultado,n,reg,"C170 sem C100 pai na leitura; item desvinculado.");
                    else atual.quantidadeC170++;
                    resultado.itens.add(item);
                } else if ("C190".equals(reg)) {
                    if (campos.length < 14) adicionar(resultado,n,reg,"Registro C190 aparentemente incompleto; confira os campos.");
                    Analitico a = new Analitico();
                    a.linha = n;
                    a.documento = atual;
                    a.cst = campo(campos,2);
                    a.cfop = campo(campos,3);
                    a.aliquota = campo(campos,4);
                    a.valorOperacao = numero(resultado,n,reg,"VL_OPR",campo(campos,5));
                    a.baseIcms = numero(resultado,n,reg,"VL_BC_ICMS",campo(campos,6));
                    a.icms = numero(resultado,n,reg,"VL_ICMS",campo(campos,7));
                    a.baseSt = numero(resultado,n,reg,"VL_BC_ICMS_ST",campo(campos,8));
                    a.icmsSt = numero(resultado,n,reg,"VL_ICMS_ST",campo(campos,9));
                    a.ipi = numero(resultado,n,reg,"VL_IPI",campo(campos,11));
                    if (atual == null) adicionar(resultado,n,reg,"C190 sem C100 associado na leitura; operação e nota desconhecidas.");
                    else atual.quantidadeC190++;
                    resultado.analiticos.add(a);
                } else if ("E100".equals(reg)) {
                    resultado.countE100++;
                    inicioE100 = campo(campos,2);
                    fimE100 = campo(campos,3);
                } else if ("E110".equals(reg)) {
                    resultado.countE110++;
                    if (campos.length < 17) adicionar(resultado,n,reg,"Registro E110 aparentemente incompleto; confira os campos.");
                    Apuracao a = new Apuracao();
                    a.linha=n;
                    a.inicio=inicioE100;
                    a.fim=fimE100;
                    a.debitos=numero(resultado,n,reg,"VL_TOT_DEBITOS",campo(campos,2));
                    a.creditos=numero(resultado,n,reg,"VL_TOT_CREDITOS",campo(campos,6));
                    a.saldoApurado=numero(resultado,n,reg,"VL_SLD_APURADO",campo(campos,11));
                    a.deducoes=numero(resultado,n,reg,"VL_TOT_DED",campo(campos,12));
                    a.recolher=numero(resultado,n,reg,"VL_ICMS_RECOLHER",campo(campos,13));
                    a.credorTransportar=numero(resultado,n,reg,"VL_SLD_CREDOR_TRANSPORTAR",campo(campos,14));
                    if (inicioE100.length()==0) adicionar(resultado,n,reg,"E110 sem E100 anterior na leitura.");
                    resultado.apuracoes.add(a);
                } else if (reg.equals("C001") || reg.equals("C010") || reg.equals("C300") || reg.equals("C350") ||
                        reg.equals("C400") || reg.equals("C405") || reg.equals("C460") || reg.equals("C490") ||
                        reg.equals("C500") || reg.equals("C600") || reg.equals("C700") || reg.equals("C800") ||
                        reg.equals("C860") || reg.equals("C990")) {
                    atual = null;
                }
            }
        }
        // O cadastro 0150 pode aparecer antes ou depois dos documentos: vincule após a leitura inteira.
        for (Doc doc : resultado.documentos)
            if (!doc.participanteCodigo.isEmpty())
                doc.participante = resultado.participantes.get(doc.participanteCodigo);
        // Resolve codigos de produto depois da leitura: a ordem dos registros nao e pressuposta.
        for(Item item:resultado.itens) {
            item.produto=resultado.produtos.get(item.codigo);
            if(item.produto==null) adicionar(resultado,item.linha,"C170","COD_ITEM '"+limitar(item.codigo,45)+"' nao localizado no 0200 desta escrituração (conferencia cadastral; nao e parecer fiscal).");
        }
        if (!resultado.tem0000) adicionar(resultado,0,"0000","Registro de abertura 0000 não encontrado.");
        return resultado;
    }

    private static BigDecimal numeroItem(Resultado r,Item item,int linha,String nome,String valor) {
        if (valor.isEmpty()) {item.ausentes.add(nome);return null;}
        return numero(r,linha,"C170",nome,valor);
    }
    private static String campo(String[] v, int indice) { return indice < v.length ? v[indice].trim() : ""; }
    private static BigDecimal numero(Resultado r, int linha, String registro, String nome, String valor) {
        if (valor.isEmpty()) return BigDecimal.ZERO;
        try {
            // EFD usa vírgula como separador decimal e nenhum separador de milhares.
            if (!valor.matches("-?[0-9]+(,[0-9]+)?")) throw new NumberFormatException();
            return new BigDecimal(valor.replace(',', '.'));
        } catch (NumberFormatException ex) {
            adicionar(r,linha,registro,"Número inválido em " + nome + ": '" + limitar(valor,50) + "'. Excluído do respectivo total; confira o arquivo.");
            return null;
        }
    }
    private static String limitar(String v,int max) { return v.length()>max ? v.substring(0,max)+"..." : v; }
    private static void adicionar(Resultado r,int l,String reg,String desc) { r.ocorrencias.add(new Ocorrencia(l,reg,desc)); }
}
