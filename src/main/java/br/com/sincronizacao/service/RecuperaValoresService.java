package br.com.sincronizacao.service;

import java.text.ParseException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import br.com.sincronizacao.enumerator.PosicaoFile;
import br.com.sincronizacao.exceptions.ResourceNotFoundException;

@Service
public class RecuperaValoresService {
	
	public String getValue(String[] values, PosicaoFile posicaoEnum) {
		try {
			return this.getStringValue(values, posicaoEnum.getPosicao());
		} catch (Exception e) {
			// LOGGER.error("Erro ao ler o arquivo", e);
			throw new ResourceNotFoundException("Erro ao ler o arquivo");
		}
	}

	public String getStringValue(final String[] values, final int posicao) throws ParseException {
		if (posicao > 0) {
			String campo;
			try {
				if (values.length > 0) {
					campo = values[posicao - 1].trim();
				} else {
					campo = null;
				}

			} catch (ArrayIndexOutOfBoundsException e) {
				campo = null;
			}

			if (RecuperaValoresService.isCampoVazio(campo)) {
				return null;
			}
			return StringEscapeUtils.unescapeJava(campo);
		}
		return null;
	}

	public static boolean isCampoVazio(final String campo) {
		return isBlank(trimToEmpty(campo));
	}

	public static boolean isBlank(String texto) {
		return StringUtils.isBlank(texto);
	}

	public static String trimToEmpty(String texto) {
		return StringUtils.trimToEmpty(texto);
	}

}
