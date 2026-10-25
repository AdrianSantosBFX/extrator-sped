package br.sig.sped;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

/** Testes da importação de EFD substituta e da exportação de cópia literal SPED/TXT. */
public final class SpedFileIOTest {
    private static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
    private static void testarCopia(File entrada,String codFin) throws Exception {
        SpedParser.Resultado original=SpedParser.ler(entrada);
        check(codFin.equals(original.codFin),"finalidade importada: "+original.codFin);
        Path base=Files.createTempDirectory("efd-v018-");
        try {
            File destino=new File(base.toFile(),"teste-exportacao.sped");
            SpedFileIO.copiar(original,destino);
            check(destino.isFile(),"copia .sped criada");
            check(Arrays.equals(Files.readAllBytes(entrada.toPath()),Files.readAllBytes(destino.toPath())),"copia byte a byte");
            SpedParser.Resultado reimportada=SpedParser.ler(destino);
            check(codFin.equals(reimportada.codFin),"finalidade preservada");
            check(reimportada.documentos.size()==original.documentos.size(),"notas preservadas");
            check(reimportada.analiticos.size()==original.analiticos.size(),"C190 preservados");
            check(reimportada.itens.size()==original.itens.size(),"C170 preservados");
            check(reimportada.apuracoes.size()==original.apuracoes.size(),"E110 preservados");
            boolean erro=false;
            try {SpedFileIO.copiar(original,destino);} catch(java.io.IOException ex){erro=true;}
            check(erro,"bloqueia sobrescrever arquivo existente");
            erro=false;
            try {SpedFileIO.copiar(original,entrada);} catch(java.io.IOException ex){erro=true;}
            check(erro,"bloqueia sobrescrever original");
            File copiaTxt=new File(base.toFile(),"teste-exportacao.txt");
            SpedFileIO.copiar(original,copiaTxt);
            check(Arrays.equals(Files.readAllBytes(entrada.toPath()),Files.readAllBytes(copiaTxt.toPath())),".txt tambem preserva bytes");
        }finally{
            try(java.util.stream.Stream<Path> s=Files.walk(base)){
                s.sorted(Collections.reverseOrder()).forEach(x->{try{Files.deleteIfExists(x);}catch(Exception ex){throw new RuntimeException(ex);}});
            }
        }
    }
    public static void main(String[] args) throws Exception {
        check(SpedFileIO.comExtensao(new File("efd"),"sped").getName().equals("efd.sped"),"extensao default");
        check(SpedFileIO.comExtensao(new File("efd"),"txt").getName().equals("efd.txt"),"extensao txt");
        check(SpedFileIO.comExtensao(new File("efd.SPED"),"txt").getName().equals("efd.SPED"),"extensao digitada respeitada");
        boolean falha=false;
        try {SpedFileIO.comExtensao(new File("efd.exe"),"sped");}catch(java.io.IOException ex){falha=true;}
        check(falha,"rejeita outra extensao");
        testarCopia(new File(args[0]),"0");
        testarCopia(new File(args[1]),"1");
        SpedParser.Resultado r=SpedParser.ler(new File(args[0]));
        // arquivo de entrada já substituto, mas ainda com crédito: COD_FIN não deve virar zero.
        String substituto=new String(Files.readAllBytes(new File(args[0]).toPath()),java.nio.charset.StandardCharsets.ISO_8859_1)
                .replaceFirst("\\|0000\\|020\\|0\\|","|0000|020|1|");
        Path temporario=Files.createTempFile("efd-retificadora-candidatos-",".sped");
        try {
            Files.write(temporario,substituto.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
            SpedParser.Resultado reimport=SpedParser.ler(temporario.toFile());
            check("1".equals(reimport.codFin),"importar arquivo retificador com creditos pendentes");
            List<CreditoUsoConsumo.Caso> grupos=CreditoUsoConsumo.analisar(reimport);
            check(grupos.size()==19,"retificadora pode ter ocorrencias para corrigir");
            CreditoUsoConsumo.Plano plano=CreditoUsoConsumo.planejar(reimport,Collections.singleton(344),true);
            Path corrigido=Files.createTempFile("efd-retificadora-corrigida-",".sped");Files.delete(corrigido);
            try {
                CreditoUsoConsumo.salvar(plano,corrigido.toFile());
                check("1".equals(SpedParser.ler(corrigido.toFile()).codFin),"retificadora permanece substituta");
            }finally {Files.deleteIfExists(corrigido);}
        }finally {Files.deleteIfExists(temporario);}
        System.out.println("PASSOU: .SPED/.TXT, original/substituta, copia identica, reimportacao, COD_FIN e protecao de gravacao.");
    }
}
