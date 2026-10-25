package br.sig.sped;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Exportação EFD: .sped e .txt são nomes para o MESMO conteúdo SPED em texto.
 * Cópias byte a byte mantêm COD_FIN, charset, quebras de linha e contadores.
 * Não afirma validade no PVA nem muda o conteúdo para adequá-lo ao fisco.
 */
public final class SpedFileIO {
    private SpedFileIO() { }

    public static File comExtensao(File selecionado, String extensaoPreferida) throws IOException {
        if(selecionado==null)throw new IOException("Selecione um arquivo de destino.");
        String ext=extensaoPreferida==null?"sped":extensaoPreferida.toLowerCase(Locale.ROOT);
        if(!"sped".equals(ext) && !"txt".equals(ext))
            throw new IOException("Formato inválido. Utilize SPED ou TXT.");
        String nome=selecionado.getName();
        if(nome.length()==0 || ".".equals(nome) || "..".equals(nome))
            throw new IOException("Informe um nome válido para a EFD.");
        String lower=nome.toLowerCase(Locale.ROOT);
        if(lower.endsWith(".sped") || lower.endsWith(".txt"))return selecionado;
        if(nome.lastIndexOf('.')>0)throw new IOException("A extensão deve ser .sped ou .txt.");
        return new File(selecionado.getParentFile(),nome+"."+ext);
    }

    public static File copiar(SpedParser.Resultado resultado, File destino) throws IOException {
        if(resultado==null || resultado.arquivo==null)throw new IOException("Carregue uma EFD antes de salvar.");
        if(destino==null)throw new IOException("Escolha o arquivo de destino.");
        File validado=comExtensao(destino,"sped");
        if(!validado.equals(destino))throw new IOException("O destino deve terminar em .sped ou .txt.");
        if(!"0".equals(resultado.codFin) && !"1".equals(resultado.codFin))
            throw new IOException("COD_FIN inválido; confira o registro 0000.");
        Path origem=resultado.arquivo.toPath().toRealPath();
        Path saida=destino.toPath().toAbsolutePath().normalize();
        if(origem.equals(saida))throw new IOException("A exportação não pode sobrescrever o original.");
        if(Files.exists(saida))throw new IOException("O destino já existe. Escolha outro nome.");
        if(!resultado.tem0000 || resultado.linhasIgnoradas>0)
            throw new IOException("SPED incompleto ou com linhas descartadas; não é seguro criar cópia para análise no PVA.");
        // Garante que finalidade e identidade do arquivo não foram alteradas depois da importação.
        SpedParser.Resultado atual=SpedParser.ler(origem.toFile());
        if(!atual.tem0000 || !resultado.codFin.equals(atual.codFin)
                || !resultado.cnpj.equals(atual.cnpj) || !resultado.periodoInicio.equals(atual.periodoInicio)
                || !resultado.periodoFim.equals(atual.periodoFim))
            throw new IOException("A EFD foi modificada após a leitura. Importe-a novamente.");
        // CREATE_NEW equivalente via Files.copy: falha se já existir, sem conversão/reformatação.
        Files.copy(origem,saida);
        return saida.toFile();
    }
}
