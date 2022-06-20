package br.com.sincronizacao.ftp;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class FTPSession {

	private static final Logger LOGGER = LoggerFactory.getLogger(FTPSession.class);

	private static final int MAX_TENTATIVAS = 5;
	private static final int WAIT_TIME = 50000;

	@Value("${sincronizacao.ftp.host.timeout.inmillis:150000}")
	private int hostTimeout;

	@Value("${sincronizacao.ftp.sftp.timeout.inmillis:3600000}")
	private int sftpTimeout;

	private static final String CHANNEL_TYPE = "sftp";

	private Session session;
	private ChannelSftp sftpChannel;

	public FTPSession(final String privateKeyLocation, final String knownHosts, final String user, final String host,
			final int porta) throws Exception {
		String descricao = "[ iniciando FTPSession ]";
		try {
			
			for (int i = 0; i < MAX_TENTATIVAS; i++) {
				try {
					initSession(privateKeyLocation, knownHosts, user, host, porta, descricao);
					LOGGER.info(String.format("%s FTPSession - Tentativa { %d } de conexão ao servidor { %s }",
							descricao, (i + 1), host));
					session.connect(hostTimeout);

					LOGGER.info(" Conexao Host ativo: " + session.isConnected());
					LOGGER.info(descricao + "FTPSession - Sessão SSH criada");

					sftpChannel = (ChannelSftp) session.openChannel(CHANNEL_TYPE);
					sftpChannel.connect(sftpTimeout);
					LOGGER.info(descricao + "FTPSession - Conexao SFTP ativo: " + sftpChannel.isConnected());
					break;
				} catch (final JSchException ex) {
					LOGGER.error(ex.getMessage(), ex);
					if (i == (MAX_TENTATIVAS - 1)) {
						LOGGER.info(descricao + "FTPSession - Número máximo de tentativas atingido: " + ex);
						throw ex;
					}
					LOGGER.info(descricao + "FTPSession - FTPSession - Fechando a sessão aberta");
					fecharConexoes();
					Thread.sleep(WAIT_TIME);
					continue;
				}
			}

		} catch (final Exception e) {
			LOGGER.error(descricao + "Erro na criação do FTPSession: " + e.getMessage(), e);
			LOGGER.info(descricao + "Fechando a sessão aberta - Erro na criação do FTPSession");
			this.fecharConexoes();

			throw e;
		}
	}

	public void fecharConexoes() {
		String nomeDoMetodo = "[ FTPSession - fecharConexoes() ] ";
		try {
			if (session != null && session.isConnected()) {

				if (sftpChannel != null && sftpChannel.isConnected()) {

					LOGGER.info(nomeDoMetodo + "Fechando a conexão sftp aberta");
					sftpChannel.disconnect();
				}

				LOGGER.info(nomeDoMetodo + "Fechando a sessão aberta");
				session.disconnect();
			}
		} catch (Exception e) {
			LOGGER.error(String.format("%s Erro ao fechar conexão FTP: %s , ", nomeDoMetodo, e.getMessage()), e);
		}
	}

	private void initSession(final String privateKeyLocation, final String knownHosts, final String user,
			final String host, final int porta, String descricao) throws JSchException {
		String nomeDoMetodo = "[ FTPSession.initSession() ] ";
		LOGGER.info(descricao + nomeDoMetodo + "User: " + user);
		LOGGER.info(descricao + nomeDoMetodo + "Porta: " + porta);
		LOGGER.info(descricao + nomeDoMetodo + "Host: " + host);
		LOGGER.info(descricao + nomeDoMetodo + "Diretório PK: " + privateKeyLocation);
		LOGGER.info(descricao + nomeDoMetodo + "Diretório known_hosts: " + knownHosts);
		LOGGER.info(descricao + nomeDoMetodo + "Host Timeout (em min): " + hostTimeout / 60000);
		LOGGER.info(descricao + nomeDoMetodo + "Sftp Timeout (em min): " + sftpTimeout / 60000);

		LOGGER.info(descricao + nomeDoMetodo + "Iniciando verificação SSH");
		final JSch jsch = new JSch();
		if (StringUtils.isNotEmpty(privateKeyLocation)) {
			jsch.addIdentity(privateKeyLocation);
		}

		LOGGER.info(descricao + nomeDoMetodo + "Identidade criada a partir da chave privada informada");
		session = jsch.getSession(user, host, porta);

		LOGGER.info(descricao + nomeDoMetodo + "Conexao com FTP ativa: " + session.isConnected());

		session.setConfig("StrictHostKeyChecking", "no");
		// AMBIENTE DE TESTES
		if (StringUtils.isEmpty(knownHosts) && StringUtils.isEmpty(privateKeyLocation)) {
			session.setPassword("password");
		}
	}

	public ChannelSftp getSftpChannel() {
		return sftpChannel;
	}
}
