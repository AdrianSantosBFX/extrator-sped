package br.sig.sped;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.swing.JTable;

/** Exporta apenas linhas VISIVEIS na ordenacao e filtros atuais da JTable. */
public final class CsvExporter {
    private CsvExporter() {}
    public static int exportar(JTable tabela, File destino) throws IOException {
        try (BufferedWriter w=Files.newBufferedWriter(destino.toPath(),StandardCharsets.UTF_8)) {
            w.write('\uFEFF'); // Excel reconhece UTF-8 com BOM
            for (int c=0;c<tabela.getColumnCount();c++) {
                if (c>0) w.write(';');
                w.write(csv(tabela.getColumnName(c)));
            }
            w.newLine();
            for (int r=0;r<tabela.getRowCount();r++) {
                for (int c=0;c<tabela.getColumnCount();c++) {
                    if (c>0) w.write(';');
                    Object v=tabela.getValueAt(r,c);
                    w.write(csv(v==null?"":String.valueOf(v)));
                }
                w.newLine();
            }
        }
        return tabela.getRowCount();
    }
    static String csv(String valor) {
        // CSV formula injection: impede formulas iniciadas por caracteres de controle.
        if (!valor.isEmpty()) {
            String s=valor.trim();
            if(!s.isEmpty() && "=+@\t\r\n".indexOf(s.charAt(0))>=0) valor="'"+valor;
        }
        return '"'+valor.replace("\"","\"\"")+'"';
    }
}
