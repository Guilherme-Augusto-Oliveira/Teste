package br.com.sincronizacao.enumerator;

public enum PosicaoFile {

	AGENCIA(1), CONTA(2), SALDO(3), STATUS(4);

	private int posicao;

	private PosicaoFile(int posicao) {
		this.posicao = posicao;
	}

	public int getPosicao() {
		return this.posicao;
	}

}
