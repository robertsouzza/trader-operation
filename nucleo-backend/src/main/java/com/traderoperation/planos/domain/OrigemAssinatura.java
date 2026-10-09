package com.traderoperation.planos.domain;

/** De onde veio a assinatura. PAGAMENTO só é criado pela skill 09 após confirmação do PSP. */
public enum OrigemAssinatura {
    MANUAL,
    PAGAMENTO
}
