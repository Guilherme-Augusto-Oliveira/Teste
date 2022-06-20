package br.com.sincronizacao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import br.com.sincronizacao.enumerator.PosicaoFile;
import br.com.sincronizacao.ftp.FTPSession;
import br.com.sincronizacao.service.ReceitaService;
import br.com.sincronizacao.service.RecuperaValoresService;

@SpringBootApplication
public class DesafioSicrediApplication implements CommandLineRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(DesafioSicrediApplication.class);

	@Autowired
	private RecuperaValoresService recuperaValoresService;

	@Autowired
	private ReceitaService receitaService;

	public static void main(String[] args) {
		SpringApplication.run(DesafioSicrediApplication.class, args);
	}

	@SuppressWarnings({ "resource", "unused" })
	@Override
	public void run(String... args) throws Exception {

		if (args.length != 1) {
			LOGGER.info("Chamar passando um parâmetro, que é o arquivo no qual deseja processar, EXEMPLO: java -jar SincronizacaoReceita arquivo.csv");
		}

		File file = null;
		BufferedReader br = null;

		if (args.length > 0) {
			file = new File(args[0]);
		}

		if (file != null) {
			br = new BufferedReader(new FileReader(file));
		}

		String linha;
		String[] values = {};
		int linhaArquivo = 0;
		List<String[]> arquivoResultado = new ArrayList<String[]>();
		List<String[]> arquivoParaEnviar = new ArrayList<String[]>();
		String statusEnvio = "Status do Envio";
		double valorSaldo = 0.0;
		boolean atualizou = true;

		if (br != null) {
			LOGGER.info("Iniciando a leitura do arquivo....");
			try {
				while ((linha = br.readLine()) != null && !linha.isEmpty()) {
					values = linha.split(";");
					String agencia = recuperaValoresService.getValue(values, PosicaoFile.AGENCIA);
					String conta = recuperaValoresService.getValue(values, PosicaoFile.CONTA);
					String saldo = recuperaValoresService.getValue(values, PosicaoFile.SALDO);
					String status = recuperaValoresService.getValue(values, PosicaoFile.STATUS);
					if (linhaArquivo > 0) {
						valorSaldo = Double.parseDouble(saldo.replace(",", ""));
						atualizou = receitaService.atualizarConta(agencia, conta, valorSaldo, status);
						statusEnvio = atualizou ? "Enviado" : "Não enviado";
						StringEscapeUtils.unescapeJava(statusEnvio);
						LOGGER.info("Linha: " + linhaArquivo + " valores: " + "AGENCIA:" + agencia + " CONTA:" + conta
								+ " SALDO:" + saldo + " STATUS:" + status + "  " + statusEnvio);
					}

					String[] dadosArquivoResultado = { agencia, conta, saldo, status, statusEnvio };
					arquivoResultado.add(dadosArquivoResultado);
					if (linhaArquivo > 0 && atualizou) {
						String[] dadosArquivoParaEnvio = { agencia, conta, saldo, status };
						arquivoParaEnviar.add(dadosArquivoParaEnvio);
					}
					linhaArquivo++;
				}
				LOGGER.info("Fim da leitura do arquivo.");
				receitaService.geraArquivoEEnvia(arquivoResultado, arquivoParaEnviar);
			} catch (Exception e) {
				LOGGER.error("Erro ao realizar a leitura do arquivo" + e.getMessage());
			}
		} 
	}

}
