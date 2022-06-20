package br.com.sincronizacao.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.jcraft.jsch.ChannelSftp;

import br.com.sincronizacao.ftp.FTPSession;

@Service
public class FTPIntegrationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(FTPIntegrationService.class);

	@Value("${sincronizacao.usuario:anonymous}")
	private String user;

	@Value("${sincronizacao.ftp.senha:}")
	private String password;

	@Value("${sincronizacao.ftp.private.key:}")
	private String privateKeyLoc;

	@Value("${sincronizacao.ftp.known.hosts:}")
	private String knownHosts;

	@Autowired
	private Environment environment;

	public Boolean enviarDocumento(File arquivo) throws UnsupportedEncodingException, IOException {
		
		final int porta = this.environment.getProperty("sincronizacao.ftp.porta") != null
				? Integer.valueOf(this.environment.getProperty("sincronizacao.ftp.porta"))
				: 0;
		final String host = this.environment.getProperty("sincronizacao.ftp.host");
		final String diretorioRemoto = this.environment.getProperty("sincronizacao.ftp.diretorio");
		final String destinationFilePath = formatarString(diretorioRemoto + "/" + arquivo.getName() + ".csv");

		final Instant inicio = Instant.now();
		LOGGER.info(String.format("%s Enviando arquivo para FTP...", host));

		InputStream inputStream = null;
		try {

			inputStream = new FileInputStream(arquivo);

			sendFile(this.privateKeyLoc, this.knownHosts, this.user, host, porta, inputStream, destinationFilePath);

			return true;

		} catch (Exception e) {
			LOGGER.error("enviarDocumento", e);
			throw e;
		} finally {

			IOUtils.closeQuietly(inputStream);
			final Instant fim = Instant.now();
			final Duration duracao = Duration.between(inicio, fim);
			LOGGER.info(String.format("%s - Tempo total enviar arquivo = { %s }min { %s }s ", "enviarDocumento",
					duracao.getSeconds() / 60, duracao.getSeconds() % 60));
		}

	}

	public void sendFile(String privateKeyLocation, String knownHosts, String user, String host, int porta,
			final InputStream inputStream, String destinationFilePath) {

		FTPSession ftpSession = null;
		try {
			ftpSession = new FTPSession(privateKeyLocation, knownHosts, user, host, porta);
			ChannelSftp sftpChannel = ftpSession.getSftpChannel();

			LOGGER.info("Enviando arquivo: PATH: - " + destinationFilePath);

			sftpChannel.put(inputStream, destinationFilePath);

		} catch (final Exception ex) {
			LOGGER.error("Erro: " + ex);
			throw new RuntimeException(ex);
		} finally {
			if (ftpSession != null) {
				LOGGER.info("FTPSession - Fechando a sessão aberta");
				ftpSession.fecharConexoes();
			}
		}
	}

	private String formatarString(final String string) throws UnsupportedEncodingException {
		final byte[] ptext = string.getBytes(Charset.defaultCharset().name());
		final String nomeArq = new String(ptext, Charset.forName("UTF-8"));
		return nomeArq;
	}

}
