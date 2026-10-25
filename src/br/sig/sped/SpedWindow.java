package br.sig.sped;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.event.KeyEvent;

/** Desktop local: GUI inspirada no mockup aprovado, implementada inteiramente em Swing/Java 8. */
public final class SpedWindow extends JFrame {
    private static final long serialVersionUID = 2L;
    private static final String VERSAO="0.1.11";
    private static final DecimalFormat MOEDA;
    private static final DecimalFormat QUANTIDADE;
    static {
        DecimalFormatSymbols s=new DecimalFormatSymbols(new Locale("pt","BR"));
        MOEDA=new DecimalFormat("#,##0.00",s);
        QUANTIDADE=new DecimalFormat("#,##0.00000",s);
    }

    private SpedParser.Resultado resultado;
    private File pastaAnterior;
    private final JLabel status = UiKit.text("Pronto para importar EFD ICMS/IPI (.txt ou .sped).",11,false,FrameChrome.TEXT);
    private final JLabel statusIcon = new JLabel(FrameChrome.statusIcon("busy"));
    private final JLabel empresa = UiKit.text("Nenhum SPED carregado",16,true,UiKit.NAVY);
    private final JLabel cnpj = UiKit.text("—",14,true,UiKit.NAVY);
    private final JLabel uf = UiKit.text("—",14,true,UiKit.NAVY);
    private final JLabel periodo = UiKit.text("—",14,true,UiKit.NAVY);
    private final JLabel docCount=UiKit.text("—",28,true,UiKit.BLUE);
    private final JLabel c190Count=UiKit.text("—",28,true,UiKit.GREEN);
    private final JLabel e110Count=UiKit.text("—",28,true,UiKit.ORANGE);
    private final JLabel errCount=UiKit.text("—",28,true,UiKit.RED);
    private final JLabel situation=UiKit.text("Importe um arquivo para começar",12,true,UiKit.SECONDARY);
    private final JLabel fileInfo=UiKit.text("Arquivo: —",12,false,UiKit.NAVY);
    private final JLabel versionInfo=UiKit.text("Leiaute: —",12,false,UiKit.NAVY);
    private final JLabel charsetInfo=UiKit.text("Codificação: —",12,false,UiKit.NAVY);
    private final JLabel linesInfo=UiKit.text("Linhas lidas: —",12,false,UiKit.NAVY);
    private final JLabel countsInfo=UiKit.text("E100: —   |   E110: —   |   Linhas descartadas: —",12,false,UiKit.NAVY);
    private final JLabel filterStats=UiKit.text("Importe um SPED para consultar valores filtrados.",12,false,UiKit.NAVY);
    private final JLabel totalsC190=UiKit.text("Totais: —",12,true,UiKit.NAVY);
    private final JLabel totalsCfop=UiKit.text("Totais: —",12,true,UiKit.NAVY);
    private final JLabel countDocs=UiKit.caption("Nenhum documento carregado.");
    private final JLabel docFilterStats=UiKit.caption("Busque por numero, serie, chave, data ou participante.");
    private final JTextField docNumero=new JTextField();
    private final JTextField docSerie=new JTextField();
    private final JTextField docChave=new JTextField();
    private final JTextField docParticipante=new JTextField();
    private final JTextField docData=new JTextField();
    private final JComboBox<String> docOperacao=new JComboBox<String>(new String[]{"Todas","Entrada","Saída","Indefinida"});
    private final JButton docsDetailsButton=new UiKit.ActionButton("Ver detalhes",UiKit.icon("file",17,Color.WHITE),UiKit.BLUE,new Color(32,108,178),Color.WHITE);
    private final JLabel countE110=UiKit.caption("Nenhuma apuração carregada.");
    private final JLabel countOcorrencias=UiKit.caption("Nenhum arquivo carregado.");
    private final JLabel countC190=UiKit.caption("Nenhum registro carregado.");
    private final JLabel countCfop=UiKit.caption("Nenhum agrupamento carregado.");
    private final JButton openButton;
    private final JButton refreshButton;
    private final JButton exportButton;
    private final JTextField cfopFilter = new JTextField(9);
    private final JTextField cstFilter = new JTextField(8);
    private final JComboBox<String> opFilter = new JComboBox<String>(new String[]{"Todas", "Entrada", "Saída", "Indefinida"});
    private final JComboBox<String> cfopChoices = new JComboBox<String>(new String[]{"Todos os CFOPs"});
    private final JComboBox<String> cstChoices = new JComboBox<String>(new String[]{"Todos os CSTs"});
    private boolean preenchendoOpcoes = false;
    private final JPanel filterArea = new JPanel(new BorderLayout());
    private final JPanel centralCards = new JPanel(new CardLayout());
    private final Map<String,UiKit.NavButton> navButtons=new LinkedHashMap<String,UiKit.NavButton>();
    private String currentPage="overview";
    private final JTable tabNotas=table();
    private final JTable tabC190=table();
    private final JTable tabCfop=table();
    private final JTable tabE110=table();
    private final JTable tabOcorrencias=table();
    private final JTable tabRelatorios=table();
    private final JTable tabItens=table();
    private final JTable tabProdutos=table();
    private final JTable tabAuditoria=table();
    private final JLabel catalogStatus=UiKit.caption("Abra uma EFD para utilizar as ferramentas de auditoria.");
    private final java.util.List<JButton> acoesCatalogo=new java.util.ArrayList<JButton>();
    private final java.util.List<CatalogoAuditoria.Ferramenta> ferramentasCatalogo=new java.util.ArrayList<CatalogoAuditoria.Ferramenta>();
    private final JLabel diagnosticoTotais=UiKit.text("Nenhuma EFD carregada",13,true,UiKit.NAVY);
    private final JTextArea diagnosticoTexto=new JTextArea();
    private final javax.swing.JEditorPane helpText=new javax.swing.JEditorPane();
    private final java.util.Map<String,JButton> helpTopics=new LinkedHashMap<String,JButton>();
    private String helpTopic="inicio";

    private final JLabel auditCount=UiKit.caption("Importe um SPED para verificar créditos por uso e consumo.");
    private final JLabel auditTotal=UiKit.text("Crédito identificado: —",12,true,UiKit.NAVY);
    private final javax.swing.JCheckBox auditSubstituta=new javax.swing.JCheckBox("Preparar como arquivo substituto (registro 0000, COD_FIN=1)",true);
    private final JButton auditPreview=new UiKit.ActionButton("Prévia e salvar SPED/TXT",UiKit.icon("file",16,Color.WHITE),UiKit.BLUE,new Color(32,108,178),Color.WHITE);
    private final JButton auditSaveCopy=new JButton("Salvar cópia .SPED/.TXT");
    private List<CreditoUsoConsumo.Caso> casosAuditoria=new ArrayList<CreditoUsoConsumo.Caso>();
    private final JTextField itemTexto=new JTextField();
    private final JTextField itemNcm=new JTextField();
    private final JTextField itemCfop=new JTextField();
    private final JTextField itemCst=new JTextField();
    private final JComboBox<String> itemOperacao=new JComboBox<String>(new String[]{"Todas","Entrada","Saída","Indefinida"});
    private final JTextField produtoTexto=new JTextField();
    private final JTextField produtoNcm=new JTextField();
    private final JLabel countItens=UiKit.caption("Nenhum C170 carregado.");
    private final JLabel totalItens=UiKit.text("Valores C170: —",12,true,UiKit.NAVY);
    private final JLabel avisoItens=UiKit.caption("Sem C170 nao implica omissao: a exigibilidade depende do modelo e da situacao.");
    private final JLabel countProdutos=UiKit.caption("Nenhum 0200 carregado.");

    private final JLabel relatorioCount=UiKit.caption("Importe um SPED para gerar relatórios.");
    private final JLabel relatorioTotal=UiKit.text("Totais C190: —",12,true,UiKit.NAVY);
    private final JLabel relatorioAviso=UiKit.caption("Relatório descritivo, não constitui apuração nem validação fiscal.");
    private final JLabel relatorioKpi=UiKit.text("—",12,true,UiKit.NAVY);
    private JButton relatorioXlsxButton;
    private JButton relatorioPdfButton;
    private JButton tabelaXlsxButton;

    public SpedWindow() {
        super("Extrator SPED Fiscal - v"+VERSAO);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1350,850);
        setMinimumSize(new Dimension(990,620));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UiKit.BG);
        setIconImage(FrameChrome.appIcon());
        openButton=FrameChrome.toolbarButton("Abrir SPED (.txt/.sped)","folder","Abrir SPED","TXT/SPED");
        refreshButton=FrameChrome.toolbarButton("Atualizar consulta","refresh","Atualizar","consulta");
        exportButton=FrameChrome.toolbarButton("Exportar CSV","download","Exportar","CSV");
        construir();
        setHasData(false);
        instalarAtalhos();
    }
    private void construir() {
        setJMenuBar(menuBar());
        JPanel root=new JPanel(new BorderLayout());root.setBackground(UiKit.BG);
        root.add(ribbon(),BorderLayout.NORTH);
        JPanel workspace=new JPanel(new BorderLayout());workspace.setBackground(UiKit.BG);
        workspace.add(sidebar(),BorderLayout.WEST);
        JPanel center=new JPanel(new BorderLayout(0,0));center.setBackground(UiKit.BG);
        JPanel header=new JPanel();header.setOpaque(false);header.setLayout(new BoxLayout(header,BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(14,16,12,16));
        JPanel c=contexto();c.setAlignmentX(Component.LEFT_ALIGNMENT);header.add(c);
        header.add(Box.createVerticalStrut(10));
        filterArea.setOpaque(false);filterArea.add(filtros(),BorderLayout.CENTER);
        filterArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(filterArea);filterArea.setVisible(false);
        center.add(header,BorderLayout.NORTH);
        centralCards.setOpaque(false);
        centralCards.add(overview(),"overview");
        centralCards.add(documentosPage(),"docs");
        centralCards.add(itensPage(),"items");
        centralCards.add(produtosPage(),"products");
        centralCards.add(dataPage("Registros analíticos", "C190: linhas efetivamente exibidas respeitam os filtros CFOP, CST e operação.", "analysis", tabC190, countC190, totalsC190),"analytic");
        centralCards.add(dataPage("Consolidação por CFOP", "Valores extraídos de C190, agrupados por operação + CFOP + CST. Não constituem apuração.", "grid", tabCfop, countCfop, totalsCfop),"cfop");
        centralCards.add(dataPage("Apuração declarada de ICMS", "E110: valores transmitidos no SPED. Sem recálculo dos impostos.", "calculator", tabE110, countE110, null),"e110");
        centralCards.add(relatoriosPage(),"reports");
        centralCards.add(catalogoPage(),"audit");
        centralCards.add(auditoriaPage(),"audit-credit");
        centralCards.add(diagnosticoPage(),"audit-diagnostic");
        centralCards.add(ajudaPage(),"help");
        centralCards.add(dataPage("Ocorrências de leitura", "Erros de estrutura, campos numéricos inválidos e avisos encontrados na importação.", "alert", tabOcorrencias, countOcorrencias, null),"issues");
        center.add(centralCards,BorderLayout.CENTER);
        workspace.add(center,BorderLayout.CENTER);
        root.add(workspace,BorderLayout.CENTER);
        root.add(statusBar(),BorderLayout.SOUTH);
        setContentPane(root);
        openButton.addActionListener(e -> abrir());
        refreshButton.addActionListener(e -> preencher());
        exportButton.addActionListener(e -> exportar());
        instalarFiltros();
        instalarConsultaDocumentos();
        instalarConsultaItens();
        mostrar("overview");
    }
    private JMenuBar menuBar() {
        JMenuBar m=new JMenuBar();
        JMenu arquivo=new JMenu("Arquivo");
        menuItem(arquivo,"Abrir SPED (Ctrl+O)",()->abrir());
        menuItem(arquivo,"Salvar cópia da EFD (.SPED ou .TXT)",()->salvarCopiaEfd());
        arquivo.addSeparator();menuItem(arquivo,"Sair",()->dispose());m.add(arquivo);
        JMenu consulta=new JMenu("Consulta");
        menuItem(consulta,"Notas C100 — consulta avançada",()->mostrar("docs"));
        menuItem(consulta,"Detalhes da nota selecionada (Enter)",()->abrirDetalhes());
        consulta.addSeparator();
        menuItem(consulta,"Itens C170 — pesquisa de mercadorias",()->mostrar("items"));
        menuItem(consulta,"Cadastro de itens 0200",()->mostrar("products"));
        menuItem(consulta,"Analítico C190",()->mostrar("analytic"));
        menuItem(consulta,"Por CFOP",()->mostrar("cfop"));
        menuItem(consulta,"Limpar filtros",()->limparFiltros());m.add(consulta);
        JMenu exportacao=new JMenu("Exportação");
        menuItem(exportacao,"Exportar CSV da tabela atual (Ctrl+E)",()->exportar());
        menuItem(exportacao,"Exportar XLSX da tabela atual",()->exportarTabelaXlsx());
        exportacao.addSeparator();
        menuItem(exportacao,"Relatório consolidado em Excel (.xlsx)",()->exportarRelatorioXlsx());
        menuItem(exportacao,"Relatório consolidado em PDF",()->exportarRelatorioPdf());
        exportacao.addSeparator();
        menuItem(exportacao,"Salvar cópia do SPED carregado (.SPED/.TXT)",()->salvarCopiaEfd());
        m.add(exportacao);
        JMenu ferramentas=new JMenu("Ferramentas");
        menuItem(ferramentas,"Atualizar consulta (F5)",()->preencher());
        menuItem(ferramentas,"Limpar filtros",()->limparFiltros());
        menuItem(ferramentas,"Catálogo de Auditoria Fiscal",()->mostrar("audit"));
        menuItem(ferramentas,"Correção de créditos (MG-RN07)",()->mostrar("audit-credit"));
        menuItem(ferramentas,"Diagnóstico complementar (bases/CST/0200)",()->mostrar("audit-diagnostic"));
        m.add(ferramentas);
        JMenu ajuda=new JMenu("Ajuda");
        menuItem(ajuda,"Manual do sistema",()->ajuda());
        menuItem(ajuda,"Correção de créditos RN07",()->abrirAjuda("creditos"));
        menuItem(ajuda,"Limites da aplicação",()->abrirAjuda("limites"));m.add(ajuda);
        return FrameChrome.decorateMenu(m);
    }
    private void menuItem(JMenu m,String s,Runnable r) {
        JMenuItem i=new JMenuItem(s);i.setFont(UiKit.font(12,false));i.addActionListener(e->r.run());m.add(i);
    }
    private void instalarAtalhos() {
        JComponent p=(JComponent)getContentPane();
        addShortcut(p,"open",KeyStroke.getKeyStroke(KeyEvent.VK_O,KeyEvent.CTRL_DOWN_MASK),()->abrir());
        addShortcut(p,"export",KeyStroke.getKeyStroke(KeyEvent.VK_E,KeyEvent.CTRL_DOWN_MASK),()->exportar());
        addShortcut(p,"refresh",KeyStroke.getKeyStroke(KeyEvent.VK_F5,0),()->preencher());
        addShortcut(p,"help",KeyStroke.getKeyStroke(KeyEvent.VK_F1,0),()->ajuda());
    }
    private void addShortcut(JComponent panel,String key,KeyStroke stroke,Runnable r) {
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke,key);
        panel.getActionMap().put(key,new AbstractAction(){public void actionPerformed(ActionEvent e){r.run();}});
    }
    private JPanel ribbon() {
        JButton helpButton=FrameChrome.toolbarButton("Ajuda","help","Ajuda","");
        helpButton.addActionListener(e->ajuda());
        return FrameChrome.ribbon(openButton,refreshButton,exportButton,helpButton);
    }
    private JPanel sidebar() {
        UiKit.GradientPanel outer=new UiKit.GradientPanel(new Color(40,82,122),new Color(25,56,91),0);
        outer.setLayout(new BorderLayout());outer.setPreferredSize(new Dimension(220,10));
        JPanel nav=new JPanel();nav.setOpaque(false);nav.setLayout(new BoxLayout(nav,BoxLayout.Y_AXIS));
        nav.setBorder(BorderFactory.createEmptyBorder(22,6,0,6));
        JLabel title=UiKit.text("NAVEGAÇÃO",11,true,new Color(160,193,225));title.setBorder(BorderFactory.createEmptyBorder(0,19,13,0));
        nav.add(title);
        nav(nav,"overview","Visão Geral","home");
        nav(nav,"docs","Notas - C100","file");
        nav(nav,"items","Itens - C170","grid");
        nav(nav,"products","Produtos - 0200","database");
        nav(nav,"analytic","Analítico - C190","analysis");
        nav(nav,"cfop","Por CFOP","grid");
        nav(nav,"e110","Apuração - E110","calculator");
        nav(nav,"audit","Auditoria Fiscal","alert");
        nav(nav,"help","Ajuda","info");
        nav(nav,"reports","Relatórios","analysis");
        nav(nav,"issues","Ocorrências","alert");
        JScrollPane navScroll=new JScrollPane(nav);
        navScroll.setBorder(null);navScroll.setOpaque(false);navScroll.getViewport().setOpaque(false);
        navScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        navScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        navScroll.getVerticalScrollBar().setUnitIncrement(18);
        outer.add(navScroll,BorderLayout.CENTER);
        JPanel footer=new JPanel();footer.setOpaque(false);footer.setBorder(BorderFactory.createEmptyBorder(14,18,20,10));
        footer.setLayout(new BoxLayout(footer,BoxLayout.Y_AXIS));
        footer.add(UiKit.text("LEITURA LOCAL",11,true,new Color(171,220,252)));
        footer.add(Box.createVerticalStrut(6));
        footer.add(UiKit.text("Seu arquivo não é enviado",11,false,new Color(209,229,246)));
        footer.add(UiKit.text("e não é modificado.",11,false,new Color(209,229,246)));
        outer.add(footer,BorderLayout.SOUTH);
        return outer;
    }
    private void nav(JPanel panel,String id,String label,String type) {
        UiKit.NavButton n=new UiKit.NavButton(label,type);n.setAlignmentX(Component.LEFT_ALIGNMENT);
        navButtons.put(id,n);panel.add(n);panel.add(Box.createVerticalStrut(4));n.addActionListener(e->mostrar(id));
    }
    private JPanel contexto() {
        UiKit.Panel p=new UiKit.Panel(UiKit.WHITE,UiKit.BORDER,16);
        p.setLayout(new BorderLayout(0,10));p.setBorder(BorderFactory.createEmptyBorder(14,18,16,18));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE,120));
        JPanel title=new JPanel(new BorderLayout());title.setOpaque(false);
        JLabel t=UiKit.text("  Contexto da consulta",16,true,UiKit.NAVY);
        t.setIcon(UiKit.icon("building",22,UiKit.BLUE));title.add(t,BorderLayout.WEST);
        JLabel badge=UiKit.caption("EFD ICMS/IPI  •  Leitura informativa");title.add(badge,BorderLayout.EAST);
        p.add(title,BorderLayout.NORTH);
        JPanel values=new JPanel(new GridLayout(1,4,12,0));values.setOpaque(false);
        values.add(field("EMPRESA",empresa));values.add(field("CNPJ",cnpj));values.add(field("UF",uf));values.add(field("PERÍODO",periodo));
        p.add(values,BorderLayout.CENTER);
        return p;
    }
    private JPanel field(String heading,JLabel value) {
        JPanel p=new JPanel();p.setOpaque(false);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.add(UiKit.text(heading,11,false,UiKit.SECONDARY));p.add(Box.createVerticalStrut(5));
        value.setMaximumSize(new Dimension(Integer.MAX_VALUE,28));
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(value);value.setToolTipText(value.getText());
        return p;
    }
    private JPanel filtros() {
        UiKit.Panel p=new UiKit.Panel(UiKit.WHITE,UiKit.BORDER,14);
        p.setBorder(BorderFactory.createEmptyBorder(10,14,10,14));
        p.setLayout(new BorderLayout(12,8));
        JPanel l=new JPanel(new FlowLayout(FlowLayout.LEFT,12,0));l.setOpaque(false);
        JLabel f=UiKit.text("Filtros",13,true,UiKit.NAVY);f.setIcon(UiKit.icon("filter",17,UiKit.BLUE));l.add(f);
        l.add(UiKit.caption("Operação:"));
        opFilter.setFont(UiKit.font(12,false));opFilter.setPreferredSize(new Dimension(124,32));l.add(opFilter);
        l.add(UiKit.caption("CFOP:"));styleChoice(cfopChoices,140);l.add(cfopChoices);
        l.add(UiKit.caption("CST:"));styleChoice(cstChoices,112);l.add(cstChoices);
        l.add(UiKit.caption("Buscar CFOP:"));styleInput(cfopFilter);l.add(cfopFilter);
        l.add(UiKit.caption("Buscar CST:"));styleInput(cstFilter);l.add(cstFilter);
        JButton clean=new UiKit.ActionButton("Limpar",UiKit.icon("close",14,UiKit.NAVY),UiKit.SKY,new Color(217,234,251),UiKit.NAVY);
        clean.setIcon(UiKit.icon("close",14,UiKit.NAVY));clean.addActionListener(e->limparFiltros());l.add(clean);
        p.add(l,BorderLayout.CENTER);p.add(filterStats,BorderLayout.SOUTH);
        return p;
    }
    private static void styleChoice(JComboBox<String> combo, int width) {
        combo.setFont(UiKit.font(12,false));
        combo.setPreferredSize(new Dimension(width,32));
        combo.setBackground(Color.WHITE);
    }
    private void atualizarOpcoes() {
        if (resultado==null) return;
        preenchendoOpcoes=true;
        try {
            java.util.Set<String> cfops = new java.util.TreeSet<String>();
            java.util.Set<String> csts = new java.util.TreeSet<String>();
            for (SpedParser.Analitico a : resultado.analiticos) {
                if (a.cfop!=null && !a.cfop.isEmpty()) cfops.add(a.cfop);
                if (a.cst!=null && !a.cst.isEmpty()) csts.add(a.cst);
            }
            cfopChoices.removeAllItems();cfopChoices.addItem("Todos os CFOPs");
            for (String code : cfops) cfopChoices.addItem(code);
            cstChoices.removeAllItems();cstChoices.addItem("Todos os CSTs");
            for (String code : csts) cstChoices.addItem(code);
        } finally { preenchendoOpcoes=false; }
    }
    private static void styleInput(JTextField txt) {
        txt.setFont(UiKit.font(13,false));txt.setPreferredSize(new Dimension(92,32));
        txt.setBackground(new Color(250,252,255));txt.setForeground(UiKit.NAVY);
        txt.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiKit.BORDER,1,true),BorderFactory.createEmptyBorder(5,8,5,8)));
    }
    private JPanel overview() {
        JPanel whole=new JPanel();whole.setBackground(UiKit.BG);whole.setLayout(new BoxLayout(whole,BoxLayout.Y_AXIS));
        whole.setBorder(BorderFactory.createEmptyBorder(0,16,20,16));
        UiKit.Panel main=new UiKit.Panel(UiKit.WHITE,UiKit.BORDER,16);main.setLayout(new BorderLayout(0,12));
        main.setBorder(BorderFactory.createEmptyBorder(17,16,15,16));
        JPanel title=new JPanel(new BorderLayout());title.setOpaque(false);
        JLabel heading=UiKit.text("  Resumo da extração",17,true,UiKit.NAVY);heading.setIcon(UiKit.icon("chart",22,UiKit.BLUE));
        title.add(heading,BorderLayout.WEST);title.add(situation,BorderLayout.EAST);main.add(title,BorderLayout.NORTH);
        JPanel cards=new JPanel(new GridLayout(1,4,12,0));cards.setOpaque(false);
        cards.add(stat("Documentos C100",docCount,"Documentos encontrados","file",new Color(237,246,255),new Color(194,224,253),UiKit.BLUE));
        cards.add(stat("Registros C190",c190Count,"Registros analíticos","chart",new Color(235,250,246),new Color(190,234,219),UiKit.GREEN));
        cards.add(stat("Registros E110",e110Count,"E110 declarados no SPED","calculator",new Color(255,247,236),new Color(248,222,182),UiKit.ORANGE));
        cards.add(stat("Ocorrências",errCount,"Alertas de leitura","alert",new Color(255,244,245),new Color(250,213,215),UiKit.RED));
        main.add(cards,BorderLayout.CENTER);
        whole.add(main);whole.add(Box.createVerticalStrut(14));
        UiKit.Panel info=new UiKit.Panel(UiKit.WHITE,UiKit.BORDER,16);info.setLayout(new BorderLayout(0,11));
        info.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        JLabel titleInfo=UiKit.text("  Informações do arquivo",16,true,UiKit.NAVY);titleInfo.setIcon(UiKit.icon("info",19,UiKit.BLUE));
        info.add(titleInfo,BorderLayout.NORTH);
        JPanel list=new JPanel();list.setOpaque(false);list.setLayout(new BoxLayout(list,BoxLayout.Y_AXIS));
        for (JLabel l:new JLabel[]{fileInfo,versionInfo,charsetInfo,linesInfo,countsInfo}) {
            l.setBorder(BorderFactory.createEmptyBorder(6,9,6,6));l.setAlignmentX(Component.LEFT_ALIGNMENT);
            list.add(l);
        }
        info.add(list,BorderLayout.CENTER);
        whole.add(info);whole.add(Box.createVerticalStrut(14));
        UiKit.Panel note=new UiKit.Panel(new Color(226,241,255),new Color(193,223,249),16);
        note.setLayout(new BorderLayout(12,0));note.setBorder(BorderFactory.createEmptyBorder(13,15,14,15));
        JLabel icon=new JLabel(UiKit.icon("info",25,UiKit.BLUE));icon.setVerticalAlignment(SwingConstants.TOP);note.add(icon,BorderLayout.WEST);
        JPanel txt=new JPanel();txt.setOpaque(false);txt.setLayout(new BoxLayout(txt,BoxLayout.Y_AXIS));
        txt.add(UiKit.text("Limites importantes",13,true,UiKit.NAVY));txt.add(Box.createVerticalStrut(5));
        for(String line: new String[]{
          "Consulta local aos dados declarados no SPED; sem validação jurídica e sem consulta à SEFAZ.",
          "C100 (documento) e C190 (detalhamento) não devem ser somados entre si.",
          "Filtros C190 nao alteram itens C170. A area Itens tem filtros proprios e cadastro 0200.",
          "Ausencia de C170 nao prova omissao: existem dispensas para modelos e situacoes fiscais.",
          "C190 não equivale à apuração: E110 mostra os valores declarados, sem recálculo.",
          "Valores inválidos são excluídos dos totais daquele campo e indicados nas ocorrências.",
          "O extrator NÃO substitui o PVA EFD ICMS/IPI."}) {
            JLabel b=UiKit.text("•  "+line,12,false,UiKit.NAVY);b.setAlignmentX(Component.LEFT_ALIGNMENT);txt.add(b);txt.add(Box.createVerticalStrut(5));
        }
        note.add(txt,BorderLayout.CENTER);whole.add(note);
        JScrollPane scroll=new JScrollPane(whole);scroll.setBorder(null);scroll.getVerticalScrollBar().setUnitIncrement(18);scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return container(scroll);
    }
    private JPanel stat(String label,JLabel value,String desc,String icon,Color bg,Color edge,Color tint) {
        UiKit.Panel panel=new UiKit.Panel(bg,edge,14);panel.setBorder(BorderFactory.createEmptyBorder(17,16,13,12));
        panel.setLayout(new BorderLayout(7,2));panel.setPreferredSize(new Dimension(180,150));
        JPanel iconHolder=new JPanel(new java.awt.GridBagLayout());
        iconHolder.setOpaque(false);
        iconHolder.add(UiKit.iconTile(icon,29,tint));
        panel.add(iconHolder,BorderLayout.WEST);
        JPanel inner=new JPanel();inner.setOpaque(false);inner.setLayout(new BoxLayout(inner,BoxLayout.Y_AXIS));
        JLabel lbl=UiKit.text(label,13,true,UiKit.NAVY);
        inner.add(lbl);inner.add(Box.createVerticalStrut(10));inner.add(value);inner.add(Box.createVerticalStrut(6));
        inner.add(UiKit.text(desc,11,false,UiKit.SECONDARY));
        panel.add(inner,BorderLayout.CENTER);return panel;
    }
    private JPanel dataPage(String title,String desc,String icon,JTable table,JLabel count,JLabel totals) {
        JPanel page=new JPanel(new BorderLayout(0,10));page.setBackground(UiKit.BG);
        page.setBorder(BorderFactory.createEmptyBorder(0,16,16,16));
        UiKit.Panel panel=new UiKit.Panel(UiKit.WHITE,UiKit.BORDER,16);
        panel.setLayout(new BorderLayout(0,12));panel.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        JPanel top=new JPanel(new BorderLayout());top.setOpaque(false);
        JPanel titlePanel=new JPanel();titlePanel.setOpaque(false);titlePanel.setLayout(new BoxLayout(titlePanel,BoxLayout.Y_AXIS));
        JLabel h=UiKit.text("  "+title,16,true,UiKit.NAVY);h.setIcon(UiKit.icon(icon,20,UiKit.BLUE));
        titlePanel.add(h);titlePanel.add(Box.createVerticalStrut(5));titlePanel.add(UiKit.caption(desc));
        top.add(titlePanel,BorderLayout.WEST);
        panel.add(top,BorderLayout.NORTH);
        JScrollPane sc=new JScrollPane(table);sc.getVerticalScrollBar().setUnitIncrement(20);
        sc.setBorder(BorderFactory.createLineBorder(UiKit.BORDER));
        sc.setBackground(UiKit.WHITE);sc.getViewport().setBackground(UiKit.WHITE);
        panel.add(sc,BorderLayout.CENTER);
        JPanel bottom=new JPanel(new BorderLayout());bottom.setOpaque(false);
        bottom.add(count,BorderLayout.WEST);if(totals!=null) bottom.add(totals,BorderLayout.EAST);
        panel.add(bottom,BorderLayout.SOUTH);
        page.add(panel,BorderLayout.CENTER);return page;
    }
    /** Painel de saída: apresenta o mesmo conjunto C190 filtrado, separado dos dados brutos C100/E110. */
    private JPanel relatoriosPage() {
        JPanel page=new JPanel(new BorderLayout(0,11));page.setBackground(UiKit.BG);
        page.setBorder(BorderFactory.createEmptyBorder(0,16,16,16));
        UiKit.Panel panel=new UiKit.Panel(UiKit.WHITE,UiKit.BORDER,16);
        panel.setLayout(new BorderLayout(0,11));panel.setBorder(BorderFactory.createEmptyBorder(15,16,15,16));
        JPanel upper=new JPanel();upper.setOpaque(false);upper.setLayout(new BoxLayout(upper,BoxLayout.Y_AXIS));
        JLabel title=UiKit.text("  Relatórios e exportação",18,true,UiKit.NAVY);
        title.setIcon(UiKit.icon("analysis",23,UiKit.BLUE));title.setAlignmentX(Component.LEFT_ALIGNMENT);
        upper.add(title);upper.add(Box.createVerticalStrut(7));
        JLabel sub=UiKit.caption("Demonstrativos de valores escriturados — mantenha CFOP, CST e operação ajustados no filtro acima.");
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);upper.add(sub);upper.add(Box.createVerticalStrut(11));
        relatorioKpi.setAlignmentX(Component.LEFT_ALIGNMENT);upper.add(relatorioKpi);upper.add(Box.createVerticalStrut(12));
        JPanel buttons=new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT,10,0));buttons.setOpaque(false);
        relatorioXlsxButton=new UiKit.ActionButton("Excel consolidado (.xlsx)",UiKit.icon("grid",19,Color.WHITE),UiKit.GREEN,new Color(16,123,90),Color.WHITE);
        relatorioPdfButton=new UiKit.ActionButton("Relatório PDF",UiKit.icon("file",19,Color.WHITE),UiKit.BLUE,new Color(29,106,176),Color.WHITE);
        tabelaXlsxButton=new UiKit.ActionButton("Excel da tabela visível",UiKit.icon("analysis",19,Color.WHITE),UiKit.NAVY,UiKit.DEEP,Color.WHITE);
        buttons.add(relatorioXlsxButton);buttons.add(relatorioPdfButton);buttons.add(tabelaXlsxButton);
        relatorioXlsxButton.addActionListener(e->exportarRelatorioXlsx());
        relatorioPdfButton.addActionListener(e->exportarRelatorioPdf());
        tabelaXlsxButton.addActionListener(e->exportarTabelaXlsx());
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);upper.add(buttons);
        upper.add(Box.createVerticalStrut(10));
        JLabel caption=UiKit.caption("O XLSX consolidado inclui Resumo, CFOP/CST, C100 por operação e E110; o PDF contém a síntese e o detalhamento paginado.");
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);upper.add(caption);
        panel.add(upper,BorderLayout.NORTH);
        JScrollPane scroll=new JScrollPane(tabRelatorios);scroll.setBorder(BorderFactory.createLineBorder(UiKit.BORDER));
        scroll.getVerticalScrollBar().setUnitIncrement(18);panel.add(scroll,BorderLayout.CENTER);
        JPanel foot=new JPanel(new BorderLayout(0,5));foot.setOpaque(false);
        JPanel line=new JPanel(new BorderLayout());line.setOpaque(false);line.add(relatorioCount,BorderLayout.WEST);line.add(relatorioTotal,BorderLayout.EAST);
        foot.add(line,BorderLayout.NORTH);foot.add(relatorioAviso,BorderLayout.SOUTH);panel.add(foot,BorderLayout.SOUTH);
        page.add(panel,BorderLayout.CENTER);return page;
    }

    /** Consulta C170: filtros independentes da apuracao e dos analiticos C190. */
    private JPanel itensPage() {
        JPanel page=new JPanel(new BorderLayout(0,10));page.setBackground(UiKit.BG);
        page.add(filtrosItens(),BorderLayout.NORTH);
        JPanel content=dataPage("Análise de itens • C170", "Produtos declarados no C170, associados ao C100 e ao 0200. Não confundir com todos os itens do XML.",
                "grid",tabItens,countItens,totalItens);
        content.setBorder(BorderFactory.createEmptyBorder(0,16,8,16));
        JPanel body=new JPanel(new BorderLayout(0,4));body.setOpaque(false);
        body.add(content,BorderLayout.CENTER);
        JPanel bottom=new JPanel(new BorderLayout());bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(0,20,10,20));bottom.add(avisoItens,BorderLayout.WEST);
        body.add(bottom,BorderLayout.SOUTH);page.add(body,BorderLayout.CENTER);
        return page;
    }
    private JPanel filtrosItens() {
        JPanel wrapper=new JPanel(new BorderLayout());wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0,16,0,16));
        UiKit.Panel panel=new UiKit.Panel(UiKit.WHITE,UiKit.BORDER,16);
        panel.setLayout(new BorderLayout(0,9));panel.setBorder(BorderFactory.createEmptyBorder(12,15,12,15));
        JLabel title=UiKit.text("  Localizar itens da EFD",14,true,UiKit.NAVY);
        title.setIcon(UiKit.icon("filter",18,UiKit.BLUE));panel.add(title,BorderLayout.NORTH);
        JPanel fields=new JPanel(new GridLayout(1,5,10,0));fields.setOpaque(false);
        fields.add(docField("Código / descrição",itemTexto,"Busca por COD_ITEM, DESCR_ITEM do 0200 ou DESCR_COMPL"));
        fields.add(docField("NCM",itemNcm,"Busca parcial pelo NCM cadastrado no 0200"));
        fields.add(docField("CFOP",itemCfop,"CFOP informado especificamente no registro C170"));
        fields.add(docField("CST ICMS",itemCst,"CST do C170"));
        JPanel operation=new JPanel(new BorderLayout(0,5));operation.setOpaque(false);
        operation.add(UiKit.caption("Operação"),BorderLayout.NORTH);
        styleChoice(itemOperacao,125);operation.add(itemOperacao,BorderLayout.CENTER);fields.add(operation);
        panel.add(fields,BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));actions.setOpaque(false);
        JButton clear=new UiKit.ActionButton("Limpar filtros de itens",UiKit.icon("close",14,UiKit.NAVY),UiKit.SKY,new Color(217,234,251),UiKit.NAVY);
        clear.addActionListener(e->limparBuscaItens());actions.add(clear);panel.add(actions,BorderLayout.SOUTH);
        wrapper.add(panel,BorderLayout.CENTER);return wrapper;
    }
    private JPanel produtosPage() {
        JPanel page=new JPanel(new BorderLayout(0,10));page.setBackground(UiKit.BG);
        JPanel top=new JPanel(new BorderLayout());top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(0,16,0,16));
        UiKit.Panel panel=new UiKit.Panel(UiKit.WHITE,UiKit.BORDER,16);
        panel.setLayout(new BorderLayout(0,9));panel.setBorder(BorderFactory.createEmptyBorder(12,15,12,15));
        JLabel title=UiKit.text("  Catálogo declarado no registro 0200",14,true,UiKit.NAVY);
        title.setIcon(UiKit.icon("database",18,UiKit.BLUE));panel.add(title,BorderLayout.NORTH);
        JPanel fields=new JPanel(new GridLayout(1,2,12,0));fields.setOpaque(false);
        fields.add(docField("Código ou descrição",produtoTexto,"Pesquise o COD_ITEM e a DESCR_ITEM"));
        fields.add(docField("NCM",produtoNcm,"Busca parcial; NCM em branco permanece em branco"));
        panel.add(fields,BorderLayout.CENTER);
        JButton clear=new UiKit.ActionButton("Limpar pesquisa",UiKit.icon("close",14,UiKit.NAVY),UiKit.SKY,new Color(217,234,251),UiKit.NAVY);
        clear.addActionListener(e->{produtoTexto.setText("");produtoNcm.setText("");});
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));buttons.setOpaque(false);buttons.add(clear);panel.add(buttons,BorderLayout.SOUTH);
        top.add(panel,BorderLayout.CENTER);page.add(top,BorderLayout.NORTH);
        JPanel content=dataPage("Cadastro de mercadorias e serviços • 0200",
            "Referência de códigos internos do declarante. NCM e unidade podem não constar, conforme a operação e o leiaute.",
            "database",tabProdutos,countProdutos,null);
        page.add(content,BorderLayout.CENTER);return page;
    }
    private void instalarConsultaItens() {
        DocumentListener l=new DocumentListener(){
            public void insertUpdate(DocumentEvent e){filtrarItens();}
            public void removeUpdate(DocumentEvent e){filtrarItens();}
            public void changedUpdate(DocumentEvent e){filtrarItens();}
        };
        for(JTextField f:new JTextField[]{itemTexto,itemNcm,itemCfop,itemCst})f.getDocument().addDocumentListener(l);
        itemOperacao.addActionListener(e->filtrarItens());
        DocumentListener p=new DocumentListener(){
            public void insertUpdate(DocumentEvent e){filtrarProdutos();}
            public void removeUpdate(DocumentEvent e){filtrarProdutos();}
            public void changedUpdate(DocumentEvent e){filtrarProdutos();}
        };
        produtoTexto.getDocument().addDocumentListener(p);produtoNcm.getDocument().addDocumentListener(p);
    }
    private ItemConsulta.Filtro filtroItem(){
        return new ItemConsulta.Filtro(itemTexto.getText(),itemNcm.getText(),itemCfop.getText(),itemCst.getText(),
                (String)itemOperacao.getSelectedItem());
    }
    private void limparBuscaItens(){
        itemTexto.setText("");itemNcm.setText("");itemCfop.setText("");itemCst.setText("");
        itemOperacao.setSelectedIndex(0);filtrarItens();
    }
    private void filtrarItens() {
        if(resultado==null||tabItens.getModel().getColumnCount()<15)return;
        final ItemConsulta.Filtro filtro=filtroItem();
        final List<SpedParser.Item> itens=resultado.itens;
        @SuppressWarnings("unchecked")
        TableRowSorter<DefaultTableModel> sorter=(TableRowSorter<DefaultTableModel>)tabItens.getRowSorter();
        if(sorter==null)return;
        sorter.setRowFilter(new RowFilter<DefaultTableModel,Integer>(){
            @Override public boolean include(Entry<? extends DefaultTableModel,? extends Integer> e){
                return ItemConsulta.corresponde(itens.get(e.getIdentifier()),filtro);
            }
        });
        ItemConsulta.Totais t=ItemConsulta.totalizar(ItemConsulta.filtrar(itens,filtro));
        countItens.setText(fmtCount(t.registros)+" de "+fmtCount(itens.size())+" C170 • "+t.semCadastro+" sem 0200 no resultado"+
            (t.camposInvalidos>0?" • "+t.camposInvalidos+" com numeros invalidos":"")+(t.semCamposPrincipais>0?" • "+t.semCamposPrincipais+" sem quantidade/valor":""));
        totalItens.setText("VL_ITEM R$ "+money(t.valor)+"  |  ICMS R$ "+money(t.icms)+"  |  IPI R$ "+money(t.ipi));
        avisoItens.setText("Documentos C100 sem C170: "+ItemConsulta.documentosSemItens(resultado)+
            " (contagem informativa, NAO evidencia de falta de escrituraçao). Valores declarados; nao somar C170 e C190.");
    }
    private void filtrarProdutos() {
        if(resultado==null||tabProdutos.getModel().getColumnCount()<6)return;
        final String q=produtoTexto.getText(),ncm=produtoNcm.getText();
        final List<SpedParser.Produto> produtos=new ArrayList<SpedParser.Produto>(resultado.produtos.values());
        @SuppressWarnings("unchecked")
        TableRowSorter<DefaultTableModel> sorter=(TableRowSorter<DefaultTableModel>)tabProdutos.getRowSorter();
        if(sorter==null)return;
        sorter.setRowFilter(new RowFilter<DefaultTableModel,Integer>() {
            @Override public boolean include(Entry<? extends DefaultTableModel,? extends Integer> e){
                return ItemConsulta.corresponde(produtos.get(e.getIdentifier()),q,ncm);
            }
        });
        countProdutos.setText(fmtCount(tabProdutos.getRowCount())+" de "+fmtCount(produtos.size())+" cadastros 0200 exibidos.");
    }

    /** Barra dedicada a C100: independente dos filtros da analise C190. */
    private JPanel documentosPage() {
        JPanel page=new JPanel(new BorderLayout(0,10));page.setBackground(UiKit.BG);
        JPanel body=dataPage("Consulta avançada de documentos", "C100: registros declarados. Clique duas vezes em uma linha para abrir o detalhamento e os C190 filhos.", "file", tabNotas, countDocs, null);
        body.setBorder(BorderFactory.createEmptyBorder(0,16,12,16));
        page.add(buscaDocumentos(),BorderLayout.NORTH);
        page.add(body,BorderLayout.CENTER);
        return page;
    }
    private JPanel buscaDocumentos() {
        JPanel wrapper=new JPanel(new BorderLayout());wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0,16,0,16));
        UiKit.Panel panel=new UiKit.Panel(UiKit.WHITE,UiKit.BORDER,16);
        panel.setBorder(BorderFactory.createEmptyBorder(13,16,12,16));
        panel.setLayout(new BorderLayout(0,10));
        JLabel title=UiKit.text("  Localizar documento",14,true,UiKit.NAVY);
        title.setIcon(UiKit.icon("filter",18,UiKit.BLUE));
        panel.add(title,BorderLayout.NORTH);
        JPanel fields=new JPanel(new GridLayout(2,3,12,8));fields.setOpaque(false);
        fields.add(docField("Número da nota",docNumero,"Busca parcial pelo NUM_DOC"));
        fields.add(docField("Série",docSerie,"Busca pela SER do C100"));
        fields.add(docField("Chave da NF-e",docChave,"Busca parcial pela CHV_NFE, se informada"));
        fields.add(docField("Participante / CNPJ / CPF",docParticipante,"Nome e identificador do cadastro 0150, ou COD_PART"));
        fields.add(docField("Data de emissão",docData,"DD/MM/AAAA, DDMMAAAA ou parte da data"));
        JPanel operation=new JPanel(new BorderLayout(0,5));operation.setOpaque(false);
        operation.add(UiKit.caption("Operação"),BorderLayout.NORTH);
        styleChoice(docOperacao,150);
        operation.add(docOperacao,BorderLayout.CENTER);
        fields.add(operation);
        panel.add(fields,BorderLayout.CENTER);
        JPanel actions=new JPanel(new BorderLayout(12,0));actions.setOpaque(false);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));buttons.setOpaque(false);
        JButton clear=new UiKit.ActionButton("Limpar busca",UiKit.icon("close",15,UiKit.NAVY),UiKit.SKY,new Color(217,234,251),UiKit.NAVY);
        clear.addActionListener(e->limparBuscaDocs());buttons.add(clear);
        docsDetailsButton.addActionListener(e->abrirDetalhes());
        docsDetailsButton.setEnabled(false);buttons.add(docsDetailsButton);
        actions.add(buttons,BorderLayout.WEST);
        actions.add(docFilterStats,BorderLayout.EAST);
        panel.add(actions,BorderLayout.SOUTH);
        wrapper.add(panel,BorderLayout.CENTER);
        return wrapper;
    }
    private JPanel docField(String caption,JTextField field,String tip) {
        JPanel p=new JPanel(new BorderLayout(0,5));p.setOpaque(false);
        p.add(UiKit.caption(caption),BorderLayout.NORTH);
        styleInput(field);field.setToolTipText(tip);
        p.add(field,BorderLayout.CENTER);
        return p;
    }
    private void instalarConsultaDocumentos() {
        DocumentListener listener=new DocumentListener() {
            public void insertUpdate(DocumentEvent e){filtrarDocumentos();}
            public void removeUpdate(DocumentEvent e){filtrarDocumentos();}
            public void changedUpdate(DocumentEvent e){filtrarDocumentos();}
        };
        for(JTextField f:new JTextField[]{docNumero,docSerie,docChave,docParticipante,docData})
            f.getDocument().addDocumentListener(listener);
        docOperacao.addActionListener(e->filtrarDocumentos());
        tabNotas.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if(e.getClickCount()==2 && javax.swing.SwingUtilities.isLeftMouseButton(e))abrirDetalhes();
            }
        });
        tabNotas.getSelectionModel().addListSelectionListener(e->{
            if(!e.getValueIsAdjusting())docsDetailsButton.setEnabled(resultado!=null && tabNotas.getSelectedRow()>=0);
        });
        tabNotas.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),"details");
        tabNotas.getActionMap().put("details",new AbstractAction(){
            @Override public void actionPerformed(ActionEvent e){abrirDetalhes();}
        });
    }
    private DocumentoConsulta.Filtro filtroDoc() {
        return new DocumentoConsulta.Filtro(docNumero.getText(),docSerie.getText(),docChave.getText(),
                docParticipante.getText(),docData.getText(),(String)docOperacao.getSelectedItem());
    }
    private void limparBuscaDocs() {
        docNumero.setText("");docSerie.setText("");docChave.setText("");
        docParticipante.setText("");docData.setText("");docOperacao.setSelectedIndex(0);
        filtrarDocumentos();
    }
    private void filtrarDocumentos() {
        if(resultado==null || tabNotas.getModel().getColumnCount()<12)return;
        final DocumentoConsulta.Filtro filtro=filtroDoc();
        final List<SpedParser.Doc> dados=resultado.documentos;
        @SuppressWarnings("unchecked")
        TableRowSorter<DefaultTableModel> sorter=(TableRowSorter<DefaultTableModel>)tabNotas.getRowSorter();
        if(sorter==null)return;
        sorter.setRowFilter(new RowFilter<DefaultTableModel,Integer>() {
            @Override public boolean include(Entry<? extends DefaultTableModel,? extends Integer> e) {
                return DocumentoConsulta.corresponde(dados.get(e.getIdentifier()),filtro);
            }
        });
        int visibles=tabNotas.getRowCount();
        docFilterStats.setText("Exibindo "+fmtCount(visibles)+" de "+fmtCount(dados.size())+" documentos");
        countDocs.setText(fmtCount(visibles)+" de "+fmtCount(dados.size())+" C100 • Ordene clicando no título das colunas • CSV respeita a consulta.");
        docsDetailsButton.setEnabled(tabNotas.getSelectedRow()>=0);
    }
    private void ajustarOrdenacaoDocumentos() {
        @SuppressWarnings("unchecked")
        TableRowSorter<DefaultTableModel> sorter=(TableRowSorter<DefaultTableModel>)tabNotas.getRowSorter();
        if(sorter==null)return;
        Comparator<Object> inteiro=(a,b)->Integer.compare(((Number)a).intValue(),((Number)b).intValue());
        sorter.setComparator(0,inteiro);sorter.setComparator(11,inteiro);
        sorter.setComparator(5,DocumentoConsulta.numericos());
        sorter.setComparator(6,DocumentoConsulta.datas());
        sorter.setComparator(7,DocumentoConsulta.datas());
        sorter.setComparator(10,DocumentoConsulta.valores());
    }
    private void abrirDetalhes() {
        if(resultado==null || tabNotas.getSelectedRow()<0)return;
        int index=tabNotas.convertRowIndexToModel(tabNotas.getSelectedRow());
        if(index<0 || index>=resultado.documentos.size())return;
        SpedParser.Doc d=resultado.documentos.get(index);
        JDialog dialog=new JDialog(this,"Documento C100 • Nº "+d.numero,JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setIconImage(FrameChrome.appIcon());
        JPanel root=new JPanel(new BorderLayout(0,10));root.setBackground(UiKit.BG);
        root.setBorder(BorderFactory.createEmptyBorder(14,16,14,16));
        JPanel head=new JPanel(new BorderLayout());head.setOpaque(false);
        JLabel title=UiKit.text("Nota "+d.numero+"  •  Série "+(d.serie.isEmpty()?"—":d.serie),20,true,UiKit.NAVY);
        title.setIcon(UiKit.icon("file",24,UiKit.BLUE));head.add(title,BorderLayout.WEST);
        head.add(UiKit.text("Registro C100 — linha "+d.linha,12,true,UiKit.SECONDARY),BorderLayout.EAST);
        root.add(head,BorderLayout.NORTH);
        JPanel sections=new JPanel(new BorderLayout(0,10));sections.setOpaque(false);
        UiKit.Panel details=new UiKit.Panel(UiKit.WHITE,UiKit.BORDER,15);
        details.setLayout(new GridLayout(4,4,12,11));details.setBorder(BorderFactory.createEmptyBorder(17,17,17,17));
        details.add(detailField("Operação",d.operacao));
        details.add(detailField("Emitente",d.emissor));
        details.add(detailField("Modelo",d.modelo));
        details.add(detailField("Situação (COD_SIT)",d.situacao));
        details.add(detailField("Série",d.serie));
        details.add(detailField("Número",d.numero));
        details.add(detailField("Emissão",date(d.data)));
        details.add(detailField("Entrada / saída",date(d.dataEntradaSaida)));
        details.add(detailField("Código participante",d.participanteCodigo));
        details.add(detailField("Nome participante (0150)",d.participante!=null?d.participante.nome:"Não localizado no 0150"));
        details.add(detailField("CNPJ / CPF (0150)",DocumentoConsulta.identificador(d)));
        details.add(detailField("Inscrição estadual",d.participante!=null?d.participante.ie:""));
        details.add(detailField("Valor declarado (VL_DOC)","R$ "+money(d.valor)));
        details.add(detailField("Registros C190",String.valueOf(d.quantidadeC190)));
        details.add(detailField("Itens C170",String.valueOf(d.quantidadeC170)));
        details.add(detailField("Linha original",String.valueOf(d.linha)));
        JPanel detailsWithKey=new JPanel(new BorderLayout(0,8));detailsWithKey.setOpaque(false);
        detailsWithKey.add(details,BorderLayout.NORTH);
        UiKit.Panel keyPanel=new UiKit.Panel(UiKit.WHITE,UiKit.BORDER,14);
        keyPanel.setLayout(new BorderLayout(8,5));
        keyPanel.setBorder(BorderFactory.createEmptyBorder(8,15,10,15));
        JLabel keyTitle=UiKit.caption("CHV_NFE completa — campo selecionável");
        keyPanel.add(keyTitle,BorderLayout.NORTH);
        JTextField fullKey=new JTextField(d.chave);
        fullKey.setEditable(false);
        fullKey.setFont(UiKit.font(12,false));
        fullKey.setForeground(UiKit.NAVY);
        fullKey.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiKit.BORDER),BorderFactory.createEmptyBorder(5,8,5,8)));
        fullKey.setBackground(new Color(251,253,255));
        fullKey.setToolTipText("Selecione e copie a chave com Ctrl+C (somente local).");
        keyPanel.add(fullKey,BorderLayout.CENTER);
        detailsWithKey.add(keyPanel,BorderLayout.SOUTH);
        sections.add(detailsWithKey,BorderLayout.NORTH);
        UiKit.Panel analytic=new UiKit.Panel(UiKit.WHITE,UiKit.BORDER,15);
        analytic.setLayout(new BorderLayout(0,9));analytic.setBorder(BorderFactory.createEmptyBorder(13,14,14,14));
        JLabel subtitle=UiKit.text("Analíticos C190 vinculados ao documento",15,true,UiKit.NAVY);
        subtitle.setIcon(UiKit.icon("analysis",19,UiKit.BLUE));analytic.add(subtitle,BorderLayout.NORTH);
        JTable table=table();
        DefaultTableModel m=model("Linha","CST","CFOP","Alíquota","Valor operação","Base ICMS","ICMS","Base ST","ICMS-ST","IPI");
        List<SpedParser.Analitico> filhos=new ArrayList<SpedParser.Analitico>();
        for(SpedParser.Analitico a:resultado.analiticos)if(a.documento==d) {
            filhos.add(a);
            m.addRow(new Object[]{a.linha,a.cst,a.cfop,a.aliquota,money(a.valorOperacao),money(a.baseIcms),money(a.icms),money(a.baseSt),money(a.icmsSt),money(a.ipi)});
        }
        bind(table,m,68,74,78,90,142,133,120,118,118,118);
        JScrollPane scroll=new JScrollPane(table);scroll.setBorder(BorderFactory.createLineBorder(UiKit.BORDER));
        analytic.add(scroll,BorderLayout.CENTER);
        SpedConsultas.Totais totals=SpedConsultas.totalizar(filhos);
        JPanel sum=new JPanel(new BorderLayout(0,6));sum.setOpaque(false);
        String summary="C190: "+filhos.size()+"  •  VL_OPR: R$ "+money(totals.operacoes)+
                "  •  ICMS: R$ "+money(totals.icms)+"  •  ICMS-ST: R$ "+money(totals.icmsSt)+"  •  IPI: R$ "+money(totals.ipi);
        JLabel totalLabel=UiKit.text(summary,12,true,UiKit.NAVY);
        sum.add(totalLabel,BorderLayout.NORTH);
        JLabel aviso=UiKit.caption("Valores do C190 são declarados, não apurados. Somá-los ao VL_DOC do C100 duplicaria a operação."+
            (totals.linhasInvalidas>0?"  •  "+totals.linhasInvalidas+" C190 com valores inválidos.":""));
        sum.add(aviso,BorderLayout.SOUTH);
        analytic.add(sum,BorderLayout.SOUTH);
        JTabbedPane detailTabs=new JTabbedPane();
        detailTabs.setFont(UiKit.font(13,true));
        detailTabs.addTab("Analíticos C190 ("+filhos.size()+")",analytic);
        UiKit.Panel itemPanel=new UiKit.Panel(UiKit.WHITE,UiKit.BORDER,15);
        itemPanel.setLayout(new BorderLayout(0,8));itemPanel.setBorder(BorderFactory.createEmptyBorder(13,14,14,14));
        JLabel itemHeading=UiKit.text("  Itens C170 vinculados ao documento",15,true,UiKit.NAVY);
        itemHeading.setIcon(UiKit.icon("grid",19,UiKit.BLUE));itemPanel.add(itemHeading,BorderLayout.NORTH);
        JTable detalheItem=table();
        DefaultTableModel mi=model("Linha","Item","COD_ITEM","Descrição 0200","NCM","CFOP","CST","Qtd","UNID","VL_ITEM","ICMS","IPI");
        List<SpedParser.Item> documentItems=new ArrayList<SpedParser.Item>();
        for(SpedParser.Item i:resultado.itens)if(i.documento==d) {
            documentItems.add(i);
            mi.addRow(new Object[]{i.linha,i.numero,i.codigo,i.descricao(),i.ncm(),i.cfop,i.cst,itemFormat(i,"QTD",i.quantidade,true),i.unidade,itemFormat(i,"VL_ITEM",i.valor,false),itemFormat(i,"VL_ICMS",i.icms,false),itemFormat(i,"VL_IPI",i.ipi,false)});
        }
        bind(detalheItem,mi,62,58,108,230,80,76,68,100,60,100,90,90);
        JScrollPane diScroll=new JScrollPane(detalheItem);diScroll.setBorder(BorderFactory.createLineBorder(UiKit.BORDER));
        itemPanel.add(diScroll,BorderLayout.CENTER);
        ItemConsulta.Totais itemTotals=ItemConsulta.totalizar(documentItems);
        itemPanel.add(UiKit.caption(documentItems.isEmpty()?"C170 não encontrado nesta nota. Isso não determina omissão: verifique modelo, emitente e situação do documento.":
            "Itens: "+itemTotals.registros+"   |   VL_ITEM: R$ "+money(itemTotals.valor)+"   |   "+itemTotals.semCadastro+" sem cadastro 0200. Dados declarados."),BorderLayout.SOUTH);
        detailTabs.addTab("Itens C170 ("+documentItems.size()+")",itemPanel);
        sections.add(detailTabs,BorderLayout.CENTER);
        root.add(sections,BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));actions.setOpaque(false);
        JButton close=new UiKit.ActionButton("Fechar",UiKit.icon("close",15,UiKit.NAVY),UiKit.SKY,new Color(217,234,251),UiKit.NAVY);
        close.addActionListener(e->dialog.dispose());actions.add(close);
        root.add(actions,BorderLayout.SOUTH);
        dialog.setContentPane(root);dialog.setSize(1110,720);
        dialog.setMinimumSize(new Dimension(860,620));dialog.setLocationRelativeTo(this);dialog.setVisible(true);
    }
    private JPanel detailField(String name,String value) {
        JPanel panel=new JPanel();panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
        JLabel top=UiKit.text(name,11,false,UiKit.SECONDARY);top.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel data=UiKit.text(value==null||value.isEmpty()?"—":value,12,true,UiKit.NAVY);
        data.setToolTipText(value);data.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(top);panel.add(Box.createVerticalStrut(5));panel.add(data);
        return panel;
    }
    private JPanel container(Component content) {JPanel p=new JPanel(new BorderLayout());p.setBackground(UiKit.BG);p.add(content);return p;}
    private JPanel statusBar() {
        return FrameChrome.statusBar(statusIcon,status,VERSAO);
    }
    private void atualizarStatus(String mensagem,String tipo) {
        status.setText(mensagem);
        status.setToolTipText(mensagem);
        statusIcon.setIcon(FrameChrome.statusIcon(tipo));
    }
    private static JTable table() {
        JTable t=new JTable();t.setFont(UiKit.font(12,false));t.setForeground(UiKit.NAVY);
        t.setRowHeight(29);t.setGridColor(new Color(230,237,244));
        t.setShowHorizontalLines(true);t.setShowVerticalLines(false);
        t.setSelectionBackground(new Color(210,233,252));t.setSelectionForeground(UiKit.NAVY);
        t.setFillsViewportHeight(true);t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        t.getTableHeader().setFont(UiKit.font(12,true));t.getTableHeader().setBackground(new Color(228,239,250));
        t.getTableHeader().setForeground(UiKit.NAVY);t.getTableHeader().setReorderingAllowed(false);
        t.setDefaultEditor(Object.class,null);
        t.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable table,Object value,boolean selected,boolean focused,int row,int col){
                super.getTableCellRendererComponent(table,value,selected,focused,row,col);
                setFont(UiKit.font(12,false));setBorder(BorderFactory.createEmptyBorder(1,9,1,7));
                if (!selected) {setBackground(row%2==0?Color.WHITE:new Color(248,251,255));setForeground("INVÁLIDO".equals(value)?UiKit.RED:UiKit.NAVY);}
                return this;
            }
        });
        return t;
    }
    private static DefaultTableModel model(String... headers) {
        return new DefaultTableModel(headers,0){private static final long serialVersionUID=1L;
            public boolean isCellEditable(int r,int c){return false;}};
    }
    private static void bind(JTable t,DefaultTableModel m,int... widths) {
        t.setModel(m);t.setAutoCreateRowSorter(true);
        for(int i=0;i<t.getColumnCount();i++) {
            int w=i<widths.length?widths[i]:120;
            t.getColumnModel().getColumn(i).setPreferredWidth(w);
        }
    }
    /** Área inicial de Auditoria Fiscal: descoberta de ferramentas, não execução implícita. */
    private JPanel catalogoPage() {
        JPanel whole=new JPanel(new BorderLayout(0,13));whole.setBackground(UiKit.BG);
        whole.setBorder(BorderFactory.createEmptyBorder(0,16,16,16));
        UiKit.Panel heading=new UiKit.Panel(UiKit.WHITE,UiKit.BORDER,16);
        heading.setLayout(new BorderLayout(0,7));heading.setBorder(BorderFactory.createEmptyBorder(18,19,16,19));
        JLabel title=UiKit.text("Auditoria Fiscal  /  Catálogo de ferramentas",19,true,UiKit.NAVY);
        title.setIcon(UiKit.icon("alert",24,UiKit.BLUE));
        heading.add(title,BorderLayout.NORTH);
        JPanel desc=new JPanel();desc.setOpaque(false);desc.setLayout(new BoxLayout(desc,BoxLayout.Y_AXIS));
        JLabel t=UiKit.text("Escolha uma função específica. Cada ferramenta possui seu escopo e suas limitações.",13,false,UiKit.SECONDARY);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);desc.add(t);desc.add(Box.createVerticalStrut(9));
        catalogStatus.setAlignmentX(Component.LEFT_ALIGNMENT);desc.add(catalogStatus);
        heading.add(desc,BorderLayout.CENTER);
        JButton docs=new JButton("Abrir documentação");docs.addActionListener(e->ajuda());
        heading.add(docs,BorderLayout.EAST);
        whole.add(heading,BorderLayout.NORTH);
        JPanel tools=new JPanel(new GridLayout(0,2,13,13));tools.setOpaque(false);
        for(CatalogoAuditoria.Ferramenta tool:CatalogoAuditoria.todas())tools.add(cartaoAuditoria(tool));
        JPanel content=new JPanel(new BorderLayout(0,13));content.setOpaque(false);
        content.add(tools,BorderLayout.NORTH);
        UiKit.Panel note=new UiKit.Panel(new Color(235,244,252),UiKit.BORDER,14);
        note.setLayout(new BorderLayout());note.setBorder(BorderFactory.createEmptyBorder(12,16,12,16));
        note.add(UiKit.text("A detecção de uma ocorrência não é, por si só, confirmação de erro fiscal. "+
            "Nunca alteramos automaticamente o arquivo original.",12,false,UiKit.NAVY),BorderLayout.CENTER);
        content.add(note,BorderLayout.SOUTH);
        JScrollPane sc=new JScrollPane(content);
        sc.setBorder(null);sc.getViewport().setBackground(UiKit.BG);
        sc.getVerticalScrollBar().setUnitIncrement(18);
        whole.add(sc,BorderLayout.CENTER);
        return whole;
    }
    private JPanel cartaoAuditoria(final CatalogoAuditoria.Ferramenta tool) {
        UiKit.Panel panel=new UiKit.Panel(UiKit.WHITE,UiKit.BORDER,15);
        panel.setLayout(new BorderLayout(0,9));panel.setBorder(BorderFactory.createEmptyBorder(15,16,15,16));
        JPanel title=new JPanel(new BorderLayout(10,0));title.setOpaque(false);
        JLabel icon=new JLabel(UiKit.icon(tool.icone,26,tool.estado==CatalogoAuditoria.Estado.DISPONIVEL?UiKit.BLUE:UiKit.SECONDARY));
        title.add(icon,BorderLayout.WEST);
        JLabel heading=UiKit.text(tool.titulo,14,true,UiKit.NAVY);title.add(heading,BorderLayout.CENTER);
        JLabel estado=UiKit.text(tool.estado==CatalogoAuditoria.Estado.DISPONIVEL?"DISPONÍVEL":"PLANEJADA",10,true,
            tool.estado==CatalogoAuditoria.Estado.DISPONIVEL?UiKit.GREEN:UiKit.SECONDARY);
        title.add(estado,BorderLayout.EAST);panel.add(title,BorderLayout.NORTH);
        JPanel details=new JPanel();details.setOpaque(false);details.setLayout(new BoxLayout(details,BoxLayout.Y_AXIS));
        JLabel code=UiKit.text(tool.id+"  •  "+tool.abrangencia,11,true,UiKit.BLUE);code.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.add(code);details.add(Box.createVerticalStrut(8));
        JTextArea descricao=textoAjustavel(tool.descricao,12,2);
        descricao.setAlignmentX(Component.LEFT_ALIGNMENT);details.add(descricao);
        details.add(Box.createVerticalStrut(6));
        JTextArea aviso=textoAjustavel(tool.aviso,11,2);aviso.setForeground(UiKit.SECONDARY);
        aviso.setAlignmentX(Component.LEFT_ALIGNMENT);details.add(aviso);
        panel.add(details,BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));actions.setOpaque(false);
        JButton action=new JButton(tool.estado==CatalogoAuditoria.Estado.PLANEJADA?"Em planejamento":"Abrir ferramenta");
        action.setFont(UiKit.font(12,true));
        action.addActionListener(e->{
            if("copy".equals(tool.pagina))salvarCopiaEfd();
            else if(tool.pagina!=null && !tool.pagina.isEmpty())mostrar(tool.pagina);
        });
        action.setEnabled(tool.executavel(false));
        acoesCatalogo.add(action);ferramentasCatalogo.add(tool);
        JButton learn=new JButton("Entenda esta função");learn.setFont(UiKit.font(12,false));
        learn.addActionListener(e->abrirAjuda(tool.topicoAjuda));
        actions.add(action);actions.add(learn);panel.add(actions,BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(350,207));
        return panel;
    }
    private static JTextArea textoAjustavel(String texto,int tamanho,int linhas) {
        JTextArea area=new JTextArea(texto);area.setEditable(false);area.setLineWrap(true);area.setWrapStyleWord(true);
        area.setOpaque(false);area.setFont(UiKit.font(tamanho,false));area.setForeground(UiKit.NAVY);
        area.setRows(linhas);area.setFocusable(false);return area;
    }
    /** O diagnóstico antes modal é agora um painel consultivo, sempre dentro do workspace. */
    private JPanel diagnosticoPage() {
        JPanel page=new JPanel(new BorderLayout(0,12));page.setBackground(UiKit.BG);
        page.setBorder(BorderFactory.createEmptyBorder(0,16,16,16));
        UiKit.Panel card=new UiKit.Panel(UiKit.WHITE,UiKit.BORDER,16);
        card.setLayout(new BorderLayout(0,12));card.setBorder(BorderFactory.createEmptyBorder(15,17,15,17));
        JPanel head=new JPanel(new BorderLayout(8,8));head.setOpaque(false);
        JPanel title=new JPanel();title.setOpaque(false);title.setLayout(new BoxLayout(title,BoxLayout.Y_AXIS));
        title.add(UiKit.text("Diagnóstico complementar — bases, CST e 0200",17,true,UiKit.NAVY));
        title.add(Box.createVerticalStrut(7));
        title.add(UiKit.caption("Relatório de indícios para análise. Não modifica os registros da EFD."));
        head.add(title,BorderLayout.CENTER);
        JButton help=new JButton("Como interpretar");help.addActionListener(e->abrirAjuda("diagnostico"));
        head.add(help,BorderLayout.EAST);card.add(head,BorderLayout.NORTH);
        diagnosticoTexto.setEditable(false);diagnosticoTexto.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        diagnosticoTexto.setLineWrap(false);diagnosticoTexto.setText("Abra uma EFD para gerar o diagnóstico.");
        JScrollPane report=new JScrollPane(diagnosticoTexto);
        report.setBorder(BorderFactory.createLineBorder(UiKit.BORDER));
        card.add(report,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout());footer.setOpaque(false);
        footer.add(diagnosticoTotais,BorderLayout.CENTER);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));buttons.setOpaque(false);
        JButton back=new JButton("Voltar ao catálogo");back.addActionListener(e->mostrar("audit"));
        JButton products=new JButton("Consultar cadastro 0200");products.addActionListener(e->mostrar("products"));
        buttons.add(products);buttons.add(back);footer.add(buttons,BorderLayout.EAST);
        card.add(footer,BorderLayout.SOUTH);page.add(card,BorderLayout.CENTER);
        return page;
    }
    private void atualizarDiagnostico() {
        if(resultado==null){diagnosticoTexto.setText("Abra uma EFD para gerar o diagnóstico.");return;}
        PendenciasTributarias.Resumo r=PendenciasTributarias.analisar(resultado);
        diagnosticoTotais.setText(r.basesSemCredito+" bases (R$ "+MOEDA.format(r.valorBases)+")  •  "+
            r.c170AliquotaSemCredito+" alíquotas  •  "+r.produtosClassificacaoDuvidosa+" produtos a conferir");
        diagnosticoTexto.setText(r.relatorio);diagnosticoTexto.setCaretPosition(0);
    }
    /** Documentação interna offline, com os tópicos acessíveis por navegação. */
    private JPanel ajudaPage() {
        JPanel page=new JPanel(new BorderLayout(0,12));page.setBackground(UiKit.BG);
        page.setBorder(BorderFactory.createEmptyBorder(0,16,16,16));
        UiKit.Panel heading=new UiKit.Panel(UiKit.WHITE,UiKit.BORDER,14);
        heading.setLayout(new BorderLayout());heading.setBorder(BorderFactory.createEmptyBorder(14,18,14,18));
        JLabel title=UiKit.text("Ajuda e documentação",18,true,UiKit.NAVY);
        title.setIcon(UiKit.icon("info",22,UiKit.BLUE));heading.add(title,BorderLayout.WEST);
        heading.add(UiKit.caption("Manual integrado · Sem acesso à internet"),BorderLayout.EAST);
        page.add(heading,BorderLayout.NORTH);
        UiKit.Panel body=new UiKit.Panel(UiKit.WHITE,UiKit.BORDER,14);body.setLayout(new BorderLayout());
        JPanel topics=new JPanel();topics.setOpaque(false);topics.setLayout(new BoxLayout(topics,BoxLayout.Y_AXIS));
        topics.setBorder(BorderFactory.createEmptyBorder(16,12,16,12));
        String[][] labels={{"inicio","Primeiros passos"},{"consultas","Consultas e relatórios"},
            {"creditos","Correção de créditos RN07"},{"diagnostico","Bases, CST e 0200"},
            {"ocorrencias","Ocorrências de leitura"},{"arquivos","Arquivos e substitutas"},
            {"futuras","Próximas ferramentas"},{"limites","Limites do sistema"}};
        for(String[] row:labels){
            final String topic=row[0];
            JButton btn=new JButton(row[1]);btn.setFont(UiKit.font(12,false));
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setMaximumSize(new Dimension(215,39));btn.setPreferredSize(new Dimension(215,39));
            btn.setFocusPainted(false);
            btn.addActionListener(e->selecionarTopico(topic));topics.add(btn);topics.add(Box.createVerticalStrut(6));
            helpTopics.put(topic,btn);
        }
        body.add(topics,BorderLayout.WEST);
        helpText.setContentType("text/html");helpText.setEditable(false);
        helpText.setBorder(BorderFactory.createEmptyBorder(6,5,5,5));
        JScrollPane sc=new JScrollPane(helpText);sc.setBorder(BorderFactory.createMatteBorder(0,1,0,0,UiKit.BORDER));
        sc.getVerticalScrollBar().setUnitIncrement(16);body.add(sc,BorderLayout.CENTER);
        page.add(body,BorderLayout.CENTER);
        selecionarTopico("inicio");
        return page;
    }
    private void selecionarTopico(String topic) {
        helpTopic=AjudaInterna.contem(topic)?topic:"inicio";
        helpText.setText(AjudaInterna.conteudo(helpTopic));helpText.setCaretPosition(0);
        for(java.util.Map.Entry<String,JButton> item:helpTopics.entrySet()){
            boolean selected=item.getKey().equals(helpTopic);
            item.getValue().setBackground(selected?UiKit.SKY:UiKit.WHITE);
            item.getValue().setForeground(selected?UiKit.BLUE:UiKit.NAVY);
            item.getValue().setFont(UiKit.font(12,selected));
        }
    }
    private void abrirAjuda(String topic) {selecionarTopico(topic);mostrar("help");}
    /** Pesquisa MG-RN07: ocorrência potencial, sem julgar a destinação real dos bens. */
    private JPanel auditoriaPage() {
        JPanel page = dataPage("Créditos de uso e consumo • MG-RN07",
            "Crédito de ICMS em compras para uso e consumo (CFOP 1.556/2.556/3.556). Somente ocorrências selecionadas serão propostas para correção.",
            "alert", tabAuditoria, auditCount, auditTotal);
        JPanel bottom = new JPanel(new BorderLayout(12,8));bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(8,14,12,14));
        JTextArea orientacao=new JTextArea("SUSPEITA, NÃO AUTUAÇÃO: confirme a destinação efetiva dos bens antes de corrigir. " +
            "Corrige somente os grupos selecionados e conciliados (CST/ALIQ/BC/ICMS de C170 e C190, " +
            "com reflexos em C100/E110 e fechamento). A base sem crédito fora da seleção e TIPO_ITEM 0200 " +
            "continuam para revisão humana. Apuração complexa é bloqueada. Nunca altera o original.");
        orientacao.setEditable(false);orientacao.setLineWrap(true);orientacao.setWrapStyleWord(true);
        orientacao.setRows(2);orientacao.setPreferredSize(new Dimension(850,35));
        orientacao.setOpaque(false);orientacao.setForeground(UiKit.SECONDARY);orientacao.setFont(UiKit.font(12,false));
        JPanel instructions=new JPanel();instructions.setOpaque(false);
        instructions.setLayout(new BoxLayout(instructions,BoxLayout.Y_AXIS));
        instructions.add(orientacao);
        auditSubstituta.setOpaque(false);
        auditSubstituta.setFont(UiKit.font(12,false));
        instructions.add(auditSubstituta);
        JButton pendencias=new JButton("Ver diagnóstico de bases, CST e produtos (0200)");
        pendencias.addActionListener(e->mostrar("audit-diagnostic"));
        pendencias.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        instructions.add(pendencias);
        bottom.add(instructions,BorderLayout.CENTER);
        JPanel actions=new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT,8,0));actions.setOpaque(false);
        JButton selecionar=new JButton("Marcar conciliados");
        JButton desmarcar=new JButton("Desmarcar todos");
        selecionar.addActionListener(e->marcarAuditoria(true));
        desmarcar.addActionListener(e->marcarAuditoria(false));
        actions.add(selecionar);actions.add(desmarcar);actions.add(auditSaveCopy);actions.add(auditPreview);
        auditSaveCopy.addActionListener(e->salvarCopiaEfd());
        auditSaveCopy.setEnabled(false);
        bottom.add(actions,BorderLayout.SOUTH);
        auditPreview.setEnabled(false);
        auditPreview.addActionListener(e->previsualizarAuditoria());
        page.add(bottom,BorderLayout.SOUTH);
        JPanel navBar=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));navBar.setOpaque(false);
        JButton back=new JButton("← Voltar ao catálogo");back.addActionListener(e->mostrar("audit"));
        JButton learn=new JButton("Ajuda da ferramenta");learn.addActionListener(e->abrirAjuda("creditos"));
        navBar.add(back);navBar.add(learn);page.add(navBar,BorderLayout.NORTH);
        return page;
    }
    private void marcarAuditoria(boolean marcar) {
        DefaultTableModel model=(DefaultTableModel)tabAuditoria.getModel();
        for(int i=0;i<model.getRowCount();i++)
            if(Boolean.TRUE.equals(model.getValueAt(i,10)))model.setValueAt(Boolean.valueOf(marcar),i,0);
    }
    private void atualizarAuditoria() {
        if(resultado==null)return;
        casosAuditoria=CreditoUsoConsumo.analisar(resultado);
        DefaultTableModel model=new DefaultTableModel(new Object[]{
            "Corrigir?","NF-e","Emitente / participante","CFOP","CST","Alíq.",
            "Crédito (R$)","Itens C170","Linha C190","Conferência"},0) {
            @Override public boolean isCellEditable(int r,int c){return c==0 && Boolean.TRUE.equals(getValueAt(r,10));}
            @Override public Class<?> getColumnClass(int c){return c==0||c==10?Boolean.class:super.getColumnClass(c);}
        };
        // Coluna técnica 10 é adicionada e escondida da visualização para habilitar apenas registros conciliados.
        model.setColumnIdentifiers(new Object[]{"Corrigir?","NF-e","Participante","CFOP","CST","Alíq.",
            "Crédito (R$)","Itens C170","Linha C190","Conferência","Elegível"});
        BigDecimal total=BigDecimal.ZERO;int disponiveis=0;
        for(CreditoUsoConsumo.Caso c:casosAuditoria){
            total=total.add(c.credito);if(c.elegivel)disponiveis++;
            model.addRow(new Object[]{Boolean.FALSE,c.nota,c.participante,c.cfop,c.cst,c.aliquota,
                MOEDA.format(c.credito),Integer.valueOf(c.itens),Integer.valueOf(c.linhaC190),c.motivo,Boolean.valueOf(c.elegivel)});
        }
        tabAuditoria.setModel(model);
        tabAuditoria.removeColumn(tabAuditoria.getColumnModel().getColumn(10));
        tabAuditoria.setAutoCreateRowSorter(true);
        int[] widths={80,90,195,66,63,65,115,90,96,250};
        for(int i=0;i<widths.length;i++)tabAuditoria.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        tabAuditoria.setRowHeight(29);
        if(tabAuditoria.getColumnModel().getColumnCount()>0)
            tabAuditoria.getColumnModel().getColumn(0).setMaxWidth(87);
        auditCount.setText("Arquivo "+("1".equals(resultado.codFin)?"SUBSTITUTO (COD_FIN=1)":"ORIGINAL (COD_FIN=0)")+
                " | "+casosAuditoria.size()+" grupos suspeitos; "+disponiveis+" elegíveis. A marcação é manual.");
        auditSubstituta.setSelected(true); // recomendacao de gravar substituta, inclusive quando origem COD_FIN=0
        auditSubstituta.setEnabled(!"1".equals(resultado.codFin));
        auditSubstituta.setText("1".equals(resultado.codFin)
                ?"Arquivo já substituto: manter COD_FIN=1"
                :"Preparar arquivo substituto (COD_FIN=1; desmarque apenas se for remessa original)");
        auditSaveCopy.setEnabled(true);
        PendenciasTributarias.Resumo diagnostico=PendenciasTributarias.analisar(resultado);
        auditTotal.setText("RN07: R$ "+MOEDA.format(total)+"  |  Bases sem crédito: R$ "+
            MOEDA.format(diagnostico.valorBases)+" ("+diagnostico.basesSemCredito+")  |  0200 a conferir: "+
            diagnostico.produtosClassificacaoDuvidosa);
        auditPreview.setEnabled(disponiveis>0);
    }
    private void previsualizarAuditoria() {
        if(resultado==null)return;
        java.util.Set<Integer> selected=new java.util.LinkedHashSet<Integer>();
        DefaultTableModel model=(DefaultTableModel)tabAuditoria.getModel();
        for(int i=0;i<model.getRowCount();i++)if(Boolean.TRUE.equals(model.getValueAt(i,0)))
            selected.add(((Number)model.getValueAt(i,8)).intValue());
        if(selected.isEmpty()){
            JOptionPane.showMessageDialog(this,"Marque ao menos uma linha na coluna 'Corrigir?'.",
                "Nenhum grupo selecionado",JOptionPane.INFORMATION_MESSAGE);return;
        }
        final CreditoUsoConsumo.Plano plano;
        try{plano=CreditoUsoConsumo.planejar(resultado,selected,auditSubstituta.isSelected());}
        catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Correção bloqueada",JOptionPane.WARNING_MESSAGE);return;}
        JTextArea preview=new JTextArea(20,82);
        StringBuilder sb=new StringBuilder();
        sb.append(plano.resumo).append("\n\nMUDANÇAS (nenhuma aplicada ainda):\n");
        for(CreditoUsoConsumo.Mudanca m:plano.mudancas)sb.append(m.toString()).append('\n');
        preview.setText(sb.toString());preview.setCaretPosition(0);preview.setEditable(false);
        preview.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        javax.swing.JCheckBox confirmacao=new javax.swing.JCheckBox(
            "Confirmei documentalmente: as mercadorias são de uso/consumo e o crédito não é permitido.");
        JPanel content=new JPanel(new BorderLayout(8,12));
        content.add(new JScrollPane(preview),BorderLayout.CENTER);
        content.add(confirmacao,BorderLayout.SOUTH);
        int ok=JOptionPane.showConfirmDialog(this,content,"Prévia de correção fiscal (SPED/TXT)",
                JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
        if(ok!=JOptionPane.OK_OPTION)return;
        if(!confirmacao.isSelected()){
            JOptionPane.showMessageDialog(this,"Confirmação fiscal obrigatória; nada foi gravado.",
                "Operação cancelada",JOptionPane.INFORMATION_MESSAGE);return;
        }
        File destino=escolherDestinoEfd("Salvar NOVA EFD corrigida", "-corrigido-RN07");
        if(destino==null)return;
        try{
            File saida=CreditoUsoConsumo.salvar(plano,destino);
            JOptionPane.showMessageDialog(this,"Arquivo corrigido salvo em:\n"+saida.getAbsolutePath()+
                "\n\nOriginal preservado. Revise CST, base, alíquota e classificação 0200; "+
                "importe no PVA e valide antes de transmitir.",
                "EFD corrigida criada",JOptionPane.INFORMATION_MESSAGE);
            atualizarStatus("EFD corrigida salva ("+extensao(destino)+"). Diagnóstico complementar e PVA ainda necessários.","ok");
        }catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Não foi possível salvar",JOptionPane.ERROR_MESSAGE);}
    }

    private void exibirPendencias() {
        if(resultado==null){JOptionPane.showMessageDialog(this,"Importe uma EFD para diagnosticar.");return;}
        atualizarDiagnostico();mostrar("audit-diagnostic");
    }

    /** Permite .SPED e .TXT; escolha do filtro decide extensao se nao digitada. */
    private File escolherDestinoEfd(String titulo,String sufixo) {
        if(resultado==null){
            JOptionPane.showMessageDialog(this,"Abra primeiro um SPED original ou substituto.");return null;
        }
        JFileChooser fc=new JFileChooser(resultado.arquivo.getParentFile());
        fc.setDialogTitle(titulo+" (sem sobrescrever)");
        FileNameExtensionFilter sped=new FileNameExtensionFilter("Arquivo SPED (*.sped)","sped");
        FileNameExtensionFilter txt=new FileNameExtensionFilter("Texto EFD (*.txt)","txt");
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(sped);fc.addChoosableFileFilter(txt);
        fc.setFileFilter(sped);
        String nome=resultado.arquivo.getName().replaceAll("(?i)\\.(txt|sped)$","")+sufixo+".sped";
        fc.setSelectedFile(new File(resultado.arquivo.getParentFile(),nome));
        fc.addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, event->{
            String selecionado=fc.getSelectedFile()==null?"":fc.getSelectedFile().getName();
            String novo=selecionado.replaceAll("(?i)\\.(sped|txt)$","")+(fc.getFileFilter()==txt?".txt":".sped");
            if(!selecionado.isEmpty())fc.setSelectedFile(new File(fc.getCurrentDirectory(),novo));
        });
        if(fc.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return null;
        try{
            String preferida=fc.getFileFilter()==txt?"txt":"sped";
            return SpedFileIO.comExtensao(fc.getSelectedFile(),preferida);
        }catch(java.io.IOException e){
            JOptionPane.showMessageDialog(this,e.getMessage(),"Extensão inválida",JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }
    private static String extensao(File f) {
        return f.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".sped")?".SPED":".TXT";
    }
    /** Cópia de bytes: não mexe em CST, COD_FIN, finalizadores, charset ou quebras de linha. */
    private void salvarCopiaEfd() {
        if(resultado==null){JOptionPane.showMessageDialog(this,"Importe um arquivo SPED antes de salvar.");return;}
        File destino=escolherDestinoEfd("Salvar cópia exata da EFD", "-copia");
        if(destino==null)return;
        try{
            SpedFileIO.copiar(resultado,destino);
            JOptionPane.showMessageDialog(this,"Cópia salva: "+destino.getAbsolutePath()+
                "\nCOD_FIN="+resultado.codFin+("1".equals(resultado.codFin)?" (substituta)":" (original)")+
                "\n\nCópia byte a byte. A extensão não valida o conteúdo. Abra no PVA para validar.",
                "Cópia EFD salva",JOptionPane.INFORMATION_MESSAGE);
            atualizarStatus("Cópia "+extensao(destino)+" salva com finalidade original preservada.","ok");
        }catch(Exception ex){
            JOptionPane.showMessageDialog(this,"Falha ao salvar cópia:\n"+ex.getMessage(),"Exportação bloqueada",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrar(String id) {
        if(!navButtons.containsKey(id) && !"audit-credit".equals(id) && !"audit-diagnostic".equals(id))return;
        currentPage=id;((CardLayout)centralCards.getLayout()).show(centralCards,id);
        String navId=id.startsWith("audit-")?"audit":id;
        for(Map.Entry<String,UiKit.NavButton> e:navButtons.entrySet()) e.getValue().setActive(navId.equals(e.getKey()));
        filterArea.setVisible("analytic".equals(id)||"cfop".equals(id)||"reports".equals(id));
        centralCards.revalidate();centralCards.repaint();
    }
    private void instalarFiltros() {
        DocumentListener dl=new DocumentListener() {
            public void insertUpdate(DocumentEvent e){filtrar();}
            public void removeUpdate(DocumentEvent e){filtrar();}
            public void changedUpdate(DocumentEvent e){filtrar();}
        };
        cfopFilter.getDocument().addDocumentListener(dl);
        cstFilter.getDocument().addDocumentListener(dl);
        opFilter.addActionListener(e->filtrar());
        cfopChoices.addActionListener(e->filtrar());
        cstChoices.addActionListener(e->filtrar());
    }
    private void limparFiltros() {
        cfopFilter.setText("");cstFilter.setText("");opFilter.setSelectedIndex(0);
        cfopChoices.setSelectedIndex(0);cstChoices.setSelectedIndex(0);filtrar();
    }
    private void setHasData(boolean loaded) {
        refreshButton.setEnabled(loaded);exportButton.setEnabled(loaded);
        if(relatorioXlsxButton!=null)relatorioXlsxButton.setEnabled(loaded);
        for(JTextField f:new JTextField[]{itemTexto,itemNcm,itemCfop,itemCst,produtoTexto,produtoNcm})f.setEnabled(loaded);
        itemOperacao.setEnabled(loaded);
        if(relatorioPdfButton!=null)relatorioPdfButton.setEnabled(loaded);
        if(tabelaXlsxButton!=null)tabelaXlsxButton.setEnabled(loaded);
        cfopFilter.setEnabled(loaded);cstFilter.setEnabled(loaded);opFilter.setEnabled(loaded);
        cfopChoices.setEnabled(loaded);cstChoices.setEnabled(loaded);
        for(JTextField t:new JTextField[]{docNumero,docSerie,docChave,docParticipante,docData})t.setEnabled(loaded);
        docOperacao.setEnabled(loaded);
        docsDetailsButton.setEnabled(loaded && tabNotas.getSelectedRow()>=0);
        auditPreview.setEnabled(loaded && !casosAuditoria.isEmpty());
        auditSaveCopy.setEnabled(loaded);
        for(int i=0;i<acoesCatalogo.size();i++)
            acoesCatalogo.get(i).setEnabled(ferramentasCatalogo.get(i).executavel(loaded));
        catalogStatus.setText(loaded?
            "EFD carregada: "+("1".equals(resultado.codFin)?"SUBSTITUTA (COD_FIN=1)":"ORIGINAL (COD_FIN=0)")+
            "  •  Ferramentas disponíveis para análise.":"Abra uma EFD original ou substituta para habilitar as ferramentas.");
    }
    private void abrir() {
        JFileChooser fc=new JFileChooser(pastaAnterior==null ? new File(System.getProperty("user.home")) : pastaAnterior);
        fc.setDialogTitle("Abrir SPED Fiscal — EFD ICMS/IPI (.txt ou .sped)");
        fc.setFileFilter(new FileNameExtensionFilter("Arquivo SPED (*.txt, *.sped)","txt","sped"));
        if(fc.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        File selected=fc.getSelectedFile();pastaAnterior=selected.getParentFile();
        openButton.setEnabled(false);setHasData(false);
        atualizarStatus("Lendo: "+selected.getName()+" ...","busy");
        new SwingWorker<SpedParser.Resultado,Void>() {
            protected SpedParser.Resultado doInBackground() throws Exception {return SpedParser.ler(selected);}
            protected void done() {
                openButton.setEnabled(true);
                try {
                    SpedParser.Resultado r=get();resultado=r;
                    preencher();limparFiltros();limparBuscaDocs();setHasData(true);mostrar("overview");
                    atualizarStatus("Leitura concluída: "+fmtCount(r.linhasLidas)+" linhas  |  "+fmtCount(r.documentos.size())+" C100  |  "+fmtCount(r.analiticos.size())+" C190  |  "+fmtCount(r.itens.size())+" C170  |  "+fmtCount(r.ocorrencias.size())+" ocorrências  |  Charset "+r.charset,"ok");
                } catch(Exception ex) {
                    setHasData(resultado!=null);
                    atualizarStatus("Falha na leitura: arquivo original preservado.","alert");
                    Throwable cause=ex.getCause()==null?ex:ex.getCause();
                    JOptionPane.showMessageDialog(SpedWindow.this,"Não foi possível ler o arquivo:\n"+cause.getMessage(),"Erro de importação",JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
    private void preencher() {
        if(resultado==null)return;
        atualizarAuditoria();
        atualizarDiagnostico();
        SpedParser.Resultado r=resultado;
        empresa.setText(r.nome.isEmpty()?"(não identificada)":r.nome);
        empresa.setToolTipText(r.nome);cnpj.setText(formatCnpj(r.cnpj));uf.setText(r.uf);
        periodo.setText(date(r.periodoInicio)+" a "+date(r.periodoFim));
        docCount.setText(fmtCount(r.documentos.size()));c190Count.setText(fmtCount(r.analiticos.size()));
        e110Count.setText(fmtCount(r.countE110));errCount.setText(fmtCount(r.ocorrencias.size()));
        situation.setText("✓  Arquivo carregado com sucesso");situation.setForeground(UiKit.GREEN);
        fileInfo.setText("Arquivo:   "+abbreviate(r.arquivo.getAbsolutePath(),100));
        fileInfo.setToolTipText(r.arquivo.getAbsolutePath());
        versionInfo.setText("COD_VER:   "+r.codVer+"   |   Finalidade: "+("1".equals(r.codFin)?"SUBSTITUTA (COD_FIN=1)":"ORIGINAL (COD_FIN=0)"));
        charsetInfo.setText("Codificação:   "+r.charset);
        linesInfo.setText("Linhas lidas:   "+fmtCount(r.linhasLidas));
        countsInfo.setText("Registros C170: "+fmtCount(r.itens.size())+"   |   Itens 0200: "+fmtCount(r.produtos.size())+"   |   Registros E100:  "+r.countE100+"    |    Registros E110:  "+r.countE110+"    |    Linhas descartadas:  "+r.linhasIgnoradas);
        DefaultTableModel n=model("Linha","Operação","Modelo","Sit.","Série","Nº nota","Emissão","Entrada/saída","Participante (0150)","CNPJ / CPF","Valor documento","Qt. C190","Chave NF-e");
        for(SpedParser.Doc d:r.documentos)
            n.addRow(new Object[]{d.linha,d.operacao,d.modelo,d.situacao,d.serie,d.numero,date(d.data),date(d.dataEntradaSaida),
                DocumentoConsulta.participante(d),DocumentoConsulta.identificador(d),money(d.valor),d.quantidadeC190,d.chave});
        bind(tabNotas,n,70,100,75,60,70,105,110,110,260,140,142,88,355);
        ajustarOrdenacaoDocumentos();
        filtrarDocumentos();
        DefaultTableModel a=model("Linha","Nº nota","Operação","Sit.","CST","CFOP","Alíquota","Valor operação","Base ICMS","ICMS","Base ST","ICMS-ST","IPI");
        for(SpedParser.Analitico v:r.analiticos) {
            String numero=v.documento==null?"(sem C100)":v.documento.numero;
            String situacao=v.documento==null?"?":v.documento.situacao;
            a.addRow(new Object[]{v.linha,numero,SpedConsultas.operacao(v),situacao,v.cst,v.cfop,v.aliquota,
                money(v.valorOperacao),money(v.baseIcms),money(v.icms),money(v.baseSt),money(v.icmsSt),money(v.ipi)});
        }
        bind(tabC190,a,75,110,105,65,76,82,95,145,132,115,125,115,108);
        DefaultTableModel itensModel=model("Linha","Nº nota","Operação","Item Nº","COD_ITEM","Descrição (0200)","NCM","CFOP","CST", "Qtd","UNID","VL_ITEM","VL_DESC","BC ICMS","ICMS","BC ST","ICMS-ST","BC IPI","IPI","Cadastro 0200");
        for(SpedParser.Item i:r.itens) {
            itensModel.addRow(new Object[]{i.linha,i.documento==null?"(sem C100)":i.documento.numero,
                i.operacao(),i.numero,i.codigo,i.descricao(),i.ncm(),i.cfop,i.cst,itemFormat(i,"QTD",i.quantidade,true),i.unidade,
                itemFormat(i,"VL_ITEM",i.valor,false),itemFormat(i,"VL_DESC",i.desconto,false),itemFormat(i,"VL_BC_ICMS",i.baseIcms,false),itemFormat(i,"VL_ICMS",i.icms,false),itemFormat(i,"VL_BC_ICMS_ST",i.baseSt,false),itemFormat(i,"VL_ICMS_ST",i.icmsSt,false),
                itemFormat(i,"VL_BC_IPI",i.baseIpi,false),itemFormat(i,"VL_IPI",i.ipi,false),i.produto==null?"Não localizado":"Localizado"});
        }
        bind(tabItens,itensModel,70,95,85,70,128,300,92,76,67,105,70,125,113,119,100,110,110,108,101,116);
        @SuppressWarnings("unchecked") TableRowSorter<DefaultTableModel> is=(TableRowSorter<DefaultTableModel>)tabItens.getRowSorter();
        if(is!=null) {
            is.setComparator(0, (x,y)->Integer.compare((Integer)x,(Integer)y));
            for(int c:new int[]{9,11,12,13,14,15,16,17,18})is.setComparator(c,(x,y)->DocumentoConsulta.valores().compare((String)x,(String)y));
        }
        Map<String,Integer> frequencia=new LinkedHashMap<String,Integer>();
        for(SpedParser.Item i:r.itens)frequencia.put(i.codigo,frequencia.containsKey(i.codigo)?frequencia.get(i.codigo)+1:1);
        DefaultTableModel produtosModel=model("Linha","COD_ITEM","Descrição","NCM","Tipo","Unid. inventário","Código barras","EX IPI","Ocorrências C170");
        for(SpedParser.Produto p:r.produtos.values())
            produtosModel.addRow(new Object[]{p.linha,p.codigo,p.descricao,p.ncm,p.tipo,p.unidadeInventario,p.ean,p.exIpi,
                frequencia.containsKey(p.codigo)?frequencia.get(p.codigo):0});
        bind(tabProdutos,produtosModel,70,150,350,115,80,115,155,75,128);
        filtrarItens();filtrarProdutos();
        DefaultTableModel e=model("Linha","Início","Fim","Débitos","Créditos","Saldo apurado","Deduções","ICMS a recolher","Credor a transportar");
        for(SpedParser.Apuracao v:r.apuracoes)
            e.addRow(new Object[]{v.linha,date(v.inicio),date(v.fim),money(v.debitos),money(v.creditos),money(v.saldoApurado),money(v.deducoes),money(v.recolher),money(v.credorTransportar)});
        bind(tabE110,e,75,110,110,134,134,147,130,151,175);
        countE110.setText(fmtCount(r.apuracoes.size())+" registros E110 declarados.");
        DefaultTableModel o=model("Linha","Registro","Ocorrência");
        for(SpedParser.Ocorrencia v:r.ocorrencias)o.addRow(new Object[]{v.linha,v.registro,v.descricao});
        bind(tabOcorrencias,o,80,90,830);
        countOcorrencias.setText(fmtCount(r.ocorrencias.size())+" ocorrências identificadas.");
        atualizarOpcoes();
        filtrar();
    }
    private void filtrar() {
        if(resultado==null || tabC190.getModel().getColumnCount()<6 || preenchendoOpcoes) return;
        final SpedConsultas.Filtro filtro=new SpedConsultas.Filtro(cfopFilter.getText(),cstFilter.getText(),
            (String)opFilter.getSelectedItem(),
            cfopChoices.getSelectedIndex()>0?(String)cfopChoices.getSelectedItem():"",
            cstChoices.getSelectedIndex()>0?(String)cstChoices.getSelectedItem():"");
        final List<SpedParser.Analitico> orig=resultado.analiticos;
        @SuppressWarnings("unchecked")
        TableRowSorter<DefaultTableModel> sorter=(TableRowSorter<DefaultTableModel>)tabC190.getRowSorter();
        if(sorter!=null) sorter.setRowFilter(new RowFilter<DefaultTableModel,Integer>() {
            @Override public boolean include(Entry<? extends DefaultTableModel,? extends Integer> e) {
                return SpedConsultas.corresponde(orig.get(e.getIdentifier()),filtro);
            }
        });
        List<SpedParser.Analitico> filtered=SpedConsultas.filtrar(orig,filtro);
        SpedConsultas.Totais total=SpedConsultas.totalizar(filtered);
        List<SpedConsultas.Grupo> grupos=SpedConsultas.agrupar(filtered);
        DefaultTableModel ag=model("Operação","CFOP","CST","Linhas C190","Linhas inválidas","Valor operação","Base ICMS","ICMS","Base ST","ICMS-ST","IPI");
        for(SpedConsultas.Grupo g:grupos) {
            SpedConsultas.Totais t=g.totais;
            ag.addRow(new Object[]{g.operacao,g.cfop,g.cst,t.quantidade,t.linhasInvalidas,money(t.operacoes),money(t.baseIcms),money(t.icms),money(t.baseSt),money(t.icmsSt),money(t.ipi)});
        }
        bind(tabCfop,ag,115,90,85,120,131,148,136,126,125,122,120);
        bind(tabRelatorios,ag,115,90,85,120,131,148,136,126,125,122,120);
        relatorioCount.setText(fmtCount(grupos.size())+" grupos  •  "+fmtCount(filtered.size())+" C190 considerados de "+fmtCount(orig.size())+" originais.");
        relatorioTotal.setText("C190 filtrado: R$ "+money(total.operacoes)+"  •  ICMS R$ "+money(total.icms)+"  •  IPI R$ "+money(total.ipi));
        relatorioKpi.setText("Registros C100 (todos): "+fmtCount(resultado.documentos.size())+"   |   C190 filtrados: "+fmtCount(filtered.size())+"   |   Apuração E110: "+resultado.countE110);
        relatorioAviso.setText(total.linhasInvalidas>0 ?
           "ATENÇÃO: "+total.linhasInvalidas+" linhas C190 com valor inválido. Totais podem estar incompletos." :
           "C100 bruto, C190 filtrado e E110 declarado são bases distintas. Não some entre si.");
        filterStats.setText("Exibindo "+fmtCount(filtered.size())+" de "+fmtCount(orig.size())+" registros C190  •  Códigos selecionados exatamente; pesquisas textuais por trecho.");
        String totals="Valor operações: R$ "+money(total.operacoes)+"    |    ICMS: R$ "+money(total.icms)+"    |    ICMS-ST: R$ "+money(total.icmsSt)+"    |    IPI: R$ "+money(total.ipi);
        totalsC190.setText(totals);totalsCfop.setText(totals);
        countC190.setText(fmtCount(filtered.size())+" de "+fmtCount(orig.size())+" linhas C190 exibidas"+(total.linhasInvalidas>0?"  •  "+total.linhasInvalidas+" com valor inválido":"")+".");
        countCfop.setText(fmtCount(grupos.size())+" grupos de operação + CFOP + CST"+(total.linhasInvalidas>0?"  •  "+total.linhasInvalidas+" linhas inválidas":"")+".");
    }
    private SpedConsultas.Filtro filtroAtual() {
        return new SpedConsultas.Filtro(cfopFilter.getText(),cstFilter.getText(),(String)opFilter.getSelectedItem(),
            cfopChoices.getSelectedIndex()>0?(String)cfopChoices.getSelectedItem():"",
            cstChoices.getSelectedIndex()>0?(String)cstChoices.getSelectedItem():"");
    }
    private File escolherDestino(String descricao,String ext,String sugerido) {
        JFileChooser fc=new JFileChooser(pastaAnterior==null?new File(System.getProperty("user.home")):pastaAnterior);
        fc.setDialogTitle(descricao);fc.setFileFilter(new FileNameExtensionFilter(ext.toUpperCase(Locale.ROOT)+" (*."+ext+")",ext));
        fc.setSelectedFile(new File(sugerido));
        if(fc.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return null;
        File target=fc.getSelectedFile();
        if(!target.getName().toLowerCase(Locale.ROOT).endsWith("."+ext))target=new File(target.getAbsolutePath()+"."+ext);
        try {
            if(target.getCanonicalFile().equals(resultado.arquivo.getCanonicalFile())){
                JOptionPane.showMessageDialog(this,"Por segurança, não é permitido sobrescrever o SPED original.");return null;
            }
        } catch(java.io.IOException ex) {JOptionPane.showMessageDialog(this,"Caminho inválido: "+ex.getMessage());return null;}
        if(target.exists() && JOptionPane.showConfirmDialog(this,"Sobrescrever arquivo existente?\n"+target.getName(),
                "Confirmar exportação",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return null;
        return target;
    }
    private interface Gravador { void salvar(File f)throws Exception; }
    private void salvarComDialogo(String formato,File destino,Gravador gravador) {
        if(destino==null)return;
        try {
            // Primeiro produz o arquivo completo em um destino temporário adjacente; só então publica.
            java.nio.file.Path original=destino.toPath().toAbsolutePath();
            java.nio.file.Path tmp=java.nio.file.Files.createTempFile(original.getParent(),"extrator-sped-", ".tmp");
            try {
                gravador.salvar(tmp.toFile());
                java.nio.file.Files.move(tmp,original,java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } finally {java.nio.file.Files.deleteIfExists(tmp);}
            atualizarStatus(formato+" gerado: "+destino.getName(),"ok");
            JOptionPane.showMessageDialog(this,formato+" salvo com sucesso:\n"+destino.getAbsolutePath()+
                "\n\nDados declarados, sem substituição do PVA e sem recálculo de tributos.","Exportação concluída",JOptionPane.INFORMATION_MESSAGE);
        } catch(Exception ex) {
            JOptionPane.showMessageDialog(this,"Não foi possível salvar "+formato+":\n"+ex.getMessage(),"Falha na exportação",JOptionPane.ERROR_MESSAGE);
        }
    }
    private void exportarTabelaXlsx() {
        if(resultado==null){JOptionPane.showMessageDialog(this,"Abra um SPED primeiro.");return;}
        final JTable tabela=tabelaDaPagina();
        if(tabela==null){JOptionPane.showMessageDialog(this,"Abra uma tabela ou a aba Relatórios para exportar.");return;}
        File destino=escolherDestino("Excel com as linhas visíveis na tabela atual","xlsx","extrator_sped_"+currentPage+".xlsx");
        salvarComDialogo("Excel da consulta",destino,f->XlsxExporter.tabela(tabela,f,"Extrator SPED — "+currentPage));
    }
    private void exportarRelatorioXlsx() {
        if(resultado==null){JOptionPane.showMessageDialog(this,"Abra um SPED primeiro.");return;}
        final RelatorioFiscal.Dados dados=RelatorioFiscal.construir(resultado,filtroAtual());
        File destino=escolherDestino("Relatório consolidado em Excel (.xlsx)","xlsx","relatorio_sped_consolidado.xlsx");
        salvarComDialogo("Relatório Excel",destino,f->XlsxExporter.relatorio(dados,f));
    }
    private void exportarRelatorioPdf() {
        if(resultado==null){JOptionPane.showMessageDialog(this,"Abra um SPED primeiro.");return;}
        final RelatorioFiscal.Dados dados=RelatorioFiscal.construir(resultado,filtroAtual());
        File destino=escolherDestino("Relatório consolidado em PDF","pdf","relatorio_sped_consolidado.pdf");
        salvarComDialogo("Relatório PDF",destino,f->PdfRelatorioExporter.exportar(dados,f));
    }

    private void exportar() {
        if(resultado==null){JOptionPane.showMessageDialog(this,"Abra um arquivo SPED primeiro.");return;}
        JTable table=tabelaDaPagina();
        if(table==null){JOptionPane.showMessageDialog(this,"Escolha uma consulta com tabela no menu lateral para exportar.");return;}
        JFileChooser fc=new JFileChooser(pastaAnterior==null?new File(System.getProperty("user.home")):pastaAnterior);
        fc.setDialogTitle("Exportar linhas visíveis em CSV (UTF-8; separador ;)");
        fc.setSelectedFile(new File("extrator_sped_"+currentPage+".csv"));
        if(fc.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        File target=fc.getSelectedFile();
        if(!target.getName().toLowerCase(Locale.ROOT).endsWith(".csv"))target=new File(target.getAbsolutePath()+".csv");
        try {
            if(target.getCanonicalFile().equals(resultado.arquivo.getCanonicalFile())){
                JOptionPane.showMessageDialog(this,"Por segurança, não é permitido sobrescrever o SPED original.");return;
            }
            if(target.exists() && JOptionPane.showConfirmDialog(this,"Sobrescrever o CSV existente?","Confirmar exportação",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
            int count=CsvExporter.exportar(table,target);
            atualizarStatus("CSV exportado: "+fmtCount(count)+" linhas  |  "+target.getName(),"ok");
            JOptionPane.showMessageDialog(this,"CSV gerado com "+count+" linhas visíveis:\n"+target.getAbsolutePath()+
                    "\n\nFiltros e ordem visual foram respeitados.\nNo Excel, importe CNPJ, CFOP, CST, chave NF-e e nota como texto.","Exportação concluída",JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,"Falha na exportação:\n"+ex.getMessage(),"Erro",JOptionPane.ERROR_MESSAGE);
        }
    }
    private JTable tabelaDaPagina() {
        switch(currentPage){case "docs":return tabNotas;case "items":return tabItens;case "products":return tabProdutos;case "analytic":return tabC190;case "cfop":return tabCfop;case "e110":return tabE110;case "issues":return tabOcorrencias;case "reports":return tabRelatorios;default:return null;}
    }
    private void ajuda() { mostrar("help"); }
    private static String abbreviate(String s,int max) {
        if(s.length()<=max)return s;
        return s.substring(0,Math.max(10,max-43))+" ... "+s.substring(s.length()-38);
    }
    private static String date(String s){return s!=null&&s.matches("[0-9]{8}")?s.substring(0,2)+"/"+s.substring(2,4)+"/"+s.substring(4):s;}
    private static String money(BigDecimal x){return x==null?"INVÁLIDO":MOEDA.format(x);}
    private static String quantity(BigDecimal x){return x==null?"INVÁLIDO":QUANTIDADE.format(x);}
    private static String itemFormat(SpedParser.Item i,String campo,BigDecimal valor,boolean quantidade){
        if (i.ausentes.contains(campo))return "—";
        return quantidade?quantity(valor):money(valor);
    }
    private static String fmtCount(long x){return String.format(new Locale("pt","BR"),"%,d",x);}
    private static String formatCnpj(String s){return s!=null&&s.matches("[0-9]{14}")?s.substring(0,2)+"."+s.substring(2,5)+"."+s.substring(5,8)+"/"+s.substring(8,12)+"-"+s.substring(12):s;}
}
