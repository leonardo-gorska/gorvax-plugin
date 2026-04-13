# 🐉 GorvaxCore — Plano de Precificação VIP

> **Documento de planejamento.** Valores sugeridos para o mercado brasileiro.
> Todos os valores em R$ (Real). Preços posicionados abaixo do mercado para atrair a base inicial.

## Filosofia

1. **Zero Pay-to-Win** — nenhum benefício de combate direto (sem armas, dano extra, HP extra).
2. **Conveniência + Cosmético** — homes extras, blocos de proteção, tags, keys de crates.
3. **Progressão clara** — cada tier deve ser notavelmente melhor, justificando o investimento.

---

## Tabela de Tiers

| Benefício               | ✦ VIP (R$9,90) | ✦ VIP+ (R$24,90) | ⚡ ELITE (R$49,90) | 🐉 LENDÁRIO (R$89,90) |
|--------------------------|:--------------:|:-----------------:|:------------------:|:----------------------:|
| Blocos extras            | +500           | +1.500            | +3.000             | +5.000                 |
| Homes extras             | +2             | +5                | +10                | +15                    |
| Keys mensais (Raro)      | 1              | 2                 | 3                  | 3                      |
| Keys mensais (Lendário)  | —              | 1                 | 1                  | 2                      |
| Desconto mercado         | 0%             | 5%                | 10%                | 15%                    |
| Prefixo exclusivo        | §a[✦ VIP]      | §b[✦ VIP+]        | §6[⚡ ELITE]       | §d[🐉 LENDÁRIO]       |
| Tag no chat              | ✔              | ✔                 | ✔                  | ✔                      |
| Cor de nick              | —              | ✔                 | ✔                  | ✔                      |
| Trail de partículas      | —              | —                 | ✔                  | ✔                      |
| Kill effect exclusivo    | —              | —                 | —                  | ✔                      |

---

## Preços Sugeridos

| Plano       | ✦ VIP     | ✦ VIP+     | ⚡ ELITE    | 🐉 LENDÁRIO  |
|-------------|-----------|------------|-------------|---------------|
| Mensal      | R$ 9,90   | R$ 24,90   | R$ 49,90    | R$ 89,90      |
| Trimestral  | R$ 24,90  | R$ 59,90   | R$ 119,90   | R$ 219,90     |
| Vitalício   | R$ 49,90  | R$ 119,90  | R$ 249,90   | R$ 449,90     |

> **Nota:** O trimestral tem ~16% de desconto vs. mensal. O vitalício equivale a ~5 meses.

---

## Comparação de Mercado (Servidores BR)

| Servidor (referência) | VIP básico | VIP top     | Observações                     |
|-----------------------|-----------|-------------|----------------------------------|
| Média BR (survival)   | R$ 10–15  | R$ 60–100   | Maioria vende vantagens P2W      |
| Nosso posicionamento  | R$ 9,90   | R$ 89,90    | Sem P2W, foco em conveniência    |

---

## Plataforma de Venda

- **Recomendação primária:** [Tebex](https://tebex.io) (melhor integração, suporte brasileiro)
- **Alternativa:** [CraftingStore](https://craftingstore.net)

### Comandos de Integração (executados via console pelo Tebex)

```
# Ao comprar VIP
lp user {username} parent set vip

# Ao comprar VIP+
lp user {username} parent set vip-plus

# Ao comprar ELITE
lp user {username} parent set elite

# Ao comprar LENDÁRIO
lp user {username} parent set lendario

# Ao expirar/cancelar
lp user {username} parent set default
```

---

## Promoções de Lançamento

| Promoção                          | Desconto | Duração    |
|-----------------------------------|----------|------------|
| Semana de Lançamento              | 30%      | 7 dias     |
| Black Friday / Cyber Monday       | 40%      | 3 dias     |
| Aniversário do Servidor           | 25%      | 5 dias     |
| Upgrade de Tier (desconto parcial)| 50%*     | Permanente |

> *\*Quem já possui VIP paga apenas a diferença + 50% ao fazer upgrade.*

---

## Implementação Técnica

- Grupos gerenciados pelo **LuckPerms** (já configurado no `PermissionManager.java`)
- Benefícios aplicados pelo **VipManager** ao logar / ao alterar tier
- Keys distribuídas automaticamente no **dia 1** de cada mês
- Placeholders PAPI: `%gorvax_vip_tier%`, `%gorvax_vip_display%`, `%gorvax_vip_blocks%`
- Comando `/vip` disponível para todos os jogadores (info/status)
- Subcomandos admin: `/vip set`, `/vip remove`, `/vip keys`, `/vip reload`
