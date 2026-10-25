package br.sig.sped;

import java.util.LinkedHashMap;
import java.util.Map;

/** Manual embutido, offline e independente de arquivos externos. */
public final class AjudaInterna {
    private AjudaInterna() { }
    private static final Map<String,String> TOPICOS=new LinkedHashMap<String,String>();
    static {
        TOPICOS.put("inicio",html("Bem-vindo à Ajuda",
            "<p>Este manual funciona dentro da área de trabalho do Extrator SPED Fiscal, mesmo sem internet.</p>"+
            "<h3>Como trabalhar</h3><ol><li>Abra uma EFD (.SPED ou .TXT), original ou substituta.</li>"+
            "<li>Entre em <b>Auditoria Fiscal</b> e escolha uma ferramenta disponível.</li>"+
            "<li>Revise as ocorrências e confirme os fatos tributários antes de qualquer correção.</li>"+
            "<li>Salve em outro arquivo, reimporte e valide no PVA oficial.</li></ol>"+
            "<p><b>Importante:</b> o sistema lê arquivos localmente, não assina nem transmite EFD.</p>"));
        TOPICOS.put("creditos",html("RN07 — Créditos de uso e consumo",
            "<p><b>O que faz:</b> localiza grupos potencialmente incompatíveis com aproveitamento de ICMS em compras com CFOP 1.556, 2.556 ou 3.556.</p>"+
            "<p><b>Como usar:</b> importe a EFD, abra o módulo, marque somente ocorrências conciliadas com destinação de uso/consumo <b>comprovada</b>, revise a prévia e salve uma nova cópia.</p>"+
            "<p><b>O que pode mudar:</b> CST (em situações suportadas), alíquota, base de cálculo, ICMS de C170/C190 e reflexos em C100/E110. Pode haver reagrupamento de C190 e atualização de contadores.</p>"+
            "<p><b>Limites atuais:</b> piloto para MG, leiaute 020, apuração simples sem ajustes complexos; não modifica CFOP, XML, sistema de origem nem cadastro 0200. Casos fora do escopo são bloqueados. A marcação não comprova o direito tributário.</p>"));
        TOPICOS.put("diagnostico",html("Diagnóstico complementar — bases, CST e 0200",
            "<p><b>O que faz:</b> lista base positiva sem valor de crédito em C190, alíquota positiva em itens sem crédito e cadastros 0200 tipo 00 associados apenas a CFOPs típicos de uso/consumo no arquivo.</p>"+
            "<p><b>Como usar:</b> abra a EFD, selecione esta ferramenta e leia o relatório na área de trabalho. Confira os documentos e a destinação antes de decidir o tratamento.</p>"+
            "<p><b>Limite:</b> são indícios para análise, não prova de erro. O diagnóstico <b>não altera</b> bases, CST ou TIPO_ITEM.</p>"));
        TOPICOS.put("ocorrencias",html("Ocorrências de leitura",
            "<p><b>O que faz:</b> exibe avisos e problemas encontrados durante a interpretação dos registros, como valores inválidos ou registros incompletos.</p>"+
            "<p><b>Como usar:</b> depois de abrir uma EFD, veja a tabela de ocorrências e localize as linhas indicadas no arquivo original.</p>"+
            "<p><b>Limite:</b> não substitui a validação completa do PVA nem uma auditoria tributária.</p>"));
        TOPICOS.put("arquivos",html("Importar, salvar e transmitir",
            "<p><b>Importação:</b> aceita .TXT e .SPED para EFD original (COD_FIN=0) ou substituta (COD_FIN=1).</p>"+
            "<p><b>Cópia fiel:</b> duplica o arquivo sem alterar o conteúdo e conserva COD_FIN. Alterar a extensão não valida os dados.</p>"+
            "<p><b>Correção assistida:</b> gera novo arquivo e relatório de alterações; o arquivo de origem é preservado.</p>"+
            "<p><b>Antes de transmitir:</b> reimporte o arquivo gerado, confira os saldos e campos e use o PVA oficial. A aplicação não assina nem transmite.</p>"));
        TOPICOS.put("consultas",html("Consultas e relatórios",
            "<p><b>C100:</b> consulte documentos por número, chave, período e participante. <b>C170:</b> pesquise itens e NCM. <b>0200:</b> veja o cadastro de produtos.</p>"+
            "<p><b>C190 / CFOP:</b> consulte os grupos escriturados e filtre por CFOP, CST e tipo de operação. <b>E110:</b> veja a apuração declarada.</p>"+
            "<p><b>Relatórios:</b> exporte CSV, Excel ou PDF descritivos. Os valores declarados não equivalem a uma validação jurídica independente.</p>"));
        TOPICOS.put("futuras",html("Ferramentas planejadas",
            "<p>O catálogo reserva espaço para auditorias independentes de substituição tributária, CIAP e outros problemas fiscais.</p>"+
            "<p><b>Planejada</b> significa que a função ainda não detecta nem corrige problemas. Os botões permanecem desabilitados.</p>"+
            "<p>Novos módulos deverão declarar seu alcance, entradas, regras de validação, alterações autorizadas e limitações.</p>"));
        TOPICOS.put("limites",html("Limites e responsabilidades",
            "<p>O Extrator SPED Fiscal é uma ferramenta local de consulta e correção assistida. É necessário verificar o regime fiscal, a destinação real dos bens, a legislação aplicável e a documentação de suporte.</p>"+
            "<p>Não há consulta automática à SEFAZ, assinatura digital, transmissão ou substituição do PVA. Uma importação e verificações internas bem-sucedidas não certificam a EFD.</p>"+
            "<p>Os módulos não devem executar alterações em situações não suportadas. Preserve sempre uma cópia do arquivo original.</p>"));
    }
    private static String html(String titulo,String conteudo){
        return "<html><head><style>body{font-family:'Segoe UI',Arial,sans-serif;font-size:13px;color:#243c58;margin:19px 23px;}"+
            "h2{color:#1d3f69;font-size:21px;margin:0 0 15px 0;}h3{color:#245b90;margin:15px 0 7px 0;}"+
            "p,li{line-height:1.55;}ol{margin-left:10px;}b{color:#214f7e;}</style></head><body>"+
            "<h2>"+titulo+"</h2>"+conteudo+"</body></html>";
    }
    public static String conteudo(String topico){return TOPICOS.containsKey(topico)?TOPICOS.get(topico):TOPICOS.get("inicio");}
    public static boolean contem(String topico){return TOPICOS.containsKey(topico);}
    public static String[] topicos(){return TOPICOS.keySet().toArray(new String[TOPICOS.size()]);}
}
