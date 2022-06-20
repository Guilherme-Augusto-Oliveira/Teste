package br.com.sincronizacao.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;

@Service
public class ReceitaService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReceitaService.class);

	@Value("${sincronizacao.teste.diretorio:}")
	private String diretorioArquivoResultado;

	@Value("${sincronizacao.producao:}")
	private boolean producao;

	@Autowired
	private FTPIntegrationService ftpIntegrationService;

	public boolean atualizarConta(String agencia, String conta, double saldo, String status)
			throws RuntimeException, InterruptedException {

		// Formato agencia: 0000
		if (agencia == null || agencia.length() != 4) {
			LOGGER.info("Agencia: " + agencia + " " + " com formato inválido, formato esperado: 0000");
			return false;
		}

		// Formato conta: 000000
		if (conta == null || conta.length() != 6) {
			LOGGER.info("Conta: " + conta + " " + " com formato inválido, formato esperado: 000000");
			return false;
		}

		// Tipos de status validos:
		List<String> tipos = new ArrayList<String>();
		tipos.add("A");
		tipos.add("I");
		tipos.add("B");
		tipos.add("P");

		if (status == null || !tipos.contains(status)) {
			LOGGER.info("Conta: " + conta + " " + " não possui o status válido, Tipos válidos: A, B, I, P");
			return false;
		}

		// Simula tempo de resposta do serviço (entre 1 e 5 segundos)
		long wait = Math.round(Math.random() * 4000) + 1000;
		Thread.sleep(wait);

		// Simula cenario de erro no serviço (0,1% de erro)
		long randomError = Math.round(Math.random() * 1000);
		if (randomError == 500) {
			throw new RuntimeException("Error");
		}

		return true;
	}

	public void geraArquivoEEnvia(List<String[]> arquivoResultado, List<String[]> arquivoParaEnviar)
			throws IOException {

		if (arquivoParaEnviar.size() > 0) {
			File dir = new File(diretorioArquivoResultado);
			File file = new File(dir, "ArquivoReceita.csv");

			LOGGER.info("Gerando o arquivo de envio para a receita....");
			try {
				file.createNewFile();
				FileWriter fileWriter = new FileWriter(file, false);
				PrintWriter printWriter = new PrintWriter(fileWriter);

				for (int i = 0; i < arquivoParaEnviar.size(); i++) {
					String[] linha = arquivoParaEnviar.get(i);

					for (int j = 0; j <= linha.length; j++) {

						if (j + 1 == linha.length) {
							printWriter.print(linha[j]);
							printWriter.println(" ");
							break;
						} else {
							printWriter.print(linha[j] + ";");
						}
					}
				}
				printWriter.flush();
				printWriter.close();
			} catch (IOException e) {
				LOGGER.error("Ocorreu um erro ao gerar o arquivo para enviar a receita ", e.getMessage());
			}
			LOGGER.info("Fim da  geração do arquivo de envio para a receita");
			
			if (producao) {
				Boolean enviouDocumento = this.ftpIntegrationService.enviarDocumento(file);
				if (enviouDocumento) {
					LOGGER.info("Arquivo enviado para a receita com sucesso!");
				}
			} else {
				LOGGER.info("O arquivo não foi enviado para receita, pois a aplicação esta em modo teste");
			}
		}

		LOGGER.info("Gerando o arquivo de resultado no caminho: " + diretorioArquivoResultado);
		try (ICSVWriter writer = new CSVWriterBuilder(new FileWriter(diretorioArquivoResultado + "/resultado.csv"))
				.withSeparator(';').build()) {
			writer.writeAll(arquivoResultado);
		} catch (Exception e) {
			LOGGER.error("Erro ao Gerar o arquivo de resultado. " + e.getMessage());
		}
		LOGGER.info("Fim da  geração do arquivo de resultado");

	}

}
